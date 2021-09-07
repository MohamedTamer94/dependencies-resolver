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

package io.mohamed.resolver.cli;

import io.mohamed.resolver.core.DependencyDownloader.Builder;
import io.mohamed.resolver.core.DependencyResolver;
import io.mohamed.resolver.core.Util;
import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.callback.FilesDownloadedCallback;
import io.mohamed.resolver.core.callback.ResolveCallback;
import io.mohamed.resolver.core.model.Dependency;
import io.mohamed.resolver.core.model.Repository;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONArray;

/**
 * A CLI to resolve dependencies for the given maven artifact
 *
 * @author Mohamed Tamer
 */
public class Main {
  private static final ArrayList<Command> SUPPORTED_COMMANDS = new ArrayList<>();
  private static final Options GENERAL_OPTIONS = new Options();
  private static final Preferences preferences = Preferences.userRoot().node(Main.class.getName());

  static {
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
        Option.builder("o").longOpt("output").desc("The output directory.").hasArg().build();
    Option filterAppInventorDependencies =
        Option.builder()
            .longOpt("filter-appinventor-dependencies")
            .desc("Don't include dependencies which app inventor includes by default.")
            .build();
    Option merge =
        Option.builder("m")
            .longOpt("merge")
            .desc("Merges all jar classes into one JAR file ( or AAR, if any AAR file exists")
            .build();
    Option gradleDependency =
        Option.builder("d")
            .longOpt("dependency")
            .hasArg()
            .desc("The dependency in gradle style : implementation 'com.test:test:1.0'")
            .build();
    Option repository =
        Option.builder("r")
            .desc(
                "Specifies custom repositories for the resolve task. Multiple repositories can be added by separating them with a space.")
            .hasArg()
            .numberOfArgs(Option.UNLIMITED_VALUES)
            .longOpt("repository")
            .build();
    Option help = Option.builder("h").longOpt("help").desc("Prints the help message").build();
    Option jarOnly =
        Option.builder("j")
            .desc(
                "If used, only jar files would be resolved, only classes.jar would be extracted from aars. Useful for extension developers.")
            .longOpt("jarOnly")
            .build();
    Options options = new Options();
    options.addOption(groupId);
    options.addOption(artifactId);
    options.addOption(aircraftVersion);
    options.addOption(aircraftType);
    options.addOption(verbose);
    options.addOption(output);
    options.addOption(merge);
    options.addOption(filterAppInventorDependencies);
    options.addOption(gradleDependency);
    options.addOption(help);
    options.addOption(repository);
    options.addOption(jarOnly);
    SUPPORTED_COMMANDS.add(new Command("resolve", options));
    Option versionOption =
        Option.builder("v")
            .longOpt("version")
            .desc("Prints the dependencies-resolver version")
            .build();
    GENERAL_OPTIONS.addOption(versionOption);
    GENERAL_OPTIONS.addOption(help);
    SUPPORTED_COMMANDS.add(new Command("clean", new Options()));
    Option repositoryOption =
        Option.builder("r")
            .longOpt("repository")
            .hasArg()
            .desc("The maven repository url")
            .required()
            .build();
    Options addRemoveRepositoryOptions = new Options();
    addRemoveRepositoryOptions.addOption(repositoryOption);
    SUPPORTED_COMMANDS.add(new Command("add-repository", addRemoveRepositoryOptions));
    SUPPORTED_COMMANDS.add(new Command("remove-repository", addRemoveRepositoryOptions));
  }

