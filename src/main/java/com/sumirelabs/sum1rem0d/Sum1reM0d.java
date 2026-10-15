package com.sumirelabs.sum1rem0d;

import com.sumirelabs.sum1rem0d.config.ConfigStore;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Sum1reM0d {
    public static final String MOD_ID = "@{MOD_ID}";
    public static final String MOD_NAME = "@{MOD_NAME}";
    public static final String VERSION = "@{MOD_VERSION}";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static ConfigStore config;

    private Sum1reM0d() {}

    public static void initialize(Path configDirectory) {
        config = new ConfigStore(configDirectory.resolve(MOD_ID + ".json5"), com.sumirelabs.sum1rem0d.game.RegisteredConfigValidation::validate);
        config.load();
        LOGGER.info("{} {} initialized", MOD_NAME, VERSION);
    }

    public static ConfigStore config() {
        if (config == null) throw new IllegalStateException("Mod has not initialized");
        return config;
    }
    public static boolean initialized() { return config != null; }
}
