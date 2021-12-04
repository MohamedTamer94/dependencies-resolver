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

package io.mohamed.resolver.core.resolver;

import io.mohamed.resolver.core.util.AI2Dependency;
import io.mohamed.resolver.core.util.LibraryJetifier;
import io.mohamed.resolver.core.util.Util;
import io.mohamed.resolver.core.merger.LibraryMerger;
import io.mohamed.resolver.core.merger.LibraryMerger.MergeResult;
import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.callback.DependencyResolverCallback.MergeStage;
import io.mohamed.resolver.core.model.Dependency;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.eclipse.EclipseProject;

public class GradleDependencyResolver {
  private GradleDependencyResolver() {}

  public void parse(
      File gradleDirectory,
      DependencyResolverCallback dependencyResolverCallback,
      Callback callback,
      boolean jarOnly,
      boolean mergeLibraries,
      boolean jetifyLibraries) {
    dependencyResolverCallback.info("Building Dependencies Using Gradle..");
    try (ProjectConnection connection =
        GradleConnector.newConnector().forProjectDirectory(gradleDirectory).connect()) {
      connection
          .newBuild()
          .setStandardOutput(System.out)
          .setStandardInput(System.in)
          .forTasks("assemble")
          .run(
              new ResultHandler<>() {
                @Override
                public void onComplete(Void unused) {
                  dependencyResolverCallback.info("Done!");
                  resolveDependencies(
                      gradleDirectory,
                      callback,
                      dependencyResolverCallback,
                      jarOnly,
                      mergeLibraries,
                      jetifyLibraries);
                }

                @Override
                public void onFailure(GradleConnectionException e) {
                  dependencyResolverCallback.error(
                      "Failed to resolve dependencies for gradle project..");
                  e.printStackTrace();
                }
              });
    }
  }

  private void resolveDependencies(
      File gradleFile,
      Callback callback,
      DependencyResolverCallback dependencyResolverCallback,
      boolean jarOnly,
      boolean mergeLibraries,
      boolean jetifyLibraries) {
    dependencyResolverCallback.info("Resolving Downloaded dependencies..");
    try (ProjectConnection connection =
        GradleConnector.newConnector().forProjectDirectory(gradleFile).connect()) {
      EclipseProject project = connection.getModel(EclipseProject.class);
      List<File> fileList = new ArrayList<>();
      List<File> finalFileList = fileList;
      project
          .getClasspath()
          .forEach(
              e -> {
                AI2Dependency appInvDependencyManager = new AI2Dependency();
                GradleModuleVersion moduleVersion = e.getGradleModuleVersion();
                Dependency dependency;
                if (moduleVersion != null) {
                  dependency =
                      new Dependency(
                          moduleVersion.getGroup(),
                          moduleVersion.getName(),
                          moduleVersion.getVersion());
                  if (appInvDependencyManager.dependencyExists(dependency)) {
                    return;
                  }
                }
                File outputJarFile =
                    new File(
                        e.getFile().getParentFile(),
                        FilenameUtils.removeExtension(e.getFile().getName()) + ".jar");
                if (jarOnly && Util.isAar(e.getFile())) {
                  dependencyResolverCallback.verbose("Extracting classes.jar from .aar file");
                  try {
                    Util.extractFile(e.getFile().toPath(), "classes.jar", outputJarFile.toPath());
                    dependencyResolverCallback.verbose("Extracted classes.jar from .aar file");
                    if (Util.hasResDirectory(e.getFile())) {
                      dependencyResolverCallback.info(
                          "[WARNING] The AAR "
                              + e.getFile().getName()
                              + " contains resource files. These files will not be included in the final JAR.");
                    }
                  } catch (IOException ioException) {
                    ioException.printStackTrace();
                  }
                }
                System.out.println(
                    "Resolved "
                        + (jarOnly
                            ? outputJarFile.getAbsolutePath()
                            : e.getFile().getAbsolutePath()));
                finalFileList.add((jarOnly ? outputJarFile : e.getFile()));
              });
      if (jetifyLibraries) {
        try {
          fileList = new LibraryJetifier().jetify(fileList, dependencyResolverCallback);
        } catch (IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }
      if (mergeLibraries) {
        dependencyResolverCallback.merging(MergeStage.START);
        MergeResult result =
            LibraryMerger.mergeLibraries(null, fileList, dependencyResolverCallback);
        fileList = result.getMergedLibraries();
        if (result.isSuccess()) {
          dependencyResolverCallback.mergeSuccess();
        } else {
          dependencyResolverCallback.mergeFailed();
        }
      }
      dependencyResolverCallback.info("Done!");
      callback.completed(fileList);
    }
  }

  public interface Callback {
    void completed(List<File> fileList);
  }

  public static class Builder {
    private File gradleFile;
    private File gradleDirectory;
    private Callback callback;
    private DependencyResolverCallback dependencyResolverCallback;
    private boolean jarOnly;
    private boolean mergeLibraries;
    private boolean jetifyLibraries;

    public Builder setGradleFile(File gradleFile) {
      if (gradleFile.isDirectory()) {
        this.gradleDirectory = gradleFile;
      } else {
        this.gradleFile = gradleFile;
      }
      return this;
    }

    public Builder setJetifyLibraries(boolean jetifyLibraries) {
      this.jetifyLibraries = jetifyLibraries;
      return this;
    }

    public Builder setMergeLibraries(boolean mergeLibraries) {
      this.mergeLibraries = mergeLibraries;
      return this;
    }

    public Builder setJarOnly(boolean jarOnly) {
      this.jarOnly = jarOnly;
      return this;
    }

    public Builder setCallback(Callback callback) {
      this.callback = callback;
      return this;
    }

    public Builder setDependencyResolverCallback(
        DependencyResolverCallback dependencyResolverCallback) {
      this.dependencyResolverCallback = dependencyResolverCallback;
      return this;
    }

    public void resolve() {
      if (gradleDirectory == null && gradleFile != null) {
        gradleDirectory = new File(Util.getGradleDirectory(), UUID.randomUUID().toString());
        if (!gradleDirectory.exists()) {
          boolean result = gradleDirectory.mkdir();
          if (!result) {
            dependencyResolverCallback.error("Failed to create gradle directory.");
            return;
          }
        }
        try {
          Files.copy(
              gradleFile.toPath(),
              new File(gradleDirectory, "build.gradle").toPath(),
              StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      new GradleDependencyResolver()
          .parse(
              gradleDirectory,
              dependencyResolverCallback,
              callback,
              jarOnly,
              mergeLibraries,
              jetifyLibraries);
    }
  }
}
