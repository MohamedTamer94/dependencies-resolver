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

public class JARMerger {
  List<ZipEntry> mergedEntries = new ArrayList<>();

  private boolean hasZipEntry(ZipEntry entry) {
    return mergedEntries.stream().anyMatch((e) -> e.getName().equals(entry.getName()));
  }

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
