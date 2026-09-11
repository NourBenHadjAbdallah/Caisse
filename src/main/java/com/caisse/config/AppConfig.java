package com.caisse.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads Supabase connection settings from config.properties (on the classpath).
 * Edit src/main/resources/config.properties with your project's URL and anon key.
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load config.properties", e);
        }
    }

    private AppConfig() {}

    public static String supabaseUrl() {
        return PROPS.getProperty("supabase.url", "").replaceAll("/+$", "");
    }

    public static String supabaseAnonKey() {
        return PROPS.getProperty("supabase.anonKey", "");
    }
}
