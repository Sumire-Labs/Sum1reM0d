package net.dimaskama.mcef.impl;

import java.nio.file.*;
import org.slf4j.*;

/** Loader-independent bootstrap for the Sum1reM0d LGPL adapter. */
public final class MCEFModern {
    public static final String MOD_ID = "mcef-modern";
    public static final Logger LOGGER = LoggerFactory.getLogger("MCEF Modern");
    public static Path MOD_DIR, JCEF_PATH, CACHE_PATH;
    private MCEFModern() {}
    public static void configure(Path configDirectory) {
        MOD_DIR = configDirectory.resolve(MOD_ID);
        JCEF_PATH = MOD_DIR.resolve("jcef");
        CACHE_PATH = MOD_DIR.resolve("cache");
        System.setProperty("java.awt.headless", "false");
        // Start AWT from the loader's client bootstrap, before CEF's asynchronous
        // initialization can inherit the common pool's restricted thread context.
        ClassLoader loader = MCEFModern.class.getClassLoader();
        java.awt.EventQueue.invokeLater(() -> Thread.currentThread().setContextClassLoader(loader));
        try { Files.createDirectories(JCEF_PATH); Files.createDirectories(CACHE_PATH); }
        catch (java.io.IOException exception) { throw new IllegalStateException("Cannot create MCEF directories", exception); }
    }
    public static void shutdown() {
        MCEFApiImpl.shutdown();
    }
}
