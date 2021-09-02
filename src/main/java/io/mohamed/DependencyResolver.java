// -*- mode: java; c-basic-offset: 2; -*-
/*
 * MIT License
 *
 * Copyright (c) 2021 Mohamed Tamer Elsayed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.mohamed;

import static io.mohamed.model.Repository.COMMON_MAVEN_REPOSITORIES;

import io.mohamed.callback.ResolveCallback;
import io.mohamed.model.Dependency;
import io.mohamed.model.ProjectProperty;
import io.mohamed.model.Repository;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Resolves and downloads the POM file all the dependencies for the given artifact.
 *
 * @author Mohamed Tamer
 */
public class DependencyResolver {

  // keeps track of all the loaded dependencies and the dependency that loaded it
  private static final HashMap<Dependency, Dependency> loadedDependencies = new HashMap<>();
  // used for iterating asynchronously over the maven repositories
  int repoIndex = 0;
  // a flag to indicate that the artifact given by the user was resolved correctly
  boolean artifactFound = false;
  // the url to download artifact POM file
  String pomDownloadUrl = "";
  // the dependencies collected for the artifact give by the user
  List<Dependency> dependencies = new ArrayList<>();
  // the repo url which was used to load the artifact
  Repository mavenRepo;
  // keeps track of the dependencies which hasn't been loaded yet
  List<Dependency> dependenciesToLoad = new ArrayList<>();
  // keeps track of the threads used for resolving dependencies
  ArrayList<Thread> resolverThreads = new ArrayList<>();
  // the callback that's called when the resolving is completely done
  ResolveCallback callback = null;
  // a flag to indicate that we have finished resolving dependencies
  private boolean done = false;

