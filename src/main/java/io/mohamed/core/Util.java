package io.mohamed.core;

import io.mohamed.core.version.Version;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
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
}
