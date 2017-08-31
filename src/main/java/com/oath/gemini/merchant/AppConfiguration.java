package com.oath.gemini.merchant;

import java.io.File;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class AppConfiguration {
    private final static Configuration config;

    static {
        try {
            String configDir = System.getProperty("config.dir", "target/conf");
            config = new PropertiesConfiguration(new File(configDir, "application.properties"));
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Configuration getConfig() {
        return config;
    }
}
