package io.mohamed.resolver.gui.settings;

public class Setting {
  private final String keyName;
  private final SettingType type;
  private Object value;

  public Setting(String keyName, SettingType type) {
    this.keyName = keyName;
    this.type = type;
    if (type.equals(SettingType.JSON_ARRAY)) {
      value = "[]";
    } else {
      value = "";
    }
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public String getKeyName() {
    return keyName;
  }

  public SettingType getType() {
    return type;
  }
}
