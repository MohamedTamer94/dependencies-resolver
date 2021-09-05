package io.mohamed.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
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
    try {
      ClassLoader classloader = Thread.currentThread().getContextClassLoader();
      InputStream inputStream = classloader.getResourceAsStream("/version.properties");
      if (inputStream == null) {
        return "This Build doesn't have a version name provided.";
      }
      Properties properties = new Properties();
      properties.load(new StringReader(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
      String version = properties.getProperty("version");
      return "Dependencies Resolver - v" + version;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "This Build doesn't have a version name provided.";
  }
}
