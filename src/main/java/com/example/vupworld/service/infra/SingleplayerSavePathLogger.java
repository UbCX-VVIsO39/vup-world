package com.example.vupworld.service.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Profile("singleplayer")
public class SingleplayerSavePathLogger {
    private static final Logger log = LoggerFactory.getLogger(SingleplayerSavePathLogger.class);

    private final Environment environment;

    public SingleplayerSavePathLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSavePath() {
        Path saveDirectory = Path.of(environment.getProperty(
                "game.singleplayer.save-directory",
                "./tmp/singleplayer-save"
        )).toAbsolutePath().normalize();
        String databaseName = environment.getProperty("game.singleplayer.database-name", "vupworld");
        try {
            Files.createDirectories(saveDirectory);
        } catch (Exception exception) {
            log.warn("Singleplayer save directory could not be created: {}", saveDirectory, exception);
        }
        log.info("Singleplayer save directory: {}", saveDirectory);
        log.info("Singleplayer save database: {}", saveDirectory.resolve(databaseName + ".mv.db"));
    }
}
