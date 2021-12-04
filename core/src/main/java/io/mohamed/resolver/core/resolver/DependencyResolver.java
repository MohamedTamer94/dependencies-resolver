/*
 *  Copyright (c) 2021 Mohamed Tamer
 *   Permission is hereby granted, free of charge, to any person obtaining
 *   a copy of this software and associated documentation files (the
 *   "Software"), to deal in the Software without restriction, including
 *   without limitation the rights to use, copy, modify, merge, publish,
 *   distribute, sublicense, and/or sell copies of the Software, and to
 *   permit persons to whom the Software is furnished to do so, subject to
 *   the following conditions:
 *
 *   The above copyright notice and this permission notice shall be
 *   included in all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *   LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *   OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *   WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

// -*- mode: java; c-basic-offset: 2; -*-
package io.mohamed.resolver.core.resolver;

import io.mohamed.resolver.core.util.VersionManager;
import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.callback.ResolveCallback;
import io.mohamed.resolver.core.model.Dependency;
import io.mohamed.resolver.core.model.DependencyVersion;
import io.mohamed.resolver.core.model.ProjectProperty;
import io.mohamed.resolver.core.model.Repository;
import io.mohamed.resolver.core.util.Fetcher;
import io.mohamed.resolver.core.util.Util;
import java.io.File;
import java.io.IOException;
import java.net.URL;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Resolves and downloads the POM file all the dependencies for the given artifact.
 *
 * @author Mohamed Tamer
 */
public class DependencyResolver {

  // keeps track of all the loaded dependencies and the dependency that loaded it
  private static HashMap<Dependency, Dependency> loadedDependencies = new HashMap<>();
  // the fetcher instance which is used to download files from the web
  private final Fetcher fetcher = Fetcher.getInstance();
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
  // the list of repositories to search against
  private List<Repository> allRepositories;
  // the callback for the dependency resolver, used to send logging information so various modules
  // could interrupt it in the correct way for the user
  private DependencyResolverCallback dependencyResolverCallback;

