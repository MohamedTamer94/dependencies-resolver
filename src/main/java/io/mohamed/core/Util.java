package io.mohamed.core;

import java.io.File;
import org.apache.commons.io.FilenameUtils;

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
}
