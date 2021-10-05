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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.tooling.model.eclipse.EclipseProject;

public class GradleDependencyResolver {
  private GradleDependencyResolver() {}

  public void parse(File gradleDirectory, boolean verbose) {
    try (ProjectConnection connection =
        GradleConnector.newConnector().forProjectDirectory(gradleDirectory).connect()) {
      connection
          .newBuild()
          .setStandardOutput(System.out)
          .setStandardInput(System.in)
          .forTasks("build")
          .addProgressListener(
              (ProgressListener)
                  progressEvent -> {
                    if (verbose) {
                      System.out.println(progressEvent.getDescription());
                    }
                  })
          .run(
              new ResultHandler<>() {
                @Override
                public void onComplete(Void unused) {
                  System.out.println("Completed..");
                  resolveDependencies(gradleDirectory);
                }

                @Override
                public void onFailure(GradleConnectionException e) {
                  System.err.println(
                      "Failed to resolve dependencies for gradle project..");
                  e.printStackTrace();
                }
              });
    }
  }

  private void resolveDependencies(File gradleFile) {
    try (ProjectConnection connection =
        GradleConnector.newConnector().forProjectDirectory(gradleFile).connect()) {
      EclipseProject project = connection.getModel(EclipseProject.class);
      System.out.println(project.getName());
      project
          .getClasspath()
          .forEach(e -> System.out.println("Resolved " + e.getFile().getAbsolutePath()));
    }
  }

  public static class Builder {
    private File gradleFile;
    private File gradleDirectory;
    private boolean verbose;

    public Builder setGradleDirectory(File gradleDirectory) {
      this.gradleDirectory = gradleDirectory;
      return this;
    }

    public Builder setGradleFile(File gradleFile) {
      this.gradleFile = gradleFile;
      return this;
    }

    public Builder setVerbose(boolean verbose) {
      this.verbose = verbose;
      return this;
    }

    public void resolve() {
      if (gradleDirectory == null && gradleFile != null) {
        gradleDirectory = new File(Util.getGradleDirectory(), UUID.randomUUID().toString());
        gradleDirectory.mkdirs();
        try {
          Files.copy(gradleFile.toPath(), gradleDirectory.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      new GradleDependencyResolver().parse(gradleDirectory, verbose);
    }
  }
}