  /**
   * Creates a new DependencyResolver
   *
   * @see DependencyResolver.Builder
   */
  private DependencyResolver() {
    loadedDependencies = new HashMap<>();
  }

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
    if (dependency == null) {
      return "";
    }
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
    DependencyVersion dependencyVersion = null;
    try {
      dependencyVersion =
          VersionManager.getVersions(
              new Dependency(groupID, artifactId, null), repository, dependencyResolverCallback);
    } catch (ParserConfigurationException | SAXException e) {
      e.printStackTrace();
    }
    Pattern p = Pattern.compile(".*\\$\\{.*}.*");
    // create matcher for pattern p and given string
    Matcher m = p.matcher(str);
    // if an occurrence if a pattern was found in a given string...
    if (m.find()) {
      boolean propertyFound = false;
      String propertyName = str.replaceAll("(.*)\\$\\{([^&]*)}(.*)", "$1$2$3");
      for (ProjectProperty projectProperty : properties) {
        if (projectProperty.getName().equals(propertyName)) {
          str = projectProperty.getValue();
          propertyFound = true;
          break;
        }
      }
      if (!propertyFound) {
        if (dependencyVersion != null) {
          str = dependencyVersion.getLatestVersion();
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
      List<String> versions;
      if (dependencyVersion != null) {
        versions = dependencyVersion.getVersionNames();
        String preferredVersion = null;
        for (String version : versions) {
          if (compareVersions(version, startVersion) == 1) {
            if (compareVersions(version, endVersion) == -1) {
              preferredVersion = version;
              break;
            }
          }
        }
        if (preferredVersion != null) return dependencyVersion.getLatestVersion();
      }
      str = startVersion;
    }
    for (Entry<Dependency, Dependency> dependencyEntry : loadedDependencies.entrySet()) {
      Dependency dependency = new Dependency(groupID, artifactId, null);
      if (dependencyEntry.getKey().compare(dependency, true)) {
        // Dependency Version Conflict!
        if (dependencyVersion != null) {
          String versionToUse = str;
          for (String version : dependencyVersion.getVersionNames()) {
            if (compareVersions(versionToUse, version) == -1) {
              versionToUse = version;
            }
          }
          str = versionToUse;
        }
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
      dependencyResolverCallback.error(
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
      dependencyResolverCallback.error("No groupId found for dependency!");
    }
    if (artifactIdNode != null) {
      artifactId = artifactIdNode.getTextContent();
    } else {
      dependencyResolverCallback.error("No artifactId found for dependency " + groupId);
    }
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
          break;
        }
      }
      if (version.isEmpty()) {
        return null;
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
   * @param repositories custom repositories to search against
   * @param dependencyResolverCallback the dependency resolver callback
   */
  private void resolveDependencies(
      Dependency dependency,
      ResolveCallback callback,
      List<Repository> repositories,
      DependencyResolverCallback dependencyResolverCallback) {
    this.callback = callback;
    this.dependencyResolverCallback = dependencyResolverCallback;
    this.artifactFound = false;
    repoIndex = 0;
    allRepositories = repositories;
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
    if (repoIndex >= (allRepositories.size() - 1)) {
      repoIndex = 0;
    }
    // start the resolving
    startResolverThread(dependency);
  }

  public void startResolverThread(Dependency dependency) {
    ResolverThread thread =
        new ResolverThread(
            allRepositories.get(repoIndex),
            dependency,
            (artifactFound, pomUrl, mavenRepo, dependencies, dependency1) -> {
              if (!artifactFound) {
                if (repoIndex >= (allRepositories.size() - 1)) {
                  this.artifactFound = false;
                  this.pomDownloadUrl = pomUrl;
                  this.dependencies = dependencies;
                  this.mavenRepo = mavenRepo;
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
      dependencyResolverCallback.error(
          "Didn't find artifact " + dependency + " in any repository!");
      dependencyResolverCallback.error("Searched in:");
      for (Repository repo : allRepositories) {
        dependencyResolverCallback.error(repo + getPomDownloadUrl(dependency));
      }
      done = true;
      return;
    } else {
      dependency.setRepository(mavenRepo);
      if (!loadedDependencies.containsValue(dependency)) {
        loadedDependencies.put(dependency, dependency);
      }
      // the artifact's POM was parsed successfully
      dependencyResolverCallback.dependencyPomParsed(mavenRepo + pomDownloadUrl);
      // remove dependencies which has been loaded from the dependencies to load
      try {
        dependenciesToLoad.removeIf(
            dependency1 -> dependency1 != null && dependency1.equals(dependency));
      } catch (ConcurrentModificationException ignored) {

      }
      // load all dependencies for the loaded dependency
      for (Dependency dependency1 : dependencies) {
        dependenciesToLoad.add(dependency1);
        resolveDependencies(dependency1, callback, allRepositories, dependencyResolverCallback);
      }
    }
    // keep track of running threads by removing all dead threads
    try {
      resolverThreads.removeIf(thread -> thread == null || !thread.isAlive());
    } catch (ConcurrentModificationException ignored) {
    }
    if (resolverThreads.size() <= 1) { // only the current thread is running
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

  public static class Builder {
    // the dependency to resolve
    private Dependency dependency;
    // the callback to call when resolving is done
    private ResolveCallback callback;
    // custom repositories to search against
    private List<String> repositoriesUrls = new ArrayList<>();
    // the dependency resolver callback
    private DependencyResolverCallback dependencyResolverCallback;

    public Builder setCallback(ResolveCallback callback) {
      this.callback = callback;
      return this;
    }

    /**
     * Specifies the dependency resolver callback
     *
     * @param dependencyResolverCallback the dependency resolver callback
     * @return the Builder instance
     */
    public Builder setDependencyResolverCallback(
        DependencyResolverCallback dependencyResolverCallback) {
      this.dependencyResolverCallback = dependencyResolverCallback;
      return this;
    }

    public Builder setRepositories(List<String> repositories) {
      this.repositoriesUrls = repositories;
      return this;
    }

    public Builder setDependency(Dependency dependency) {
      this.dependency = dependency;
      return this;
    }

    public void resolve() {
      if (dependencyResolverCallback == null) {
        throw new IllegalArgumentException("Dependency Resolver Callback must be set.");
      }
      List<Repository> repositories = new ArrayList<>(Repository.COMMON_MAVEN_REPOSITORIES);
      for (String repoUrl : repositoriesUrls) {
        if (!repoUrl.endsWith("/")) {
          repoUrl = repoUrl + "/";
        }
        repositories.add(new Repository(repoUrl));
      }
      new DependencyResolver()
          .resolveDependencies(dependency, callback, repositories, dependencyResolverCallback);
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
      this.dependency = dependency;
      this.callback = callback;
    }

    @Override
    public void run() {
      try {
        if (dependency.getVersion() == null || dependency.getVersion().isEmpty()) {
          DependencyVersion version =
              VersionManager.getVersions(dependency, repo, dependencyResolverCallback);
          if (version != null) {
            dependency.setVersion(version.getLatestVersion());
          }
        }
        pomDownloadUrl = getPomDownloadUrl(dependency);
        // the plexus library caused many builds to fail, because of the fact, its main artifact has
        // an invalid XML, better skip it
        if (dependency.getGroupId().contains("plexus")) {
          dependencyResolverCallback.info(
              "[WARNING] Skipping Invalid file " + repo + pomDownloadUrl);
          try {
            // we report the artifact was found here, yet with null dependency, so we indicate we
            // want to skip this dependency.
            callback.done(true, pomDownloadUrl, repo, dependencies, dependency);
            return;
          } catch (IOException ioException) {
            ioException.printStackTrace();
          }
        }
        dependencyResolverCallback.verbose("Preloading " + repo + pomDownloadUrl);
        URL url = new URL(repo + pomDownloadUrl);
        File cachesDir = Util.getCachesDirectory();
        File artifactDirectory =
            new File(cachesDir, pomDownloadUrl.substring(0, pomDownloadUrl.lastIndexOf('/')));
        if (!artifactDirectory.exists()) {
          if (!artifactDirectory.mkdirs()) {
            dependencyResolverCallback.info("[WARNING] Failed to create some artifact directories");
          }
        }
        String fileName = pomDownloadUrl.split("/")[pomDownloadUrl.split("/").length - 1];
        File outputFile = new File(artifactDirectory, fileName);
        Document doc;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        if (!outputFile.exists()) {
          dependencyResolverCallback.dependencyPomDownloading(repo + pomDownloadUrl);
          fetcher.downloadFile(url, outputFile);
          dependencyResolverCallback.dependencyPomDownloaded(repo + pomDownloadUrl);
        }
        doc = builder.parse(outputFile);
        dependencyResolverCallback.dependencyPomParsing(repo + pomDownloadUrl);
        dependency.setRepository(mavenRepo);
        artifactFound = true;
        Element rootElement = doc.getDocumentElement();
        // parse packaging elements to infer artifact type
        NodeList packagingElements = rootElement.getElementsByTagName("packaging");
        for (int i = 0; i < packagingElements.getLength(); i++) {
          Node node = packagingElements.item(i);
          dependency.setType(node.getTextContent());
        }
        // parse properties elements to find out defined properties
        NodeList propertiesElements = rootElement.getElementsByTagName("properties");
        for (int i = 0; i < propertiesElements.getLength(); i++) {
          Node propertiesNode = propertiesElements.item(i);
          for (int x = 0; x < propertiesNode.getChildNodes().getLength(); x++) {
            Node propertyNode = propertiesNode.getChildNodes().item(x);
            ProjectProperty projectProperty =
                new ProjectProperty(propertyNode.getNodeName(), propertyNode.getTextContent());
            properties.add(projectProperty);
          }
        }
        // parse parent elements to find out the parent for the artifact
        // we must load the parent first to get the defined versions ( if any ) !
        NodeList parentNodes = rootElement.getElementsByTagName("parent");
        for (int i = 0; i < parentNodes.getLength(); i++) {
          Node parentNode = parentNodes.item(i);
          Dependency resolvedDependency = getDependency(parentNode, properties, parent, repo);
          if (resolvedDependency != null) {
            parent = resolvedDependency;
            resolveDependencies(
                resolvedDependency,
                DependencyResolver.this.callback,
                allRepositories,
                dependencyResolverCallback);
          }
        }
        // parse dependencies definitions in the dependencyManagement elements
        NodeList dependencyManagementsNodes =
            rootElement.getElementsByTagName("dependencyManagement");
        for (int i = 0; i < dependencyManagementsNodes.getLength(); i++) {
          Node dependencyManagementNode = dependencyManagementsNodes.item(i);
          for (int x = 0; x < dependencyManagementNode.getChildNodes().getLength(); x++) {
            Node dependenciesNode = dependencyManagementNode.getChildNodes().item(x);
            if (dependenciesNode.getNodeName().equals("dependencies")) {
              for (int y = 0; y < dependencyManagementNode.getChildNodes().getLength(); y++) {
                Node dependencyNode = dependencyManagementNode.getChildNodes().item(y);
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
        }
        NodeList dependenciesNodes = rootElement.getElementsByTagName("dependencies");
        for (int i = 0; i < dependenciesNodes.getLength(); i++) {
          Node dependenciesNode = dependenciesNodes.item(i);
          for (int x = 0; x < dependenciesNode.getChildNodes().getLength(); x++) {
            Node dependencyNode = dependenciesNode.getChildNodes().item(x);
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
      } catch (IOException | ParserConfigurationException ignored) {
      } catch (SAXException e) {
        dependencyResolverCallback.info("[WARNING] Skipping Invalid file " + repo + pomDownloadUrl);
        try {
          // we report the artifact was found here, yet with null dependency, so we indicate we want
          // to skip this dependency.
          callback.done(true, pomDownloadUrl, repo, dependencies, dependency);
        } catch (IOException ioException) {
          ioException.printStackTrace();
        }
      }
      try {
        callback.done(artifactFound, pomDownloadUrl, repo, dependencies, dependency);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
