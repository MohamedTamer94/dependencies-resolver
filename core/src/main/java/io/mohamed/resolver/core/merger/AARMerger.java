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

package io.mohamed.resolver.core.merger;

import com.android.ide.common.internal.AaptCruncher;
import com.android.ide.common.internal.PngCruncher;
import com.android.ide.common.process.DefaultProcessExecutor;
import com.android.ide.common.process.LoggedProcessOutputHandler;
import com.android.ide.common.res2.MergedResourceWriter;
import com.android.ide.common.res2.MergingException;
import com.android.ide.common.res2.ResourceMerger;
import com.android.ide.common.res2.ResourceSet;
import com.android.manifmerger.ManifestMerger2;
import com.android.manifmerger.ManifestMerger2.Invoker;
import com.android.manifmerger.ManifestMerger2.MergeFailureException;
import com.android.manifmerger.ManifestMerger2.MergeType;
import com.android.manifmerger.MergingReport;
import com.android.manifmerger.XmlDocument;
import com.android.utils.ILogger;
import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.callback.DependencyResolverCallback.MergeStage;
import io.mohamed.resolver.core.model.Dependency;
import io.mohamed.resolver.core.util.Util;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Merges multiple AAR files into one AAR file
 *
 * @author Mohamed Tamer
 */
public class AARMerger {

  // the dependency downloader callback
  private DependencyResolverCallback callback;

