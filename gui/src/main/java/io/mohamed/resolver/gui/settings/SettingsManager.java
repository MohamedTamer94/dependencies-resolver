package io.mohamed.resolver.gui.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.json.JSONArray;

public class SettingsManager {
  private static final ArrayList<Setting> ALL_SETTINGS =
      new ArrayList<>(
          Arrays.asList(
              new Setting(SettingsConstants.REPOS_SETTING_KEY, SettingType.JSON_ARRAY),
              new Setting(SettingsConstants.MERGE_LIBRARIES_SETTINGS_KEY, SettingType.STRING),
              new Setting(SettingsConstants.JAR_ONLY_SETTINGS_KEY, SettingType.STRING),
              new Setting(SettingsConstants.VERBOSE_SETTINGS_KEY, SettingType.STRING),
              new Setting(
                  SettingsConstants.FILTER_APPINVENTOR_DEPENDENCIES_SETTINGS_KEY,
                  SettingType.STRING)));
  private static final ArrayList<Setting> loadedSettings = new ArrayList<>();
  private static final Preferences preferences = Preferences.userRoot();
  private static OnSettingsChangeListener listener;

  public static Setting getSettingForKey(String keyName) {
    for (Setting setting : loadedSettings) {
      if (setting.getKeyName().equals(keyName)) {
        return setting;
      }
    }
    throw new NullPointerException("No setting found for setting key " + keyName);
  }

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

  public static void setOnSettingsChangeListener(OnSettingsChangeListener listener) {
    SettingsManager.listener = listener;
  }

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

  public static void resetSettings() throws BackingStoreException {
    preferences.clear();
    listener.change();
  }
}
