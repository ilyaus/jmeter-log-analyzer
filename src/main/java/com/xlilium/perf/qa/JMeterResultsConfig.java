package com.xlilium.perf.qa;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.List;

public class JMeterResultsConfig {

  private Config config;
  private static final String CONFIG_ROOT = "jmeter.logs";

  JMeterResultsConfig(String configFile) {
    loadConfig(configFile);
  }

  public void loadConfig(String configFile) {
    config = ConfigFactory.load(configFile).getConfig(CONFIG_ROOT);
  }

  public String getConfig(String configItem) {
    return System.getProperty(configItem, config != null ? config.getString(configItem) : null);
  }

  public List<String> getStringList(String configItem) {
    return config != null ? config.getStringList(configItem) : null;
  }
}
