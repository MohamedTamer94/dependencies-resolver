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

package io.mohamed.cli;

import io.mohamed.core.DependencyDownloader;
import io.mohamed.core.DependencyResolver;
import io.mohamed.core.callback.FilesDownloadedCallback;
import io.mohamed.core.callback.ResolveCallback;
import io.mohamed.core.model.Dependency;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A CLI to resolve dependencies for the given maven artifact
 *
 * @author Mohamed Tamer
 */
public class Main {

  public static void main(String[] args) throws ParseException {
    Option groupId =
        Option.builder().longOpt("groupId").desc("The aircraft group ID.").hasArg().build();
    Option artifactId =
        Option.builder().longOpt("artifactId").desc("The artifactId ID.").hasArg().build();
    Option aircraftVersion =
        Option.builder().longOpt("version").desc("The aircraft version.").hasArg().build();
    Option aircraftType =
        Option.builder().longOpt("type").desc("The dependency version.").hasArg().build();
    Option verbose = Option.builder("v").longOpt("verbose").desc("Show debug messages.").build();
    Option output =
        Option.builder("o")
            .longOpt("output")
            .desc("The output directory.")
            .required()
            .hasArg()
            .build();
    Option filterAppInventorDependencies =
        Option.builder()
            .longOpt("filter-appinventor-dependencies")
            .desc("Don't include dependencies which app inventor includes by default.")
            .build();
    Option compress =
        Option.builder("c")
            .longOpt("compress")
            .desc("Compresses all jar classes into one JAR file ( or AAR, if any AAR file exists")
            .build();
    Option gradleDependency =
        Option.builder("d")
            .longOpt("dependency")
            .hasArg()
            .desc("The dependency in gradle style : implementation 'com.test:test:1.0'")
            .build();
    Options options = new Options();
    options.addOption(groupId);
    options.addOption(artifactId);
    options.addOption(aircraftVersion);
    options.addOption(aircraftType);
    options.addOption(verbose);
    options.addOption(output);
    options.addOption(compress);
    options.addOption(filterAppInventorDependencies);
    options.addOption(gradleDependency);
    CommandLineParser parser = new DefaultParser();
    final CommandLine commandLine;
    try {
      commandLine = parser.parse(options, args);
    } catch (MissingOptionException e) {
      System.out.println(e.getMessage());
      new HelpFormatter().printHelp("java -jar dependencies-resolve-version-all.jar", options);
      return;
    }
    System.out.println("Fetching Dependencies..");
    // resolves and locates the dependencies by parsing their POM files
    Dependency mainDependency;
    if (commandLine.hasOption("dependency")) {
      mainDependency = Dependency.valueOf(commandLine.getOptionValue("dependency"));
    } else if (commandLine.hasOption("groupId")
        && commandLine.hasOption("artifactId")
        && commandLine.hasOption("version")) {
      mainDependency =
          new Dependency(
              commandLine.getOptionValue("groupId"),
              commandLine.getOptionValue("artifactId"),
              commandLine.getOptionValue("version"),
              commandLine.getOptionValue("type"));
    } else {
      throw new IllegalArgumentException(
          "Neither a dependency argument nor a groupId, artifactId, version arguments were provided.");
    }
    ResolveCallback resolveCallback =
        (artifactFound, pomUrl, mavenRepo, dependencyList, dependency) -> {
          if (dependencyList.isEmpty()) {
            System.err.println("Didn't find any dependencies!");
          } else {
            System.out.println("Successfully Resolved " + dependencyList.size() + " dependencies!");
          }
          System.out.println("Downloading Dependencies..");
          // downloads the JAR/AAR files for the resolved dependencies
          FilesDownloadedCallback callback =
              fileList -> {
                System.out.println("Successfully downloaded " + fileList.size() + " files.");
                File dependenciesDir = new File(commandLine.getOptionValue("output"));
                if (!dependenciesDir.exists()) {
                  if (!dependenciesDir.mkdir()) {
                    System.err.println("Failed to create dependencies directory.");
                    return;
                  }
                }
                // copy all downloaded files to the output directory
                System.out.println("Copying libraries to output directory..");
                for (File file : fileList) {
                  if (file == null) {
                    continue;
                  }
                  if (commandLine.hasOption("verbose")) {
                    System.out.println(
                        "Copying library: "
                            + file.getAbsolutePath()
                            + " to output directory "
                            + dependenciesDir.getAbsolutePath());
                  }
                  File destFile = new File(dependenciesDir.getAbsolutePath(), file.getName());
                  try {
                    Files.copy(
                        file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
                System.out.println("Success!");
                System.exit(0);
              };
          new DependencyDownloader.Builder()
              .setMainDependency(mainDependency)
              .setCallback(callback)
              .setDependencies(dependencyList)
              .setCompress(commandLine.hasOption("compress"))
              .setFilterAppInventorDependencies(
                  commandLine.hasOption("filter-appinventor-dependencies"))
              .setVerbose(commandLine.hasOption("verbose"))
              .resolve();
        };
    new DependencyResolver.Builder()
        .setDependency(mainDependency)
        .setCallback(resolveCallback)
        .resolve();
  }
}
