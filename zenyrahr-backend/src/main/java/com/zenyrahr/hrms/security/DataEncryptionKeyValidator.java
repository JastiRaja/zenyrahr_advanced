package com.zenyrahr.hrms.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataEncryptionKeyValidator implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean strictMode = isStrictMode(activeProfiles);
        String strictProperty = environment.getProperty("app.data.encryption.strict");
        boolean envKeyPresent = hasText(System.getenv("APP_DATA_ENCRYPTION_KEY"));
        boolean sysPropKeyPresent = hasText(System.getProperty("app.data.encryption.key"));
        boolean envStrictPresent = hasText(System.getenv("APP_DATA_ENCRYPTION_STRICT"));
        boolean envFileKeyPresent = LocalEnvFile.hasText("APP_DATA_ENCRYPTION_KEY");
        boolean envFileStrictPresent = LocalEnvFile.hasText("APP_DATA_ENCRYPTION_STRICT");

        log.info(
                "Encryption config diagnostic: strictMode={}, strictProperty={}, envKeyPresent={}, systemPropertyKeyPresent={}, envStrictPresent={}, envFileKeyPresent={}, envFileStrictPresent={}, activeProfiles={}",
                strictMode,
                strictProperty,
                envKeyPresent,
                sysPropKeyPresent,
                envStrictPresent,
                envFileKeyPresent,
                envFileStrictPresent,
                Arrays.toString(activeProfiles)
        );

        if (FieldCrypto.isUsingDefaultDevKey()) {
            String message = "Unsafe encryption key configuration detected. Configure APP_DATA_ENCRYPTION_KEY or app.data.encryption.key.";
            if (strictMode) {
                throw new IllegalStateException(message + " Startup blocked in strict mode.");
            }
            log.warn("{} Running with default dev key because strict mode is OFF.", message);
            return;
        }

        log.info("Sensitive field encryption key loaded from {}", FieldCrypto.keySource());
    }

    private boolean isStrictMode(String[] activeProfiles) {
        String explicit = environment.getProperty("app.data.encryption.strict");
        if (explicit != null) {
            return "true".equalsIgnoreCase(explicit.trim());
        }
        String envStrict = System.getenv("APP_DATA_ENCRYPTION_STRICT");
        if (hasText(envStrict)) {
            return "true".equalsIgnoreCase(envStrict.trim());
        }
        String envFileStrict = LocalEnvFile.get("APP_DATA_ENCRYPTION_STRICT");
        if (hasText(envFileStrict)) {
            return "true".equalsIgnoreCase(envFileStrict.trim());
        }
        return Arrays.stream(activeProfiles)
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
