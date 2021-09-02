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

import io.mohamed.callback.DownloadCallback;
import io.mohamed.callback.FilesDownloadedCallback;
import io.mohamed.model.Dependency;
import io.mohamed.model.Repository;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

public class DependencyDownloader {

  private static boolean filterAppInventorDependencies;
  List<File> downloadedFiles = new ArrayList<>();
  ArrayList<Dependency> dependenciesToLoad = new ArrayList<>();
  List<DownloaderThread> downloaderThreads = new ArrayList<>();
  int currentDependency = 0;
  private boolean done = false;
  private FilesDownloadedCallback callback;

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

  public void resolveDependenciesFiles(
      List<Dependency> dependencies,
      FilesDownloadedCallback callback,
      boolean filterAppInventorDependencies) {
    this.callback = callback;
    DependencyDownloader.filterAppInventorDependencies = filterAppInventorDependencies;
    if (done) {
      return;
    }
    for (Dependency dependency : dependencies) {
      if (!dependency.getType().equals("pom") && !dependenciesToLoad.contains(dependency)) {
        dependenciesToLoad.add(dependency);
      }
    }
    doResolve(dependenciesToLoad.get(0));
  }

  private void doResolve(Dependency dependency) {
    DownloaderThread thread =
        new DownloaderThread(
            dependency,
            dependency.getRepository(),
            ((downloadedFile, dependency1) -> {
              dependenciesToLoad.remove(dependency1);
              if (downloadedFile != null) { // the file wasn't found
                downloadedFiles.add(downloadedFile);
              }
              finishDownload(callback);
              if (currentDependency < (dependenciesToLoad.size() - 1)) {
                currentDependency++;
                doResolve(dependenciesToLoad.get(currentDependency));
              }
            }));
    thread.start();
    downloaderThreads.add(thread);
  }

  private void finishDownload(FilesDownloadedCallback callback) {
    if (done) {
      return;
    }
    if (currentDependency >= (dependenciesToLoad.size() - 1)) { // only the current thread is running
      done = true;
      if (callback != null) {
        callback.done(downloadedFiles);
        Thread.currentThread().interrupt();
      }
    }
  }

  static class DownloaderThread extends Thread {

    private final DownloadCallback callback;
    Dependency dependency;
    Repository repository;

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
        File localFileDir = Util.getLocalFilesDir();
        File cachesDir = new File(localFileDir, "caches");
        if (!cachesDir.exists() && !cachesDir.mkdir()) {
          System.err.println("Failed to create caches directory.");
          return;
        }
        File artifactDirectory =
            new File(cachesDir, fileDownloadPath.substring(0, fileDownloadPath.lastIndexOf('/')));
        if (!artifactDirectory.exists()) {
          if (!artifactDirectory.mkdirs()) {
            System.out.println("[WARNING] Failed to create some artifact directories");
          }
        }
        String fileName = fileDownloadPath.split("/")[fileDownloadPath.split("/").length - 1];
        File outputFile = new File(artifactDirectory, fileName);
        if (filterAppInventorDependencies) {
          AppInvDependenciesCloner cloner = new AppInvDependenciesCloner();
          if (cloner.dependencyExists(dependency)) {
            // this file was already included in app inventor libraries, we can skip this
            callback.done(null, dependency);
            interrupt();
            return;
          }
        }
        if (outputFile.exists()) {
          // this file was already downloaded in cache, we can directly report success
          callback.done(outputFile, dependency);
          interrupt();
          return;
        }
        URL fileDownloadUrl = null;
        for (Repository repository : Repository.COMMON_MAVEN_REPOSITORIES) {
          try {
            fileDownloadUrl = new URL(repository.getUrl() + fileDownloadPath);
            try (ReadableByteChannel rbc = Channels.newChannel(fileDownloadUrl.openStream())) {
              System.out.println("Downloading " + fileDownloadUrl);
              try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                break;
              }
            }
          } catch (FileNotFoundException ignored) {
          }
        }
        System.out.println("Downloaded " + fileDownloadUrl);
        callback.done(outputFile, dependency);
        interrupt();
        return;
      } catch (IOException ignored) {
      }
      callback.done(null, dependency);
      interrupt();
    }
  }
}
