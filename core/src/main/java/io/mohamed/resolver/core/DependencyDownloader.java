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
package io.mohamed.resolver.core;

import com.android.ide.common.res2.MergingException;
import com.android.manifmerger.ManifestMerger2.MergeFailureException;
import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.callback.DependencyResolverCallback.MergeStage;
import io.mohamed.resolver.core.callback.DownloadCallback;
import io.mohamed.resolver.core.callback.FilesDownloadedCallback;
import io.mohamed.resolver.core.model.Dependency;
import io.mohamed.resolver.core.model.Repository;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 * Downloads library files for the given dependencies
 *
 * @author Mohamed Tamer
 */
public class DependencyDownloader {

  // a flag to indicate weather to filter appinventor dependencies from the downloaded dependencies
  private static boolean filterAppInventorDependencies;
  // list of all repositories which dependencies will be validated against
  private static List<Repository> allRepositories = new ArrayList<>();
  // weather to include jar files only or not
  private static boolean jarOnly;
  // weather to log debug messages or not
  private static boolean verbose;
  // the dependency resolver callback
  private static DependencyResolverCallback dependencyResolverCallback;
  // the list of the downloaded files
  List<File> downloadedFiles = new ArrayList<>();
  // the dependencies which should be downloaded
  ArrayList<Dependency> dependenciesToLoad = new ArrayList<>();
  // the dependencies which hasn't been downloaded yet.
  ArrayList<Dependency> remainingDependencies = new ArrayList<>();
  // the threads which is currently downloading dependencies
  List<DownloaderThread> downloaderThreads = new ArrayList<>();
  // a flag to indicate that this instance has finished downloading
  private boolean done = false;
  // the callback which is called when downloading all files finishes
  private FilesDownloadedCallback callback;
  // a flag to indicate weather to merge files into one JAR/AAR or not
  private boolean merge;
  // the main dependency
  private Dependency mainDependency;

  /**
   * Creates a new DependencyDownloader
   *
   * @see DependencyDownloader.Builder
   */
  private DependencyDownloader() {}

  /**
   * Returns the path for the dependency (JAR/AAR), this path is examined against the supported
   * maven repositories
   *
   * @param dependency The dependency to find its url
   * @return the dependency url
   */
  private static String getFileDownloadUrl(Dependency dependency) {
    return dependency.getGroupId().replaceAll("\\.", "/")
        + "/"
        + dependency.getArtifactId()
        + "/"
        + dependency.getVersion()
        + "/"
        + dependency.getArtifactId()
        + "-"
        + dependency.getVersion()
        + (dependency.getType().equalsIgnoreCase("aar") ? ".aar" : ".jar");
  }

  /**
   * Creates an output file for the given dependency, output files are located in
   * /path/to/app/data/caches/group/id/artifactId/version/artifactId-version-.type
   *
   * @param dependency the dependency
   * @return the output file created for the dependency.
   */
  private static File getOutputFileForDependency(Dependency dependency, String extension) {
    String fileDownloadPath = getFileDownloadUrl(dependency);
    File cachesDir = Util.getCachesDirectory();
    File artifactDirectory =
        new File(cachesDir, fileDownloadPath.substring(0, fileDownloadPath.lastIndexOf('/')));
    if (!artifactDirectory.exists()) {
      if (!artifactDirectory.mkdirs()) {
        dependencyResolverCallback.info("[WARNING] Failed to create some artifact directories");
      }
    }
    String fileName = fileDownloadPath.split("/")[fileDownloadPath.split("/").length - 1];
    if (!extension.isEmpty() && !FilenameUtils.getExtension(fileName).equals(extension)) {
      fileName =
          FilenameUtils.removeExtension(fileName) + FilenameUtils.EXTENSION_SEPARATOR + extension;
    }
    return new File(artifactDirectory, fileName);
  }

  /**
   * Downloads dependency files for the given dependencies
   *
   * @param dependencies the dependencies to download
   * @param callback the callback to call when file download finishes
   * @param filterAppInventorDependencies a flag to filter appinventor dependencies from the
   *     downloaded dependencies
   * @param merge a flag to merge all files into one JAR/AAR
   * @param mainDependency the main dependency
   * @param verbose a flag to log debug messages
   * @param repositories list of custom repository urls
   * @param jarOnly includes jar files only
   * @param dependencyResolverCallback the dependency resolver callback
   */
  private void resolveDependenciesFiles(
      List<Dependency> dependencies,
      FilesDownloadedCallback callback,
      boolean filterAppInventorDependencies,
      boolean merge,
      Dependency mainDependency,
      boolean verbose,
      List<String> repositories,
      boolean jarOnly,
      DependencyResolverCallback dependencyResolverCallback) {
    allRepositories = new ArrayList<>();
    this.callback = callback;
    this.merge = merge;
    this.mainDependency = mainDependency;
    DependencyDownloader.dependencyResolverCallback = dependencyResolverCallback;
    DependencyDownloader.verbose = verbose;
    DependencyDownloader.jarOnly = jarOnly;
    allRepositories.addAll(Repository.COMMON_MAVEN_REPOSITORIES);
    for (String repoUrl : repositories) {
      if (!repoUrl.endsWith("/")) {
        repoUrl = repoUrl + "/";
      }
      allRepositories.add(new Repository(repoUrl));
    }
    DependencyDownloader.filterAppInventorDependencies = filterAppInventorDependencies;
    // don't perform download if we have already finished downloading
    if (done) {
      return;
    }
    // filter POM dependencies
    for (Dependency dependency : dependencies) {
      if (!dependency.getType().equals("pom") && !dependenciesToLoad.contains(dependency)) {
        dependenciesToLoad.add(dependency);
      }
    }
    remainingDependencies = dependenciesToLoad;
    // start downloading
    doResolve(dependenciesToLoad.get(0));
  }

