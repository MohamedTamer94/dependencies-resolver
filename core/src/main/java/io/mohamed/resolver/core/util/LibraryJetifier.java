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

package io.mohamed.resolver.core.util;

import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.util.Util;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class LibraryJetifier {
  public List<File> jetify(List<File> libraries, DependencyResolverCallback callback)
      throws IOException, InterruptedException {
    List<File> jetifiedLibraries = new ArrayList<>();
    File jetifierZipFile = new File(Util.getLocalFilesDir(), "jetifier-standalone.zip");
    if (!jetifierZipFile.exists()) {
      callback.info("Extracting Jetifier..");
      Util.copyResource("/jetifier-standalone.zip", jetifierZipFile);
      ZipFile zipFile = new ZipFile(jetifierZipFile);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      ZipEntry entry;
      while (entries.hasMoreElements()) {
        entry = entries.nextElement();
        callback.verbose("Extracting " + entry.getName());
        File out = new File(Util.getLocalFilesDir(), entry.getName());
        if (entry.isDirectory()) {
          if (!out.exists() && out.mkdirs()) {
            System.out.println("[WARNING] Failed to create some directories for the extracted jetifier.");
          }
        } else {
          try (FileOutputStream fileOutputStream = new FileOutputStream(out)) {
            IOUtils.copy(zipFile.getInputStream(entry), fileOutputStream);
          }
        }
      }
      callback.info("Done!");
    }
    callback.info("Jetifing Libraries..");
    File jetifierFile = new File(Util.getLocalFilesDir(), "jetifier-standalone");
    String os = System.getProperty("os.name").toLowerCase();
    File jetifierScript =
        new File(
            new File(jetifierFile, "bin"),
            "jetifier-standalone" + (os.contains("win") ? ".bat" : ""));
    File outputLibrary;
    for (File library : libraries) {
      outputLibrary =
          new File(
              library.getParent(),
              FilenameUtils.removeExtension(library.getName())
                  + "-jetified."
                  + FilenameUtils.getExtension(library.getName()));
      callback.info("Jetifying " + library.getAbsolutePath());
      Process process =
          new ProcessBuilder()
              .command(
                  jetifierScript.getAbsolutePath(),
                  "-i",
                  library.getAbsolutePath(),
                  "-o",
                  outputLibrary.getAbsolutePath())
              .start();
      process.waitFor();
      if (!outputLibrary.exists()) {
        callback.info(
            "[WARNING] Failed to jetify library. Ignoring.."); // we ignore, and not abort here, since
                                                               // this could normally happen with
                                                               // the support library it self, which
                                                               // won't be required after jetifying
      } else {
        jetifiedLibraries.add(outputLibrary);
        callback.info("Jetify Successful..");
      }
    }
    return jetifiedLibraries;
  }
}