  /**
   * Saves the manifest XmlDocument to a file
   *
   * @param xmlDocument the manifest xml document
   * @param out the output manifest file
   */
  private static void save(XmlDocument xmlDocument, File out) {
    try {
      Files.write(
          out.toPath(),
          Arrays.asList(xmlDocument.prettyPrint().split("\n")),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Merges the given aar files into one
   *
   * @param downloadedFiles the aar files to merge
   * @param mainDependency the main dependency, used for writing the AAR name
   * @return the output AAR file
   * @throws IOException when writing aar file fails
   * @throws MergeFailureException when an error occurs when merging android manifests
   * @throws MergingException when resources merging fails
   */
  public File merge(
      List<File> downloadedFiles, Dependency mainDependency, DependencyResolverCallback callback)
      throws IOException, MergeFailureException, MergingException {
    // weather to print debug messages
    this.callback = callback;
    // collect and extract android manifest and classes.jar files
    List<File> androidManifests = new ArrayList<>();
    List<File> classesJars = new ArrayList<>();
    List<File> resDirectories = new ArrayList<>();
    List<File> jniDirectories = new ArrayList<>();
    List<File> assetsDirectory = new ArrayList<>();
    List<File> libsDirectory = new ArrayList<>();
    List<File> proguardFiles = new ArrayList<>();
    for (File library : downloadedFiles) {
      if (Util.isAar(library)) {
        try {
          File androidManifestOutputFile = new File(library.getParentFile(), "AndroidManifest.xml");
          Util.extractFile(
              library.toPath(), "AndroidManifest.xml", androidManifestOutputFile.toPath());
          androidManifests.add(androidManifestOutputFile);
        } catch (IOException
            | ProviderNotFoundException ignored) { // the library has no android manifest
        }
        try {
          File proguardOutputFile = new File(library.getParentFile(), "proguard.txt");
          Util.extractFile(library.toPath(), "proguard.txt", proguardOutputFile.toPath());
          proguardFiles.add(proguardOutputFile);
        } catch (IOException
            | ProviderNotFoundException ignored) { // the library has no android manifest
        }
        try {
          File classesJar = new File(library.getParentFile(), "classes.jar");
          Util.extractFile(library.toPath(), "classes.jar", classesJar.toPath());
          classesJars.add(classesJar);
        } catch (IOException | ProviderNotFoundException ignored) {
        }
        try {
          if (extractFolder(library, library.getParentFile(), "jni")) {
            jniDirectories.add(new File(library.getParentFile(), "jni"));
          }
        } catch (IOException ignored) {
        }
        try {
          if (extractFolder(library, library.getParentFile(), "assets")) {
            assetsDirectory.add(new File(library.getParentFile(), "assets"));
          }
        } catch (IOException ignored) {
        }
        try {
          if (extractFolder(library, library.getParentFile(), "libs")) {
            libsDirectory.add(new File(library.getParentFile(), "libs"));
          }
        } catch (IOException ignored) {
        }
      } else {
        classesJars.add(library);
      }
    }
    // merge android manifest files into one file
    callback.merging(MergeStage.MERGE_MANIFEST);
    File mainManifest = new File(Util.getMergedLibrariesDirectory(), "AndroidManifest.xml");
    PrintWriter writer = new PrintWriter(mainManifest, StandardCharsets.UTF_8);
    writer.println(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "    package=\"com.test\"><uses-sdk android:minSdkVersion=\"31\"/></manifest>");
    writer.close();
    ILogger logger = new Logger(callback);
    Invoker<?> invoker = ManifestMerger2.newMerger(mainManifest, logger, MergeType.APPLICATION);
    invoker.addLibraryManifests(androidManifests.toArray(new File[0]));
    MergingReport report = invoker.merge();
    if (report.getResult().isSuccess()) {
      XmlDocument xmlDocument =
          report.getMergedXmlDocument(MergingReport.MergedManifestKind.MERGED);
      save(xmlDocument, mainManifest);
      callback.merging(MergeStage.MERGE_MANIFEST_SUCCESS);
    } else {
      callback.merging(MergeStage.MERGE_MANIFEST_FAILED);
      callback.error(report.getReportString());
    }
    // merge classes.jar
    callback.merging(MergeStage.MERGE_CLASS_FILES);
    File mainClassesJar = new File(Util.getMergedLibrariesDirectory(), "classes.jar");
    new JARMerger().merge(classesJars, mainClassesJar);
    callback.merging(MergeStage.MERGE_CLASS_FILES_SUCCESS);
    callback.merging(MergeStage.MERGE_RESOURCES);
    // merge resources using Android AAPT tool
    String aaptTool;
    String osName = System.getProperty("os.name");
    if (osName.equals("Mac OS X")) {
      aaptTool = "/mac/aapt";
    } else if (osName.equals("Linux")) {
      aaptTool = "/linux/aapt";
    } else if (osName.startsWith("Windows")) {
      aaptTool = "/windows/aapt";
    } else {
      callback.error("Cannot run AAPT on OS " + osName);
      return null;
    }
    File aaptToolFile = File.createTempFile("aapt", "");
    if (!aaptToolFile.setExecutable(true)) {
      callback.info("[WARNING] Failed to set AAPT tool executable.");
    }
    Util.copyResource(aaptTool, aaptToolFile);
    File outputDir = new File(Util.getMergedLibrariesDirectory(), "res");
    if (!outputDir.exists()) {
      if (!outputDir.mkdir()) {
        callback.info("[WARNING] Failed to create output resources directory");
      }
    }
    PngCruncher cruncher =
        new AaptCruncher(
            aaptToolFile.getAbsolutePath(),
            new DefaultProcessExecutor(logger),
            new LoggedProcessOutputHandler(logger));
    ResourceSet mainResSet = new ResourceSet("main");
    mainResSet.addSources(resDirectories);
    ResourceMerger merger = new ResourceMerger();
    mainResSet.loadFromFiles(logger);
    merger.addDataSet(mainResSet);
    MergedResourceWriter mergedResourceWriter =
        new MergedResourceWriter(outputDir, cruncher, false, false, null);
    mergedResourceWriter.setInsertSourceMarkers(true);
    merger.mergeData(mergedResourceWriter, false);
    callback.merging(MergeStage.MERGE_RESOURCES_SUCCESS);
    // merge proguard files
    File mainProguardFile = new File(Util.getMergedLibrariesDirectory(), "proguard.txt");
    HashSet<String> outputProguardLines = new HashSet<>(); // we use hashset to disallow duplicates
    for (File proguardFile : proguardFiles) {
      List<String> proguardLines = Files.readAllLines(proguardFile.toPath());
      outputProguardLines.addAll(proguardLines);
    }
    try (PrintWriter proguardWriter = new PrintWriter(mainProguardFile, StandardCharsets.UTF_8)) {
      for (String line : outputProguardLines) {
        if (!line.trim().startsWith("#")) { // don't include comments
          proguardWriter.write(line + "\n");
        }
      }
    }
    // generate the final aar
    File aar =
        new File(
            Util.getMergedLibrariesDirectory(),
            (mainDependency != null
                    ? mainDependency.getArtifactId() + "-" + mainDependency.getVersion()
                    : "merged")
                + ".aar");
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(aar))) {
      // add assets
      for (File assetDir : assetsDirectory) {
        callback.verbose("Adding directory " + assetDir);
        addDirectory(assetDir, out);
      }
      // add jni directories
      for (File jniDir : jniDirectories) {
        callback.verbose("Adding directory " + jniDir);
        addDirectory(jniDir, out);
      }
      // add libraries
      for (File libDir : libsDirectory) {
        callback.verbose("Adding directory " + libDir);
        addDirectory(libDir, out);
      }
      // add proguard file
      addFile(mainProguardFile, "proguard.txt", out);
      // add jar file
      addFile(mainClassesJar, "classes.jar", out);
      // add android manifest
      addFile(mainManifest, "AndroidManifest.xml", out);
      addDirectory(outputDir, out);
    }
    return aar;
  }

  /**
   * Adds a file to zip file, given its path in the zip
   *
   * @param file the file to add
   * @param filePath the path for the file to take in the zip
   * @param out the zip stream
   * @throws IOException when writing file to aar fails
   */
  private void addFile(File file, String filePath, ZipOutputStream out) throws IOException {
    ZipEntry e = new ZipEntry(filePath);
    out.putNextEntry(e);

    byte[] data = FileUtils.readFileToByteArray(file);
    out.write(data, 0, data.length);
    out.closeEntry();
  }

  /**
   * Adds a directory and all of its sub-files, and subdirectories to zip file
   *
   * @param directory the directory file
   * @param out the zip stream
   * @throws IOException when writing file to aar fails
   */
  private void addDirectory(File directory, ZipOutputStream out) throws IOException {
    Files.walkFileTree(
        directory.toPath(),
        new SimpleFileVisitor<>() {
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            out.putNextEntry(
                new ZipEntry(directory.getParentFile().toPath().relativize(file).toString()));
            Files.copy(file, out);
            out.closeEntry();
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /**
   * Extract a folder from a zip file
   *
   * @param file the zip file
   * @param outputDir the output directory
   * @param folder the folder name
   * @return weather extracting folder succeeds
   * @throws IOException when extracting folder fails
   */
  private boolean extractFolder(File file, File outputDir, String folder) throws IOException {
    List<File> filesCopied = new ArrayList<>();
    try (java.util.zip.ZipFile zipFile = new ZipFile(file)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (!entry.getName().contains(folder + "/")) {
          continue;
        }
        File entryDestination = new File(outputDir, entry.getName());
        if (entry.isDirectory()) {
          if (!entryDestination.exists() && !entryDestination.mkdirs()) {
            callback.info("[WARNING] Failed to create entry destination");
          }
        } else {
          callback.verbose("Extracting to " + entryDestination.getAbsolutePath());
          if (!entryDestination.getParentFile().exists()
              && !entryDestination.getParentFile().mkdirs()) {
            callback.info("[WARNING] Failed to create entry destination");
          }
          try (InputStream in = zipFile.getInputStream(entry);
              OutputStream out = new FileOutputStream(entryDestination)) {
            IOUtils.copy(in, out);
          }
          filesCopied.add(entryDestination);
        }
      }
    }
    return !filesCopied.isEmpty();
  }

  static class Logger implements ILogger {
    private final DependencyResolverCallback callback;

    public Logger(DependencyResolverCallback callback) {
      super();
      this.callback = callback;
    }

    @Override
    public void error(Throwable throwable, String s, Object... objects) {
      callback.error(s);
    }

    @Override
    public void warning(String s, Object... objects) {
      callback.info("[WARNING] " + s);
    }

    @Override
    public void info(String s, Object... objects) {
      callback.info(s);
    }

    @Override
    public void verbose(String s, Object... objects) {
      callback.verbose(s);
    }
  }
}
