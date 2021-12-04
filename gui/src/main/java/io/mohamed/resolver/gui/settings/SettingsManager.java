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

package io.mohamed.resolver.gui.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.json.JSONArray;

/**
 * A class that manages the user defined settings
 *
 * @author Mohamed Tamer
 */
public class SettingsManager {
  private static final ArrayList<Setting> ALL_SETTINGS =
      new ArrayList<>(
          Arrays.asList(
              new Setting(SettingsConstants.REPOS_SETTING_KEY, SettingType.JSON_ARRAY),
              new Setting(SettingsConstants.MERGE_LIBRARIES_SETTINGS_KEY, SettingType.STRING),
              new Setting(SettingsConstants.JAR_ONLY_SETTINGS_KEY, SettingType.STRING),
              new Setting(SettingsConstants.VERBOSE_SETTINGS_KEY, SettingType.STRING),
              new Setting(SettingsConstants.JETIFY_SETTINGS_KEY, SettingType.STRING)));
  private static final ArrayList<Setting> loadedSettings = new ArrayList<>();
  private static final Preferences preferences = Preferences.userRoot();
  private static OnSettingsChangeListener listener;

  /**
   * Returns the setting object for the key
   *
   * @param keyName the setting key name
   * @return the setting object
   */
  public static Setting getSettingForKey(String keyName) {
    for (Setting setting : loadedSettings) {
      if (setting.getKeyName().equals(keyName)) {
        return setting;
      }
    }
    throw new NullPointerException("No setting found for setting key " + keyName);
  }

  /** Loads all settings from the user preferences */
  public static void load() {
    for (Setting setting : ALL_SETTINGS) {
      String value = preferences.get(setting.getKeyName(), setting.getValue().toString());
      if (setting.getType().equals(SettingType.STRING)) {
        setting.setValue(value);
        loadedSettings.add(setting);
      } else if (setting.getType().equals(SettingType.JSON_ARRAY)) {
        ArrayList<String> list = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(preferences.get(setting.getKeyName(), "[]"));
        for (int i = 0; i < jsonArray.length(); i++) {
          list.add(jsonArray.getString(i));
        }
        setting.setValue(list);
        loadedSettings.add(setting);
      }
    }
  }

  /**
   * Registers a listener to listen for setting changes
   *
   * @param listener the listener
   */
  public static void setOnSettingsChangeListener(OnSettingsChangeListener listener) {
    SettingsManager.listener = listener;
  }

  /**
   * Updates the value for the setting
   *
   * @param setting the setting
   * @param value the new value
   */
  public static void updateSettingValue(Setting setting, Object value) {
    // if single value, save as a string, otherwise save as a JSONArray.
    if (setting.getType().equals(SettingType.STRING)) {
      preferences.put(setting.getKeyName(), value.toString());
    } else if (setting.getType().equals(SettingType.JSON_ARRAY)) {
      Iterable<?> iterable = (Iterable<?>) setting.getValue();
      JSONArray valuesJSONArray = new JSONArray();
      for (Object value1 : iterable) {
        valuesJSONArray.put(value1);
      }
      preferences.put(setting.getKeyName(), valuesJSONArray.toString());
    }
    listener.change();
  }

  /**
   * Resets settings to its default values
   *
   * @throws BackingStoreException if this operation cannot be completed due to a failure in the
   *     backing store, or inability to communicate with it.
   */
  public static void resetSettings() throws BackingStoreException {
    preferences.clear();
    listener.change();
  }
}
