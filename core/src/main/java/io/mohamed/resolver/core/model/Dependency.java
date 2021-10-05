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

// -*- mode: java; c-basic-offset: 2; -*-
package io.mohamed.resolver.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to represent a maven dependency a maven dependency is represented in POM XML files as:
 * <dependency> <groupId>com.example</groupId> <artifactId>test-project</artifactId>
 * <version>1.0</version> <scope>runtime</scope> <type>aar</type> </dependency>
 *
 * @author Mohamed Tamer
 */
public class Dependency {
  // gradle groovy implementation syntax
  private static final Pattern GRADLE_GROOVY_IMPLEMENTATION =
      Pattern.compile("(implementation) (['\"])(.*)(['\"])");
  // gradle kotlin implementation syntax
  private static final Pattern GRADLE_KOTLIN_IMPLEMENTATION =
      Pattern.compile("(implementation\\()(['\"])(.*)(['\"]\\))");
  private static final Pattern GRADLE_LONG_IMPLEMENTATION =
      Pattern.compile(
          "implementation group: ['\"](.*)['\"], name: ['\"](.*)['\"], version: ['\"](.*)['\"]");
  private static final String GRADLE_DEPENDENCY_SEPARATOR = ":";
  // the dependency group id
  private final String groupId;
  // the dependency artifact id
  private final String artifactId;
  // the dependency version
  private String version;
  // the dependency type, defaults to jar
  private String type = "jar";
  // the dependency scope, defaults to compile
  private String scope = "compile";
  // the dependency repo, defaults to null
  private Repository repository;

  /**
   * Creates a new Dependency object
   *
   * @param groupId the dependency's group ID
   * @param artifactId the dependency's artifact ID
   * @param version the dependency's version
   */
  public Dependency(String groupId, String artifactId, String version) {
    this(groupId, artifactId, version, null, null);
  }

  /**
   * Creates a new Dependency object
   *
   * @param groupId the dependency's group ID
   * @param artifactId the dependency's artifact ID
   * @param version the dependency's version
   * @param type the dependency's type, or null for the default type
   */
  public Dependency(String groupId, String artifactId, String version, String type) {
    this(groupId, artifactId, version, type, null);
  }

  /**
   * Creates a new Dependency object
   *
   * @param groupId the dependency's group ID
   * @param artifactId the dependency's artifact ID
   * @param version the dependency's version
   * @param type the dependency's type, or null for the default type
   * @param scope the dependency's scope, or null for the default scope
   */
  public Dependency(String groupId, String artifactId, String version, String type, String scope) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    if (type != null) {
      this.type = type;
    }
    if (scope != null) {
      this.scope = scope;
    }
  }

  /**
   * Creates a Dependency class given the string representation. Supported string representations
   * for artifacts are: <br>
   * Gradle Groovy Implementations. e.g: <code>implementation 'com.test:test:1.0'`</code> <br>
   * Gradle Kotlin Implementation. e.g: <code>implementation("com.test:test:1.0")</code> <br>
   * Long Gradle Implementation. e.g: <code>implementation group: com.test, name: test, version: 1.0
   * </code>
   *
   * @param str the string representation for the dependency
   * @return the dependency created from the string representation
   */
  public static Dependency valueOf(String str) {
    Matcher groovyMatcher = GRADLE_GROOVY_IMPLEMENTATION.matcher(str);
    Matcher kotlinMatcher = GRADLE_KOTLIN_IMPLEMENTATION.matcher(str);
    Matcher gradleLongMatcher = GRADLE_LONG_IMPLEMENTATION.matcher(str);
    String dependencyValue = null;
    if (groovyMatcher.matches()) {
      dependencyValue = groovyMatcher.group(3).trim();
    } else if (kotlinMatcher.matches()) {
      dependencyValue = kotlinMatcher.group(3).trim();
    } else if (gradleLongMatcher.matches()) {
      dependencyValue =
          gradleLongMatcher.group(1).trim()
              + GRADLE_DEPENDENCY_SEPARATOR
              + gradleLongMatcher.group(2).trim()
              + GRADLE_DEPENDENCY_SEPARATOR
              + gradleLongMatcher.group(3).trim();
    }
    if (dependencyValue != null) {
      String[] dependencyPieces = dependencyValue.split(GRADLE_DEPENDENCY_SEPARATOR);
      if (dependencyPieces.length >= 2) {
        String artifactId = dependencyPieces[0];
        String groupId = dependencyPieces[1];
        String version = dependencyPieces[2];
        return new Dependency(artifactId, groupId, version);
      }
    }
    throw new IllegalArgumentException(
        "Failed to convert dependency string " + str + " to " + Dependency.class.getName());
  }

  /** @return the dependency artifact id */
  public String getArtifactId() {
    return artifactId;
  }

  /** @return the dependency group id */
  public String getGroupId() {
    return groupId;
  }

  /** @return the dependency type */
  public String getType() {
    return type;
  }

  /** Specifies the dependency's type */
  public void setType(String type) {
    this.type = type;
  }

  /** @return the dependency version */
  public String getVersion() {
    return version;
  }

  /**
   * Changes the dependency version
   *
   * @param version the new version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return groupId + ":" + artifactId + ":" + type + ":" + version + ":" + scope;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof Dependency) {
      Dependency inst = (Dependency) obj;
      return (inst.getGroupId().equals(getGroupId())
          && inst.getArtifactId().equals(getArtifactId())
          && inst.getVersion().equals(getVersion()));
    }
    return false;
  }

  public boolean compare(Dependency dependency, boolean ignoreVersion) {
    if (dependency == null) {
      return false;
    }
    if (!ignoreVersion) {
      return equals(dependency);
    } else {
      return (dependency.getGroupId().equals(getGroupId())
          && dependency.getArtifactId().equals(getArtifactId()));
    }
  }

  /** @return the dependency repository url, or null if it wasn't specified */
  public Repository getRepository() {
    return repository;
  }

  /** Specifies the dependency url */
  public void setRepository(Repository repo) {
    this.repository = repo;
  }
}
