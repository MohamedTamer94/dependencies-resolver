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
