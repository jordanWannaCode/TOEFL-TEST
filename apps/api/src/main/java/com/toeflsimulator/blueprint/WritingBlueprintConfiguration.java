package com.toeflsimulator.blueprint;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@Configuration
public class WritingBlueprintConfiguration {
    @Bean ApplicationRunner writingTiming(JdbcTemplate jdbc,PlatformTransactionManager tx,@Value("${WRITING_SECTION_DURATION_SECONDS:300}") int seconds) {
        return args->{
            if(seconds<1 || seconds>86400) throw new IllegalStateException("Writing section duration must be 1..86400 seconds.");
            new TransactionTemplate(tx).executeWithoutResult(status->{
                jdbc.execute("LOCK TABLE simulator.blueprint_versions IN EXCLUSIVE MODE");
                var rows=jdbc.queryForList("SELECT DISTINCT ON(name) * FROM simulator.blueprint_versions WHERE section='WRITING' AND mode='SECTION_TEST' ORDER BY name,version DESC");
                for(var row:rows) if((int)row.get("duration_seconds")!=seconds) jdbc.update("INSERT INTO simulator.blueprint_versions(id,name,version,duration_seconds,verification_status,question_version_ids,section,mode,navigation_policy) VALUES (?,?,?,?,'CUSTOM_PRACTICE',?::jsonb,'WRITING','SECTION_TEST',?)",UUID.randomUUID(),row.get("name"),(int)row.get("version")+1,seconds,row.get("question_version_ids").toString(),row.get("navigation_policy"));
            });
        };
    }
}
