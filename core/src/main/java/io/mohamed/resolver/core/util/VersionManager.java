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

package io.mohamed.resolver.core.util;


import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.model.Dependency;
import io.mohamed.resolver.core.model.DependencyVersion;
import io.mohamed.resolver.core.model.Repository;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Retrieves version information for dependencies
 *
 * @author Mohamed Tamer
 */
public class VersionManager {
  // the fetcher instance used to download maven metadata files from the web
  public static final Fetcher fetcher = Fetcher.getInstance();

  /**
   * Retrieves the version information for the dependency
   *
   * @param dependency the dependency to resolve version information for
   * @param repository the repository for the dependency
   * @param callback the callback to print logging information to
   * @return the version information for the dependency
   * @throws ParserConfigurationException if an error occurs while parsing the metadata XML
   * @throws SAXException if an error occurs while parsing the metadata XML
   */
  public static DependencyVersion getVersions(
      Dependency dependency, Repository repository, DependencyResolverCallback callback)
      throws ParserConfigurationException, SAXException {
    try {
      File mavenMetaDataFile = downloadMavenMetaData(dependency, repository);
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document document = db.parse(mavenMetaDataFile);
      Node versioningNode = document.getElementsByTagName("versioning").item(0);
      Node latestVersionNode = document.getElementsByTagName("latest").item(0);
      String latestVersion = latestVersionNode.getTextContent();
      ArrayList<String> versions = new ArrayList<>();
      NodeList versionsNodes = ((Element) versioningNode).getElementsByTagName("version");
      for (int i = 0; i < versionsNodes.getLength(); i++) {
        Node versionNode = versionsNodes.item(i);
        versions.add(versionNode.getTextContent().trim());
      }
      return new DependencyVersion(versions, latestVersion);
    } catch (FileNotFoundException e) {
      callback.info(
          "Invalid Dependency: The dependency doesn't include a maven-metadata.xml file.");
      return null;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Downloads the maven-metadata.xml file for the dependency
   * @param dependency the dependency
   * @param repository the repository for the dependency
   * @return the downloaded maven metadata file
   * @throws IOException if an error is thrown while downloading the files
   */
  public static File downloadMavenMetaData(Dependency dependency, Repository repository)
      throws IOException {
    URL mavenMetaDataUrl =
        new URL(
            repository.getUrl()
                + "/"
                + dependency.getGroupId().replaceAll("\\.", "/")
                + "/"
                + dependency.getArtifactId()
                + "/maven-metadata.xml");
    File outputFile =
        new File(
            Util.getCachesDirectory(),
            dependency.getGroupId().replaceAll("\\.", "/")
                + File.separator
                + dependency.getArtifactId()
                + "/maven-metadata.xml");
    if (!outputFile.exists()) {
      if (!outputFile.getParentFile().exists()) {
        outputFile.getParentFile().mkdirs();
      }
      fetcher.downloadFile(mavenMetaDataUrl, outputFile);
    }
    return outputFile;
  }
}
