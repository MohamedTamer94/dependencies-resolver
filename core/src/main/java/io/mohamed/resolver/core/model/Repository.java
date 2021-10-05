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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Repository {

  // the google's maven repository
  public static final Repository GOOGLE_REPOSITORY = new Repository("https://maven.google.com/");
  // the google's central repository
  public static final Repository CENTRAL_REPOSITORY =
      new Repository("https://repo.maven.apache.org/maven2/");
  // the bintray maven repository
  public static final Repository BINTRAY_REPOSITORY =
      new Repository("https://jcenter.bintray.com/");
  // the clojars maven repository
  public static final Repository CLOJARS_REPOSITORY = new Repository("https://repo.clojars.org/");
  // the atlassian maven repository
  public static final Repository ATLASSIAN_REPOSITORY = new Repository("https://packages.atlassian.com/mvn/maven-atlassian-external/");
  // a collection of common public maven repositories
  // TODO: allow users to add custom maven repositories to it
  public static final List<Repository> COMMON_MAVEN_REPOSITORIES =
      new ArrayList<>(
          Arrays.asList(
              Repository.GOOGLE_REPOSITORY,
              Repository.CENTRAL_REPOSITORY,
              Repository.BINTRAY_REPOSITORY,
              Repository.BINTRAY_REPOSITORY,
              Repository.CLOJARS_REPOSITORY,
              ATLASSIAN_REPOSITORY));
  String url;

  public Repository(String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return getUrl();
  }

  public String getUrl() {
    return url;
  }
}
