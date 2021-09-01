// -*- mode: java; c-basic-offset: 2; -*-
/*
 * MIT License
 *
 * Copyright (c) 2021 Mohamed Tamer Elsayed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.mohamed.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A class to represent a maven dependency
 * a maven dependency is represented in POM XML files as:
 * <dependency>
 *   <groupId>com.example</groupId>
 *   <artifactId>test-project</artifactId>
 *   <version>1.0</version>
 *   <scope>runtime</scope>
 *   <type>aar</type>
 * </dependency>
 *
 * @author Mohamed Tamer
 */
public class Dependency {
  // the dependency group id
  private final String groupId;
  // the dependency artifact id
  private final String artifactId;
  // the dependency version
  private final String version;
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
   * @param type the dependency's type, or null for the default type
   * @param scope the dependency's scope, or null for the default scope
   */
  public Dependency(
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version,
      @Nullable String type,
      @Nullable String scope) {
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
  @Nullable
  public Repository getRepository() {
    return repository;
  }

  /** Specifies the dependency url */
  public void setRepository(Repository repo) {
    this.repository = repo;
  }
}
