package com.toeflsimulator.blueprint;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@Configuration
@Profile("local")
public class LocalBlueprintConfiguration {
    @Bean ApplicationRunner practiceTiming(JdbcTemplate jdbc, PlatformTransactionManager transactions,
        @Value("${READING_PRACTICE_DURATION_SECONDS:0}") int seconds) {
        return args -> {
            if(seconds<0 || seconds>86400) throw new IllegalStateException("Practice duration must be between 0 and 86400 seconds.");
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.execute("LOCK TABLE simulator.blueprint_versions IN EXCLUSIVE MODE");
                var latest=jdbc.queryForMap("SELECT * FROM simulator.blueprint_versions WHERE name='Reading practice' ORDER BY version DESC LIMIT 1");
                if ((int)latest.get("duration_seconds") != seconds) jdbc.update("INSERT INTO simulator.blueprint_versions(id,name,version,duration_seconds,verification_status,question_version_ids) VALUES (?,'Reading practice',?,?,'CUSTOM_PRACTICE',?::jsonb)", UUID.randomUUID(),(int)latest.get("version")+1,seconds,latest.get("question_version_ids").toString());
            });
        };
    }
}
