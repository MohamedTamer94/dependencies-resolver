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

import com.android.ide.common.res2.MergingException;
import com.android.manifmerger.ManifestMerger2.MergeFailureException;
import io.mohamed.resolver.core.util.Util;
import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.model.Dependency;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibraryMerger {

  /**
   * Merges all the downloaded libraries into one file If all the downloaded files were a jar:
   * Invokes the JARMerger to merge all jar files into one If any of the downloaded files were an
   * aar: Invokes the AARMerger to merge all the aar and jar library files into one AAR
   *
   * @return true if merging libraries was successful
   */
  public static MergeResult mergeLibraries(
      Dependency mainDependency,
      List<File> downloadedFiles,
      DependencyResolverCallback dependencyResolverCallback) {
    try {
      if (hasAnyAar(downloadedFiles)) {
        File result =
            new AARMerger().merge(downloadedFiles, mainDependency, dependencyResolverCallback);
        if (result != null) {
          downloadedFiles = new ArrayList<>(Collections.singletonList(result));
          return new MergeResult(downloadedFiles, true);
        }
      } else {
        File result =
            new File(
                Util.getMergedLibrariesDirectory(),
                (mainDependency != null
                        ? mainDependency.getArtifactId() + "-" + mainDependency.getVersion()
                        : "merged")
                    + ".jar");
        new JARMerger().merge(downloadedFiles, result);
        downloadedFiles = new ArrayList<>(Collections.singletonList(result));
        return new MergeResult(downloadedFiles, true);
      }
      return new MergeResult(downloadedFiles, true);
    } catch (IOException | MergingException | MergeFailureException e) {
      e.printStackTrace();
      return new MergeResult(downloadedFiles, false);
    }
  }

  /**
   * Checks if any of the downloaded files is an AAR file
   *
   * @return true if any AAR file was downloaded
   */
  private static boolean hasAnyAar(List<File> downloadedFiles) {
    return downloadedFiles.stream().anyMatch(Util::isAar);
  }

  public static class MergeResult {
    private final List<File> mergedLibraries;
    private final boolean success;

    public MergeResult(List<File> mergedLibraries, boolean success) {
      this.mergedLibraries = mergedLibraries;
      this.success = success;
    }

    public boolean isSuccess() {
      return success;
    }

    public List<File> getMergedLibraries() {
      return mergedLibraries;
    }
  }
}