  public static void main(String[] args) throws ParseException {
    Command currentCommand = null;
    for (String arg : args) {
      for (Command command : SUPPORTED_COMMANDS) {
        if (command.getName().equals(arg)) {
          currentCommand = command;
        }
      }
    }
    CommandLineParser parser = new DefaultParser();
    final CommandLine commandLine;
    if (currentCommand != null) {
      try {
        commandLine = parser.parse(currentCommand.getOptions(), args);
      } catch (MissingOptionException e) {
        System.out.println(e.getMessage());
        new HelpFormatter()
            .printHelp(
                "java -jar dependencies-resolve-version-all.jar " + currentCommand.getName(),
                currentCommand.getOptions());
        return;
      }
    } else {
      commandLine = parser.parse(GENERAL_OPTIONS, args);
    }
    if (commandLine.hasOption("help")) {
      System.out.println(
          "A java CLI to resolve all the dependencies declared for the a specific maven artifact.");
      new HelpFormatter()
          .printHelp(
              "java -jar dependencies-resolve-version-all.jar "
                  + (currentCommand != null ? currentCommand.getName() : ""),
              currentCommand == null ? GENERAL_OPTIONS : currentCommand.getOptions());
      return;
    }
    if (commandLine.hasOption("version")) {
      System.out.println(Util.getVersion());
      return;
    }
    if (currentCommand != null && currentCommand.getName().equals("clean")) {
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Are you sure you want to delete cache files [Y/N]: ");
        if (br.readLine().equalsIgnoreCase("y")) {
          System.out.println("Cleaning caches..");
          preferences.clear();
          Util.clearCache();
          System.out.println("Clear Cache Successful..");
        }
        return;
      } catch (IOException | BackingStoreException e) {
        System.err.println("Failed to clear cache..");
        e.printStackTrace();
        return;
      }
    }
    if (currentCommand != null && currentCommand.getName().equals("add-repository")) {
      String repositories = preferences.get("repos", "[]");
      String repositoryUrl = commandLine.getOptionValue("repository");
      JSONArray reposArray = new JSONArray(repositories);
      for (int i = 0; i < reposArray.length(); i++) {
        String value = reposArray.getString(i);
        if (value.equals(repositoryUrl)) {
          System.out.println("Ignoring to add already existing repository " + repositoryUrl);
          return;
        }
      }
      for (Repository repository : Repository.COMMON_MAVEN_REPOSITORIES) {
        if (repository.getUrl().equals(repositoryUrl)) {
          System.out.println("Ignoring to add already existing repository " + repositoryUrl);
          return;
        }
      }
      reposArray.put(repositoryUrl);
      preferences.put("repos", reposArray.toString());
      System.out.println("Successfully added repository!");
      return;
    }
    if (currentCommand != null && currentCommand.getName().equals("remove-repository")) {
      String repositories = preferences.get("repos", "[]");
      String repositoryUrl = commandLine.getOptionValue("repository");
      JSONArray reposArray = new JSONArray(repositories);
      int repoIndex = -1;
      for (int i = 0; i < reposArray.length(); i++) {
        String value = reposArray.getString(i);
        if (value.equals(repositoryUrl)) {
          repoIndex = i;
          break;
        }
      }
      if (repoIndex == -1) {
        System.out.println("Ignoring to remove non-existing repository " + repositoryUrl);
      } else {
        reposArray.remove(repoIndex);
        preferences.put("repos", reposArray.toString());
        System.out.println("Successfully removed repository..");
      }
      return;
    }
    // if the repository is null, make it an empty list
    List<String> repositories =
        Optional.ofNullable(commandLine.getOptionValues("repository"))
            .map(Arrays::stream)
            .orElseGet(Stream::empty)
            .collect(Collectors.toList());
    String repositoriesFromPrefs = preferences.get("repos", "[]");
    JSONArray reposArray = new JSONArray(repositoriesFromPrefs);
    repositories.addAll(
        reposArray.toList().stream()
            .map(object -> Objects.toString(object, ""))
            .collect(Collectors.toList()));
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
    if (!commandLine.hasOption("output")) {
      throw new IllegalArgumentException("The required option --output wasn't provided.");
    }
    // For the CLI, all logs are printed to the stdout
    DependencyResolverCallback dependencyResolverCallback =
        new DependencyResolverCallback() {
          @Override
          public void dependencyPomDownloading(String url) {
            System.out.println("Downloading " + url);
          }

          @Override
          public void dependencyPomDownloaded(String url) {
            System.out.println("Downloaded " + url);
          }

          @Override
          public void dependencyPomParsing(String url) {
            System.out.println("Parsing " + url);
          }

          @Override
          public void dependencyPomParsed(String url) {
            System.out.println("Parsed " + url);
          }

          @Override
          public void dependencyFileDownloading(String url) {
            System.out.println("Downloading " + url);
          }

          @Override
          public void dependencyFileDownloaded(String url) {
            System.out.println("Downloaded " + url);
          }

          @Override
          public void merging(MergeStage stage) {
            switch (stage) {
              case MERGE_MANIFEST:
                System.out.println("Merging Android Manifests..");
                break;
              case MERGE_MANIFEST_FAILED:
                System.err.println("Failed to Merge Android Manifests..");
                break;
              case MERGE_CLASS_FILES:
                System.out.println("Merging Class Files..");
                break;
              case MERGE_MANIFEST_SUCCESS:
                System.out.println("Successfully Merged Android Manifests..");
                break;
              case START:
                System.out.println("Merging Libraries..");
                break;
              case MERGE_RESOURCES:
                System.out.println("Merging Resources..");
                break;
              case MERGE_RESOURCES_SUCCESS:
                System.out.println("Successfully Merged Resources..");
              case MERGE_CLASS_FILES_SUCCESS:
                System.out.println("Successfully merged class files..");
            }
          }

          @Override
          public void mergeSuccess() {
            System.out.println("Successfully Merged Libraries..");
          }

          @Override
          public void mergeFailed() {
            System.out.println("Failed to merge libraries..");
          }

          @Override
          public void verbose(String message) {
            if (commandLine.hasOption("verbose")) {
              System.out.println(message);
            }
          }

          @Override
          public void error(String message) {
            System.err.println(message);
          }

          @Override
          public void info(String message) {
            System.out.println(message);
          }
        };
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
          new Builder()
              .setMainDependency(mainDependency)
              .setCallback(callback)
              .setDependencies(dependencyList)
              .setRepositories(repositories)
              .setJarOnly(commandLine.hasOption("jarOnly"))
              .setMerge(commandLine.hasOption("merge"))
              .setDependencyResolverCallback(dependencyResolverCallback)
              .setFilterAppInventorDependencies(
                  commandLine.hasOption("filter-appinventor-dependencies"))
              .setVerbose(commandLine.hasOption("verbose"))
              .resolve();
        };
    new DependencyResolver.Builder()
        .setDependency(mainDependency)
        .setDependencyResolverCallback(dependencyResolverCallback)
        .setCallback(resolveCallback)
        .setRepositories(repositories)
        .resolve();
  }
}
