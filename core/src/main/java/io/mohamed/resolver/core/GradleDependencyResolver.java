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

package io.mohamed.resolver.core;

import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.tooling.model.eclipse.EclipseProject;

public class GradleDependencyResolver {
  private GradleDependencyResolver() {}

  public void parse(File gradleDirectory, DependencyResolverCallback dependencyResolverCallback, Callback callback) {
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
                  resolveDependencies(gradleDirectory, callback, dependencyResolverCallback);
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

  private void resolveDependencies(File gradleFile, Callback callback, DependencyResolverCallback dependencyResolverCallback) {
    dependencyResolverCallback.info("Resolving Downloaded dependencies..");
    try (ProjectConnection connection =
        GradleConnector.newConnector().forProjectDirectory(gradleFile).connect()) {
      EclipseProject project = connection.getModel(EclipseProject.class);
      List<File> fileList = new ArrayList<>();
      project
          .getClasspath()
          .forEach(e -> {
            System.out.println("Resolved " + e.getFile().getAbsolutePath());
            fileList.add(e.getFile());
          });
      dependencyResolverCallback.info("Done!");
      callback.completed(fileList);
    }
  }

  public static class Builder {
    private File gradleFile;
    private File gradleDirectory;
    private Callback callback;
    private DependencyResolverCallback dependencyResolverCallback;

    public Builder setGradleFile(File gradleFile) {
      if (gradleFile.isDirectory()) {
        this.gradleDirectory = gradleFile;
      } else {
        this.gradleFile = gradleFile;
      }
      return this;
    }

    public Builder setCallback(Callback callback) {
      this.callback = callback;
      return this;
    }

    public Builder setDependencyResolverCallback(DependencyResolverCallback dependencyResolverCallback) {
      this.dependencyResolverCallback = dependencyResolverCallback;
      return this;
    }

    public void resolve() {
      if (gradleDirectory == null && gradleFile != null) {
        gradleDirectory = new File(Util.getGradleDirectory(), UUID.randomUUID().toString());
        gradleDirectory.mkdir();
        try {
          Files.copy(gradleFile.toPath(), new File(gradleDirectory, "build.gradle").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      new GradleDependencyResolver().parse(gradleDirectory, dependencyResolverCallback, callback);
    }
  }

  public interface Callback {
    void completed(List<File> fileList);
  }
}