  /**
   * Recursively downloads all the library files for the dependencies
   *
   * @param dependency dependency to download
   */
  private void doResolve(Dependency dependency) {
    DownloaderThread thread =
        new DownloaderThread(
            dependency,
            dependency.getRepository(),
            ((downloadedFile, dependency1) -> {
              remainingDependencies.remove(dependency1);
              if (downloadedFile != null) { // the file wasn't found
                downloadedFiles.add(downloadedFile);
              }
              finishDownload();
              if (!remainingDependencies.isEmpty()) {
                doResolve(remainingDependencies.get(0));
              }
            }));
    thread.start();
    downloaderThreads.add(thread);
  }

  /** Called when a download finishes */
  private void finishDownload() {
    if (done) {
      return;
    }
    if (dependenciesToLoad.isEmpty()) { // all dependencies has been downloaded
      done = true;
      if (merge) {
        dependencyResolverCallback.merging(MergeStage.START);
        boolean result = mergeLibraries();
        if (result) {
          dependencyResolverCallback.mergeSuccess();
        } else {
          dependencyResolverCallback.mergeFailed();
        }
      }
      if (callback != null) {
        callback.done(downloadedFiles);
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Checks if any of the downloaded files is an AAR file
   *
   * @return true if any AAR file was downloaded
   */
  private boolean hasAnyAar() {
    return downloadedFiles.stream().anyMatch(Util::isAar);
  }

  /**
   * Merges all the downloaded libraries into one file If all the downloaded files were a jar:
   * Invokes the JARMerger to merge all jar files into one If any of the downloaded files were an
   * aar: Invokes the AARMerger to merge all the aar and jar library files into one AAR
   *
   * @return true if merging libraries was successful
   */
  private boolean mergeLibraries() {
    try {
      if (hasAnyAar()) {
        File result =
            new AARMerger()
                .merge(verbose, downloadedFiles, mainDependency, dependencyResolverCallback);
        if (result != null) {
          downloadedFiles = new ArrayList<>(Collections.singletonList(result));
          return true;
        }
      } else {
        File result =
            new File(
                Util.getMergedLibrariesDirectory(),
                mainDependency.getArtifactId() + "-" + mainDependency.getVersion() + ".jar");
        new JARMerger().merge(downloadedFiles, result);
        downloadedFiles = new ArrayList<>(Collections.singletonList(result));
        return true;
      }
      return false;
    } catch (IOException | MergingException | MergeFailureException e) {
      e.printStackTrace();
      return false;
    }
  }

  /** The thread which downloads library files */
  static class DownloaderThread extends Thread {

    // the callback invoked when the file was download / or an error has occurred.
    private final DownloadCallback callback;
    // the dependency to download
    Dependency dependency;
    // the dependency's repository
    Repository repository;

    /**
     * Creates a new Downloader Thread
     *
     * @param dependency the dependency to download
     * @param repository the dependency's repository
     * @param callback the callback to invoke when the download finishes
     */
    public DownloaderThread(
        Dependency dependency, Repository repository, DownloadCallback callback) {
      this.dependency = dependency;
      this.repository = repository;
      this.callback = callback;
    }

    @Override
    public void run() {
      super.run();
      try {
        String fileDownloadPath = getFileDownloadUrl(dependency);
        File outputFile = getOutputFileForDependency(dependency, "");
        File outputJarFile = getOutputFileForDependency(dependency, "jar");
        if (filterAppInventorDependencies) {
          AppInvDependencyManager cloner = new AppInvDependencyManager();
          if (cloner.dependencyExists(dependency)) {
            // this file was already included in app inventor libraries, we can skip this
            callback.done(null, dependency);
            interrupt();
            return;
          }
        }
        if (!jarOnly) {
          if (outputFile.exists()) {
            // this file was already downloaded in cache, we can directly report success
            callback.done(outputFile, dependency);
            interrupt();
            return;
          }
        } else if (outputJarFile.exists()) {
          // this file was already downloaded in cache, we can directly report success
          callback.done(outputFile, dependency);
          interrupt();
          return;
        }
        URL fileDownloadUrl = null;
        for (Repository repository : allRepositories) {
          try {
            fileDownloadUrl = new URL(repository.getUrl() + fileDownloadPath);
            try (ReadableByteChannel rbc = Channels.newChannel(fileDownloadUrl.openStream())) {
              dependencyResolverCallback.dependencyFileDownloading(fileDownloadUrl.toString());
              try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                break;
              }
            }
          } catch (FileNotFoundException ignored) {
          }
        }
        if (!outputFile.exists()) {
          callback.done(null, dependency);
          return;
        }
        if (jarOnly && Util.isAar(outputFile)) {
          dependencyResolverCallback.verbose("Extracting classes.jar from .aar file");
          Util.extractFile(outputFile.toPath(), "classes.jar", outputJarFile.toPath());
          dependencyResolverCallback.verbose("Extracted classes.jar from .aar file");
          if (Util.hasResDirectory(outputFile)) {
            dependencyResolverCallback.info("[WARNING] The AAR " + outputFile.getName() + " contains resource files. These files will not be included in the final JAR."
                + "");
          }
        }
        if (fileDownloadUrl != null) {
          dependencyResolverCallback.dependencyFileDownloaded(fileDownloadUrl.toString());
        }
        callback.done(jarOnly ? outputJarFile : outputFile, dependency);
        interrupt();
        return;
      } catch (IOException ignored) {
      }
      callback.done(null, dependency);
      interrupt();
    }
  }

  /** Creates a DependencyDownloader instance */
  public static class Builder {
    // weather to merge files into one JAR/AAR or not
    private boolean merge = false;
    // the main dependency
    private Dependency mainDependency = null;
    // the callback to invoke when the file download finishes
    private FilesDownloadedCallback callback = null;
    // weather to filter appinventor dependencies from the download or not
    private boolean filterAppInventorDependencies = false;
    // the dependencies to download
    private List<Dependency> dependencies = new ArrayList<>();
    // weather to log debug messages
    private boolean verbose = false;
    // custom repositories for resolving dependencies
    private List<String> repositories = new ArrayList<>();
    // includes jar files only
    private boolean jarOnly = false;
    // the dependency resolver callback
    private DependencyResolverCallback dependencyResolverCallback;

    /**
     * Specifies weather to log debug messages
     *
     * @param verbose true to log debug messages
     * @return the Builder instance
     */
    public Builder setVerbose(boolean verbose) {
      this.verbose = verbose;
      return this;
    }

    /**
     * Specifies the dependency resolver callback
     *
     * @param dependencyResolverCallback the dependency resolver callback
     * @return the builder instance
     */
    public Builder setDependencyResolverCallback(
        DependencyResolverCallback dependencyResolverCallback) {
      this.dependencyResolverCallback = dependencyResolverCallback;
      return this;
    }

    /**
     * Only includes jar files when downloading
     *
     * @param jarOnly true to include jar files only
     * @return the Builder instance
     */
    public Builder setJarOnly(boolean jarOnly) {
      this.jarOnly = jarOnly;
      return this;
    }

    /**
     * Specifies custom repository urls to search within
     *
     * @param repositories the repository url list
     * @return the Builder instance
     */
    public Builder setRepositories(List<String> repositories) {
      this.repositories = repositories;
      return this;
    }

    /**
     * Weather to merge library files into one JAR/AAR file
     *
     * @param merge true to merge library files
     * @return the Builder instance
     */
    public Builder setMerge(boolean merge) {
      this.merge = merge;
      return this;
    }

    /**
     * Specifies the main dependency, this should only by used if merging was enabled
     *
     * @param mainDependency the main dependency
     * @return the Builder instance
     */
    public Builder setMainDependency(Dependency mainDependency) {
      this.mainDependency = mainDependency;
      return this;
    }

    /**
     * Specifies the callback to listen when the download finishes, or null to remove existing
     * callback
     *
     * @param callback the callback
     * @return the Builder instance
     */
    public Builder setCallback(FilesDownloadedCallback callback) {
      this.callback = callback;
      return this;
    }

    /**
     * Specifies weather to filter appinventor dependencies from being downloaded or not
     *
     * @param filterAppInventorDependencies true to filter appinventor dependencies
     * @return the Builder instance
     */
    public Builder setFilterAppInventorDependencies(boolean filterAppInventorDependencies) {
      this.filterAppInventorDependencies = filterAppInventorDependencies;
      return this;
    }

    /**
     * Specifies the dependencies to download
     *
     * @param dependencies the dependencies to download
     * @return the Builder instance
     */
    public Builder setDependencies(List<Dependency> dependencies) {
      this.dependencies = dependencies;
      return this;
    }

    /** Starts resolving dependency files, using the given input */
    public void resolve() {
      if (dependencyResolverCallback == null) {
        throw new IllegalArgumentException("Dependency Resolver Callback must be set.");
      }
      new DependencyDownloader()
          .resolveDependenciesFiles(
              dependencies,
              callback,
              filterAppInventorDependencies,
              merge,
              mainDependency,
              verbose,
              repositories,
              jarOnly,
              dependencyResolverCallback);
    }
  }
}
