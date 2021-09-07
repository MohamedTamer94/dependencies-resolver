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

package io.mohamed.resolver.core.model;

/**
 * A class representing a maven project's property. A property is typically is represented in POM
 * XML files as: <properties> <my.property>property value</my.property> </properties>
 *
 * @author Mohamed Tamer
 */
public class ProjectProperty {

  // the property name
  private final String value;
  // the property value
  private final String name;

  /**
   * Creates a new project property
   *
   * @param name the property name
   * @param value the property value
   */
  public ProjectProperty(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /** @return the property's value */
  public String getValue() {
    return value;
  }

  /** @return the property's value */
  public String getName() {
    return name;
  }
}
