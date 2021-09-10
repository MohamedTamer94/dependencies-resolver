package io.mohamed.resolver.gui.settings;

import java.util.prefs.Preferences;

public class SettingsConstants {
  public static final Preferences preferences = Preferences.userRoot();
  public static final String REPOS_SETTING_KEY = "repos";
  public static final String MERGE_LIBRARIES_SETTINGS_KEY = "mergeLibraries";
  public static final String JAR_ONLY_SETTINGS_KEY = "jarOnly";
  public static final String VERBOSE_SETTINGS_KEY = "verbose";
  public static final String FILTER_APPINVENTOR_DEPENDENCIES_SETTINGS_KEY =
      "filterAppinventorDependencies";
}
