package io.mohamed.core;

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
import com.android.utils.StdLogger;
import com.android.utils.StdLogger.Level;
import io.mohamed.core.model.Dependency;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
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
  // weather to print debug messages
  private boolean verbose;

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
   * @param verbose weather to print debug messages or not
   * @param downloadedFiles the aar files to merge
   * @param mainDependency the main dependency, used for writing the AAR name
   * @return the output AAR file
   * @throws IOException when writing aar file fails
   * @throws MergeFailureException when an error occurs when merging android manifests
   * @throws MergingException when resources merging fails
   */
  public File merge(boolean verbose, List<File> downloadedFiles, Dependency mainDependency)
      throws IOException, MergeFailureException, MergingException {
    this.verbose = verbose;
    System.out.println("Merging Libraries..");
    // collect and extract android manifest and classes.jar files
    List<File> androidManifests = new ArrayList<>();
    List<File> classesJars = new ArrayList<>();
    List<File> resDirectories = new ArrayList<>();
    List<File> jniDirectories = new ArrayList<>();
    List<File> assetsDirectory = new ArrayList<>();
    List<File> libsDirectory = new ArrayList<>();
    for (File library : downloadedFiles) {
      if (Util.isAar(library)) {
        try {
          File androidManifestOutputFile = new File(library.getParentFile(), "AndroidManifest.xml");
          extractFile(library.toPath(), "AndroidManifest.xml", androidManifestOutputFile.toPath());
          androidManifests.add(androidManifestOutputFile);
        } catch (IOException
            | ProviderNotFoundException ignored) { // the library has no android manifest
        }
        try {
          File classesJar = new File(library.getParentFile(), "classes.jar");
          extractFile(library.toPath(), "classes.jar", classesJar.toPath());
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
    File mainManifest = new File(Util.getLocalFilesDir(), "AndroidManifest.xml");
    PrintWriter writer = new PrintWriter(mainManifest, "UTF-8");
    writer.println(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "    package=\"com.test\"><uses-sdk android:minSdkVersion=\"31\"/></manifest>");
    writer.close();
    ILogger logger = new StdLogger(verbose ? Level.VERBOSE : Level.ERROR);
    Invoker<?> invoker = ManifestMerger2.newMerger(mainManifest, logger, MergeType.APPLICATION);
    invoker.addLibraryManifests(androidManifests.toArray(new File[0]));
    MergingReport report = invoker.merge();
    if (report.getResult().isSuccess()) {
      XmlDocument xmlDocument =
          report.getMergedXmlDocument(MergingReport.MergedManifestKind.MERGED);
      save(xmlDocument, mainManifest);
      System.out.println("Successfully merged android manifests..");
    } else {
      System.err.println("Error merging android manifests.");
      System.err.println(report.getReportString());
    }
    // merge classes.jar
    System.out.println("Merging Class Files..");
    File mainClassesJar = new File(Util.getLocalFilesDir(), "classes.jar");
    new JARMerger().merge(classesJars, mainClassesJar);
    System.out.println("Successfully Merged Class Files..");
    System.out.println("Merging Resources..");
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
      System.err.println("Cannot run AAPT on OS " + osName);
      return null;
    }
    File aaptToolFile = File.createTempFile("aapt", "");
    if (!aaptToolFile.setExecutable(true)) {
      System.out.println("[WARNING] Failed to set AAPT tool executable.");
    }
    Util.copyResource(aaptTool, aaptToolFile);
    File outputDir = new File(Util.getLocalFilesDir(), "res");
    if (!outputDir.exists()) {
      if (!outputDir.mkdir()) {
        System.out.println("[WARNING] Failed to create output resources directory");
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
    File aar =
        new File(
            Util.getLocalFilesDir(),
            mainDependency.getArtifactId() + "-" + mainDependency.getVersion() + ".aar");
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(aar))) {
      // add assets
      for (File assetDir : assetsDirectory) {
        if (verbose) {
          System.out.println("Adding directory " + assetDir);
        }
        addDirectory(assetDir, out);
      }
      for (File jniDir : jniDirectories) {
        if (verbose) {
          System.out.println("Adding directory " + jniDir);
        }
        addDirectory(jniDir, out);
      }
      for (File libDir : libsDirectory) {
        if (verbose) {
          System.out.println("Adding directory " + libDir);
        }
        addDirectory(libDir, out);
      }
      addFile(mainClassesJar, "classes.jar", out);
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
        new SimpleFileVisitor<Path>() {
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
            System.out.println("[WARNING] Failed to create entry destination");
          }
        } else {
          if (verbose) {
            System.out.println("Extracting to " + entryDestination.getAbsolutePath());
          }
          if (!entryDestination.getParentFile().exists()
              && !entryDestination.getParentFile().mkdirs()) {
            System.out.println("[WARNING] Failed to create entry destination");
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

  /**
   * Extracts a file from zip file
   *
   * @param zipFile the zip file path
   * @param fileName the file name
   * @param outputFile the output file path
   * @throws ProviderNotFoundException if extracting file fails or the file doesn't exist
   * @throws IOException if extracting file fails or the file doesn't exist
   */
  public void extractFile(Path zipFile, String fileName, Path outputFile)
      throws ProviderNotFoundException, IOException {
    // Wrap the file system in a try-with-resources statement
    // to auto-close it when finished and prevent a memory leak
    try (FileSystem fileSystem = FileSystems.newFileSystem(zipFile, null)) {
      Path fileToExtract = fileSystem.getPath(fileName);
      Files.copy(fileToExtract, outputFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
