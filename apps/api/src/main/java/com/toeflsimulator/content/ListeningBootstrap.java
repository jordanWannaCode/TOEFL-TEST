package com.toeflsimulator.content;
import java.util.*;
import java.nio.file.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.toeflsimulator.common.Json;
@Configuration
public class ListeningBootstrap {
    @Bean ApplicationRunner listeningContent(ListeningImporter importer,JdbcTemplate jdbc,Json json,
      @Value("${LISTENING_SECTION_DURATION_SECONDS:300}") int duration,@Value("${LISTENING_IMPORT_DIRECTORY:}") String directory) {
        return args -> {
            if(duration<1 || duration>86400) throw new IllegalArgumentException("Configure a positive custom Listening section duration.");
            Path root=directory.isBlank()?null:Path.of(directory).toRealPath();
            String text=root==null?new String(new ClassPathResource("listening/manifest.json").getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8):Files.readString(root.resolve("manifest.json"));
            var manifest=json.read(text); if(manifest.path("schemaVersion").asInt()!=1) throw new IllegalArgumentException("Unsupported manifest schema");
            List<UUID> approved=new ArrayList<>();
            for(var exercise:manifest.path("exercises")) {
                String file=exercise.path("audioFile").asText(); if(!file.matches("[a-zA-Z0-9_-]+[.]wav")) throw new IllegalArgumentException("Invalid audio file path");
                Path path=root==null?null:root.resolve(file).toRealPath(); if(path!=null && !path.startsWith(root)) throw new IllegalArgumentException("Audio path escapes import directory");
                byte[] audio=root==null?new ClassPathResource("listening/"+file).getInputStream().readAllBytes():Files.readAllBytes(path);
                UUID id=importer.importExercise(exercise,audio);
                if(exercise.path("reviewed").asBoolean()) { approved.add(id); publish(jdbc,json,"Listening: "+exercise.path("title").asText(),"PRACTICE",0,List.of(id),"FREE",0,true,true); }
            }
            if(!approved.isEmpty()) publish(jdbc,json,"Listening custom section","SECTION_TEST",duration,approved,"FORWARD_ONLY",1,false,false);
        };
    }
    private void publish(JdbcTemplate jdbc,Json json,String name,String mode,int seconds,List<UUID> ids,String navigation,int maxPlays,boolean pause,boolean seek) {
        var rows=jdbc.queryForList("SELECT * FROM simulator.blueprint_versions WHERE name=? ORDER BY version DESC LIMIT 1",name);
        if(!rows.isEmpty() && ((Number)rows.getFirst().get("duration_seconds")).intValue()==seconds && json.read(rows.getFirst().get("question_version_ids").toString()).equals(json.tree(ids))) return;
        int version=rows.isEmpty()?1:((Number)rows.getFirst().get("version")).intValue()+1;
        jdbc.update("INSERT INTO simulator.blueprint_versions(id,name,version,duration_seconds,verification_status,question_version_ids,section,mode,navigation_policy,playback_policy) VALUES (?,?,?,?,'CUSTOM_PRACTICE',?::jsonb,'LISTENING',?,?,?::jsonb)",UUID.randomUUID(),name,version,seconds,json.write(ids),mode,navigation,json.write(Map.of("maxPlays",maxPlays,"allowPause",pause,"allowSeek",seek,"resumeAllowed",true)));
    }
}
