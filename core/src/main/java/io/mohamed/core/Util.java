package io.mohamed.core;

import io.mohamed.core.version.Version;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class Util {

  static File getLocalFilesDir() {
    String workingDirectory;
    String OS = System.getProperty("os.name").toLowerCase();
    if (OS.contains("win")) {
      // it is simply the location of the "AppData" folder
      workingDirectory = System.getenv("AppData");
    } else if (OS.contains("mac")) {
      workingDirectory = "/Library/Application Support";
    } else {
      // assume linux
      workingDirectory = System.getProperty("user.home");
    }
    File localFilesDir = new File(workingDirectory, "dependencies-resolver");
    if (!localFilesDir.exists()) {
      if (!localFilesDir.mkdir()) {
        System.err.println("Failed to create app data directory!");
      }
    }
    return localFilesDir;
  }

  public static boolean isAar(File file) {
    return FilenameUtils.getExtension(file.getName()).equals("aar");
  }

  public static void copyResource(String resource, File outputFile) throws IOException {
    URL aaptToolResource = DependencyDownloader.class.getResource(resource);
    if (aaptToolResource != null) {
      try (FileOutputStream fos = new FileOutputStream(outputFile)) {
        IOUtils.copy(aaptToolResource, fos);
      }
    }
  }

  public static String getVersion() {
    // the Version class is generated in compile time in build.gradle
    return "Dependency Resolver - Version " + Version.VERSION;
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
  public static void extractFile(Path zipFile, String fileName, Path outputFile)
      throws ProviderNotFoundException, IOException {
    // Wrap the file system in a try-with-resources statement
    // to auto-close it when finished and prevent a memory leak
    try (FileSystem fileSystem = FileSystems.newFileSystem(zipFile, null)) {
      Path fileToExtract = fileSystem.getPath(fileName);
      Files.copy(fileToExtract, outputFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public static void clearCache() throws IOException {
    FileUtils.deleteDirectory(getCachesDirectory());
  }

  public static File getMergedLibrariesDirectory() {
    File file = new File(getCachesDirectory(), "merged");
    if (!file.exists()) {
      if (!file.mkdir()) {
        System.err.println("Failed to create merged directory..");
      }
    }
    return file;
  }

  public static File getCachesDirectory() {
    File file = new File(getLocalFilesDir(), "caches");
    if (!file.exists()) {
      if (!file.mkdir()) {
        System.err.println("Failed to create caches directory..");
      }
    }
    return file;
  }
}
