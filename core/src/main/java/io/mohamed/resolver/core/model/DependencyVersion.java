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

package io.mohamed.resolver.core.model;

import java.util.List;

/**
 * A class that represents the dependency version information
 *
 * @author Mohamed Tamer
 */
public class DependencyVersion {

  // the repository latest version name
  private final String latestVersion;
  // the repository version names
  private final List<String> versionNames;

  /**
   * Creates a new DependencyVersion class
   *
   * @param versionNames a list of the dependency versions
   * @param latestVersion the dependency latest version
   */
  public DependencyVersion(List<String> versionNames, String latestVersion) {
    this.versionNames = versionNames;
    this.latestVersion = latestVersion;
  }

  /** @return the dependency version names */
  public List<String> getVersionNames() {
    return versionNames;
  }

  /** @return the dependency latest version name */
  public String getLatestVersion() {
    return latestVersion;
  }
}
