package com.oath.gemini.merchant;

import java.io.File;
import java.io.InputStream;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class AppConfiguration {
    private final static Configuration config;

    static {
        try {
            // We must always load from an external file in production release
            String configDir = System.getProperty("config.dir", "target/conf");
            File pfile = new File(configDir, "application.properties");

            if (pfile.exists()) {
                config = new PropertiesConfiguration(pfile);
            } else {
                // Alternatively, load from an embedded property file in non-production environment
                InputStream inputStream = AppConfiguration.class.getResourceAsStream("application.properties");
                config = new PropertiesConfiguration();
                ((PropertiesConfiguration) config).load(inputStream);
            }

            File logback = new File(configDir, "logback.xml");
            System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.log4j.Log4jMLog");

            if (logback.exists()) {
                System.setProperty("logback.configurationFile", logback.getAbsolutePath());
            }

        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Configuration getConfig() {
        return config;
    }
}