  public static boolean isNumeric(String str) {
    try {
      Double.parseDouble(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Returns the path for the dependency, this path is examined against the supported maven
   * repositories
   *
   * @param dependency The dependency to find its url
   * @return the dependency url
   */
  private static String getPomDownloadUrl(Dependency dependency) {
    return dependency.getGroupId().replaceAll("\\.", "/")
        + "/"
        + dependency.getArtifactId()
        + "/"
        + dependency.getVersion()
        + "/"
        + dependency.getArtifactId()
        + "-"
        + dependency.getVersion()
        + ".pom";
  }

  /**
   * Replaces the property name declaration in property name with the property value ( if any )
   *
   * @param properties the dependency's project properties
   * @param str the dependency version string
   * @return the cleaned up version
   */
  private String parseVersion(
      List<ProjectProperty> properties,
      String str,
      String groupID,
      String artifactId,
      Repository repository) {
    Pattern p = Pattern.compile(".*\\$\\{.*}.*");
    // create matcher for pattern p and given string
    Matcher m = p.matcher(str);
    // if an occurrence if a pattern was found in a given string...
    if (m.find()) {
      String propertyName = str.replaceAll("(.*)\\$\\{([^&]*)}(.*)", "$1$2$3");
      for (ProjectProperty projectProperty : properties) {
        if (projectProperty.getName().equals(propertyName)) {
          str = projectProperty.getValue();
        }
      }
    }
    p = Pattern.compile("\\[.*]");
    m = p.matcher(str);
    if (!m.find()) {
      return str;
    }
    str = str.replaceAll("\\[(.*)]", "$1");
    if (str.contains(",")) {
      String startVersion = str.split(",")[0].trim();
      String endVersion = str.split(",")[1].trim();
      try {
        URL url = new URL(repository.getUrl() + groupID + "/" + artifactId);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));

        StringBuilder stringBuilder = new StringBuilder();

        String inputLine;
        while ((inputLine = bufferedReader.readLine()) != null) {
          stringBuilder.append(inputLine);
          stringBuilder.append(System.lineSeparator());
        }

        bufferedReader.close();
        String html = stringBuilder.toString().trim();
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        Elements elements = document.select("a");
        List<String> versions = new ArrayList<>();
        for (org.jsoup.nodes.Element element : elements) {
          String href = element.attr("href");
          if (href.endsWith("/") && !href.equals("../")) {
            versions.add(href.substring(0, href.length() - 1));
          }
        }
        String preferredVersion = null;
        for (String version : versions) {
          System.out.println(
              version
                  + " compared to "
                  + startVersion
                  + " == "
                  + compareVersions(version, startVersion));
          if (compareVersions(version, startVersion) == 1) {
            if (compareVersions(version, endVersion) == -1) {
              preferredVersion = version;
              break;
            }
          }
        }
        if (preferredVersion != null) return startVersion;
      } catch (IOException e) {
        // the repository doesn't support listing files
        return startVersion;
      }
    }
    return str;
  }

  /**
   * Returns a {@link Dependency} for the given dependency XML node
   *
   * @param dependencyNode the node to parse
   * @param properties the project properties
   * @param parent the dependency parent ( if any )
   * @return a {@link Dependency} for the xml node or null if the dependency node is bad, or if the
   *     dependency is a test dependency
   */
  private Dependency getDependency(
      Node dependencyNode,
      List<ProjectProperty> properties,
      Dependency parent,
      Repository repository) {
    if (!dependencyNode.getNodeName().equals("dependency")
        && !dependencyNode.getNodeName().equals("parent")) {
      System.err.println(
          "Expected a dependency or parent node, found a " + dependencyNode.getNodeName());
      return null;
    }
    Node groupIDNode = null;
    Node artifactIdNode = null;
    Node versionNode = null;
    Node scopeNode = null;
    Node typeNode = null;
    // parse groupId, artifactId, version, scope, type nodes for the given dependency
    for (int y = 0; y < dependencyNode.getChildNodes().getLength(); y++) {
      Node dependencySubNode = dependencyNode.getChildNodes().item(y);
      if (dependencySubNode.getNodeName().equals("groupId")) {
        groupIDNode = dependencySubNode;
        continue;
      }
      if (dependencySubNode.getNodeName().equals("artifactId")) {
        artifactIdNode = dependencySubNode;
        continue;
      }
      if (dependencySubNode.getNodeName().equals("version")) {
        versionNode = dependencySubNode;
        continue;
      }
      if (dependencySubNode.getNodeName().equals("scope")) {
        scopeNode = dependencySubNode;
      }
      if (dependencySubNode.getNodeName().equals("type")) {
        typeNode = dependencySubNode;
      }
    }
    String groupId = "";
    String artifactId = "";
    String version = "";
    String scope = "runtime";
    String type = "jar";
    if (groupIDNode != null) {
      groupId = groupIDNode.getTextContent();
    } else {
      System.err.println("No groupId found for dependency!");
    }
    if (artifactIdNode != null) {
      artifactId = artifactIdNode.getTextContent();
    } else {
      System.err.println("No artifactId found for dependency " + groupId);
    }
    // TODO: make an optional flag to allow test dependencies
    if (scopeNode != null) {
      scope = scopeNode.getTextContent();
      if (scope.equals("test")) {
        return null;
      }
    }
    if (versionNode != null) {
      version =
          parseVersion(properties, versionNode.getTextContent(), groupId, artifactId, repository);
    } else {
      // make sure no parent has defined the version for this artifact before
      for (Entry<Dependency, Dependency> dependency : loadedDependencies.entrySet()) {
        if (dependency.getValue().compare(parent, true)) {
          // we have this dependency loaded from a prent before!
          version = dependency.getValue().getVersion();
          System.out.println("Got version from parent " + version);
          break;
        }
      }
      if (version.isEmpty()) {
        System.err.println("No version found for dependency!");
      }
    }
    if (typeNode != null) {
      type = typeNode.getTextContent();
    }
    return new Dependency(groupId, artifactId, version, type, scope);
  }

  private int compareVersions(String version1, String version2) {
    if (isNumeric(version1) && isNumeric(version2)) {
      double version1Num = Integer.parseInt(version1);
      double version2Num = Integer.parseInt(version2);
      return version1Num >= version2Num ? 1 : (version1Num == version2Num ? 0 : -1);
    } else {
      int compareToResult = version1.compareToIgnoreCase(version2);
      return Integer.compare(compareToResult, 0);
    }
  }

  /**
   * Resolves all dependencies for the given dependency
   *
   * @param dependency the dependency to resolve dependencies for
   * @param callback the callback to call when the resolving is complete
   * @throws IOException if any resolver throws an IOException
   */
  public void resolveDependencies(Dependency dependency, ResolveCallback callback)
      throws IOException {
    this.callback = callback;
    this.artifactFound = false;
    repoIndex = 0;
    resolve(dependency);
  }

  /**
   * Resolves all the dependencies for the given dependency by reading its POM file
   *
   * @param dependency the dependency to resolve dependencies for
   */
  private void resolve(Dependency dependency) {
    // if resolving is done, it makes no sense to resolve any further dependency
    if (done) {
      return;
    }
    // don't resolve a dependency which was already resolved/
    if (loadedDependencies.containsValue(dependency)) {
      return;
    }
    // start the resolving
    startResolverThread(dependency);
  }

  public void startResolverThread(Dependency dependency) {
    // if repo index exceeds the maven repositories size, reset it to 0
    if (repoIndex > (Repository.COMMON_MAVEN_REPOSITORIES.size() - 1)) {
      repoIndex = 0;
    }
    ResolverThread thread =
        new ResolverThread(
            COMMON_MAVEN_REPOSITORIES.get(repoIndex),
            dependency,
            (artifactFound, pomUrl, mavenRepo, dependencies, dependency1) -> {
              if (!artifactFound) {
                if (repoIndex > (COMMON_MAVEN_REPOSITORIES.size() - 1)) {
                  this.artifactFound = false;
                  this.pomDownloadUrl = pomUrl;
                  this.dependencies = dependencies;
                  finishResolve(dependency1);
                } else {
                  repoIndex++;
                  resolve(dependency1);
                }
              } else {
                this.artifactFound = true;
                this.pomDownloadUrl = pomUrl;
                this.dependencies = dependencies;
                this.mavenRepo = mavenRepo;
                finishResolve(dependency1);
              }
            });
    thread.start();
    resolverThreads.add(thread);
  }

  /**
   * Called when the resolving is done
   *
   * @param dependency the dependency which its dependencies were resolved
   * @throws IOException if any thread throws an IOException
   */
  private void finishResolve(Dependency dependency) throws IOException {
    // it makes no sense to finish resolving dependencies when we had finished resolving already
    if (done) {
      return;
    }
    // we failed to find the artifact in any repository
    if (!artifactFound) {
      System.err.println("Didn't find artifact " + dependency + " in any repository!");
      System.err.println("Searched in:");
      for (Repository repo : COMMON_MAVEN_REPOSITORIES) {
        System.err.println(repo + pomDownloadUrl);
      }
      System.exit(0); // don't resolve any further dependencies
      // if we failed to resolve some dependencies
    } else {
      dependency.setRepository(mavenRepo);
      if (!loadedDependencies.containsValue(dependency)) {
        System.out.println("Adding dependency " + dependency);
        loadedDependencies.put(dependency, dependency);
      }
      // the artifact's POM was parsed successfully
      System.out.println("Parsed " + mavenRepo + pomDownloadUrl);
      // remove dependencies which has been loaded from the dependencies to load
      try {
        dependenciesToLoad.removeIf(
            dependency1 -> dependency1 != null && dependency1.equals(dependency));
      } catch (ConcurrentModificationException ignored) {

      }
      // load all dependencies for the loaded dependency
      for (Dependency dependency1 : dependencies) {
        try {
          dependenciesToLoad.add(dependency1);
          resolveDependencies(dependency1, callback);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    // keep track of running threads by removing all dead threads
    try {
      resolverThreads.removeIf(thread -> thread != null && !thread.isAlive());
    } catch (ConcurrentModificationException ignored) {
    }
    if (resolverThreads.size() == 1) { // only the current thread is running
      done = true; // disallow any further methods to be executed
      if (callback != null) {
        callback.done(
            true,
            pomDownloadUrl,
            mavenRepo,
            Arrays.asList(loadedDependencies.values().toArray(new Dependency[0])),
            dependency);
      }
    }
  }

  /** a custom thread to resolve dependencies for the given dependency */
  class ResolverThread extends Thread {

    // the maven repo url for the dependency
    private final Repository repo;
    // the dependency to resolve dependencies for
    private final Dependency dependency;
    // the callback to call when resolving is done
    private final ResolveCallback callback;
    // the dependency node or null if the project doesn't define a dependency
    Dependency parent = null;
    // a flag to indicate weather an artifact was found or not
    boolean artifactFound = false;
    // the dependencies resolved for the dependency
    ArrayList<Dependency> dependencies = new ArrayList<>();
    // the properties resolved for the dependency
    ArrayList<ProjectProperty> properties = new ArrayList<>();
    // the pom download url
    String pomDownloadUrl;

    /**
     * Creates a new Resolver thread
     *
     * @param repo the maven repo url
     * @param dependency the dependency
     * @param callback the callback
     */
    public ResolverThread(Repository repo, Dependency dependency, ResolveCallback callback) {
      this.repo = repo;
      pomDownloadUrl = getPomDownloadUrl(dependency);
      this.dependency = dependency;
      this.callback = callback;
    }

    @Override
    public void run() {
      try {
        URL url = new URL(repo + pomDownloadUrl);
        File localFileDir = Util.getLocalFilesDir();
        File cachesDir = new File(localFileDir, "caches");
        if (!cachesDir.exists() && !cachesDir.mkdir()) {
          System.err.println("Failed to create caches directory.");
          return;
        }
        File artifactDirectory =
            new File(cachesDir, pomDownloadUrl.substring(0, pomDownloadUrl.lastIndexOf('/')));
        if (!artifactDirectory.exists()) {
          artifactDirectory.mkdirs();
        }
        String fileName = pomDownloadUrl.split("/")[pomDownloadUrl.split("/").length - 1];
        File outputFile = new File(artifactDirectory, fileName);
        Document doc;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        if (!outputFile.exists()) {
          // download and save the file first
          ReadableByteChannel rbc = Channels.newChannel(url.openStream());
          // if we reached here with no FileNotFoundException, so the POM file was found in this
          // repo
          System.out.println("Downloading " + repo + pomDownloadUrl);
          FileOutputStream fos = new FileOutputStream(outputFile);
          fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
          System.out.println("Downloaded " + repo + pomDownloadUrl);
        }
        doc = builder.parse(outputFile);
        System.out.println("Parsing " + repo + pomDownloadUrl);
        dependency.setRepository(mavenRepo);
        artifactFound = true;
        Element rootElement = doc.getDocumentElement();
        // iterate over the project elements to find parent, dependencies, dependencyManagement,
        // and properties XML nodes
        for (int i = 0; i < rootElement.getChildNodes().getLength(); i++) {
          Node node = rootElement.getChildNodes().item(i);
          switch (node.getNodeName()) {
            case "packaging":
              dependency.setType(node.getTextContent());
            case "properties":
              for (int x = 0; x < node.getChildNodes().getLength(); x++) {
                Node propertyNode = node.getChildNodes().item(x);
                ProjectProperty projectProperty =
                    new ProjectProperty(propertyNode.getNodeName(), propertyNode.getTextContent());
                properties.add(projectProperty);
              }
              break;
            case "parent":
              // we must load the parent first to get the defined versions ( if any ) !
              Dependency resolvedDependency = getDependency(node, properties, parent, repo);
              if (resolvedDependency != null) {
                parent = resolvedDependency;
                resolveDependencies(resolvedDependency, DependencyResolver.this.callback);
              }
              break;
            case "dependencyManagement":
              for (int x = 0; x < node.getChildNodes().getLength(); x++) {
                Node dependenciesNode = node.getChildNodes().item(x);
                if (dependenciesNode.getNodeName().equals("dependencies")) {
                  for (int y = 0; y < node.getChildNodes().getLength(); y++) {
                    Node dependencyNode = node.getChildNodes().item(y);
                    if (dependencyNode.getNodeName().equals("dependency")) {
                      Dependency resolvedDependency1 =
                          getDependency(dependencyNode, properties, parent, repo);
                      if (resolvedDependency1 != null) {
                        dependencies.add(resolvedDependency1);
                        loadedDependencies.put(dependency, resolvedDependency1);
                      }
                    }
                  }
                }
              }
              break;
            case "dependencies":
              for (int x = 0; x < node.getChildNodes().getLength(); x++) {
                Node dependencyNode = node.getChildNodes().item(x);
                if (dependencyNode.getNodeName().equals("dependency")) {
                  Dependency resolvedDependency1 =
                      getDependency(dependencyNode, properties, parent, repo);
                  if (resolvedDependency1 != null) {
                    dependencies.add(resolvedDependency1);
                    loadedDependencies.put(dependency, resolvedDependency1);
                  }
                }
              }
              break;
          }
        }
      } catch (IOException | ParserConfigurationException | SAXException ignored) {
      }
      try {
        callback.done(artifactFound, pomDownloadUrl, repo, dependencies, dependency);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
