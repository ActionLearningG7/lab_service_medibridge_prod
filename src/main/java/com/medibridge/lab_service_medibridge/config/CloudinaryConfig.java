package com.medibridge.lab_service_medibridge.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

/**
 * Cloudinary Configuration for Lab Report Storage
 *
 * Security:
 * - Loads credentials from environment variables only
 * - Supports .env file in dev profile via dotenv-java
 * - Fails fast if required variables missing in non-dev profiles
 * - Never logs or exposes API secrets
 */
@Configuration
@Slf4j
public class CloudinaryConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Creates Cloudinary bean for dev profile with dotenv support
     */
    @Bean
    @Profile("dev")
    public Cloudinary cloudinaryDev() {
        log.info("Initializing Cloudinary for DEV profile with .env support");

        // Load .env file if exists
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            log.debug(".env file loaded successfully");
        } catch (Exception e) {
            log.warn("No .env file found, using system environment variables");
        }

        String cloudName = getEnvVar(dotenv, "CLOUDINARY_CLOUD_NAME");
        String apiKey = getEnvVar(dotenv, "CLOUDINARY_API_KEY");
        String apiSecret = getEnvVar(dotenv, "CLOUDINARY_API_SECRET");

        if (cloudName == null || apiKey == null || apiSecret == null) {
            log.warn("Cloudinary credentials not fully configured in dev. Upload features will be disabled.");
            log.warn("Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET in .env file");
            // Return dummy config for dev - will fail at upload time
            return new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "not_configured",
                    "api_key", "not_configured",
                    "api_secret", "not_configured"
            ));
        }

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure", getEnvVar(dotenv, "CLOUDINARY_SECURE", "true"));

        log.info("✓ Cloudinary configured successfully for cloud: {}", cloudName);
        // Never log api_key or api_secret

        return new Cloudinary(config);
    }

    /**
     * Creates Cloudinary bean for production with strict validation
     */
    @Bean
    @Profile("!dev")
    public Cloudinary cloudinaryProd() {
        log.info("Initializing Cloudinary for PRODUCTION profile");

        String cloudName = "du1pyzthl";
        String apiKey = "451954261338871";
        String apiSecret = "vL9wmoZn8Yid40FtSS1irqtT3N8";

        // Fail fast in production if credentials missing
        if (cloudName == null || cloudName.isBlank()) {
            throw new IllegalStateException("CLOUDINARY_CLOUD_NAME environment variable is required in production");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("CLOUDINARY_API_KEY environment variable is required in production");
        }
        if (apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("CLOUDINARY_API_SECRET environment variable is required in production");
        }

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure", System.getenv().getOrDefault("CLOUDINARY_SECURE", "true"));

        log.info("✓ Cloudinary configured successfully for cloud: {}", cloudName);

        return new Cloudinary(config);
    }

    /**
     * Helper to get environment variable with dotenv fallback
     */
    private String getEnvVar(Dotenv dotenv, String key) {
        String value = System.getenv(key);
        if (value == null && dotenv != null) {
            value = dotenv.get(key);
        }
        return value;
    }

    /**
     * Helper to get environment variable with default value
     */
    private String getEnvVar(Dotenv dotenv, String key, String defaultValue) {
        String value = getEnvVar(dotenv, key);
        return value != null ? value : defaultValue;
    }
}
