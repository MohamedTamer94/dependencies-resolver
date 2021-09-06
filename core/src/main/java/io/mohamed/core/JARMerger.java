package io.mohamed.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Merges multiple .jar files into one jar file
 *
 * @author Mohamed Tamer
 */
public class JARMerger {
  // the merged zip entries
  List<ZipEntry> mergedEntries = new ArrayList<>();

  /**
   * Weather the same entry was included before or not
   *
   * @param entry the entry
   * @return true if the entry was already include it
   */
  private boolean hasZipEntry(ZipEntry entry) {
    return mergedEntries.stream().anyMatch((e) -> e.getName().equals(entry.getName()));
  }

  /**
   * Merges multiple jar files into one jar file
   *
   * @param jarFiles list of jar files
   * @param outputFile the output jar file
   * @throws IOException if an error occurs while creating the Jar file
   */
  public void merge(List<File> jarFiles, File outputFile) throws IOException {
    ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(outputFile));
    for (File classesJar : jarFiles) {
      ZipInputStream inStream = new ZipInputStream(new FileInputStream(classesJar));
      byte[] buffer = new byte[1024];
      int len;

      for (ZipEntry e; (e = inStream.getNextEntry()) != null; ) {
        if (hasZipEntry(e)) {
          continue;
        }
        outStream.putNextEntry(e);
        mergedEntries.add(e);
        while ((len = inStream.read(buffer)) > 0) {
          outStream.write(buffer, 0, len);
        }
      }
      inStream.close();
    }
  }
}
