package io.mohamed.resolver.cli;

import org.apache.commons.cli.Options;

public class Command {
  private final String name;
  private final Options options;

  public Command(String name, Options options) {
    this.name = name;
    this.options = options;
  }

  public String getName() {
    return name;
  }

  public Options getOptions() {
    return options;
  }
}
