package main.java.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Responsible for loading and parsing the configuration file.
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String CONFIG_FILE = "config.cfg";
    private final Properties config;

    /**
     * Constructor for the ConfigLoader class.
     * Loads the configuration from the file.
     */
    public ConfigLoader() {
        this.config = new Properties();
        loadConfig();
    }

    /**
     * Loads the configuration file into the Properties object.
     */
    private void loadConfig() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            config.load(fis);
        } catch (IOException e) {
            logger.error("Failed to load" + CONFIG_FILE + "configuration file", e);
        }
    }

    /**
     * Get a string property from the configuration.
     *
     * @param key the property key
     * @return the property value
     */
    public String getStringProperty(String key) {
        return config.getProperty(key);
    }

    /**
     * Get an integer property from the configuration.
     *
     * @param key the property key
     * @return the property value as an integer
     */
    public int getIntProperty(String key) {
        return Integer.parseInt(config.getProperty(key));
    }
}
