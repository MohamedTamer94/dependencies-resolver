/*
 *
 *  * Copyright (c) 2021 Mohamed Tamer
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining
 *  * a copy of this software and associated documentation files (the
 *  * "Software"), to deal in the Software without restriction, including
 *  * without limitation the rights to use, copy, modify, merge, publish,
 *  * distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to
 *  * the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be
 *  * included in all copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *  *
 *
 */

package io.mohamed.resolver.gui.settings;

/**
 * A class representing a user setting
 *
 * @author Mohamed Tamer
 */
public class Setting {
  // the setting key name
  private final String keyName;
  // the setting type
  private final SettingType type;
  // the setting value
  private Object value;

  /**
   * Creates a new setting with the given SettingType and keyName
   *
   * @param keyName the setting keyName, used for saving and loading the setting from the
   *     preferences
   * @param type the setting type
   */
  public Setting(String keyName, SettingType type) {
    this.keyName = keyName;
    this.type = type;
    if (type.equals(SettingType.JSON_ARRAY)) {
      value = "[]";
    } else {
      value = "";
    }
  }

  /** @return the setting value object */
  public Object getValue() {
    return value;
  }

  /**
   * Changes the setting value
   *
   * @param value the new value
   */
  public void setValue(Object value) {
    this.value = value;
  }

  /** @return the setting key name */
  public String getKeyName() {
    return keyName;
  }

  /** @return the setting type */
  public SettingType getType() {
    return type;
  }
}

