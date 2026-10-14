package com.toeflsimulator.content;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import javax.sound.sampled.AudioSystem;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.toeflsimulator.media.ObjectStorage;
import com.toeflsimulator.common.Json;
@Service
public class ListeningImporter {
    private final JdbcTemplate jdbc; private final Json json; private final ObjectStorage storage;
    public ListeningImporter(JdbcTemplate jdbc,Json json,ObjectStorage storage) { this.jdbc=jdbc; this.json=json; this.storage=storage; }
    @Transactional
    public UUID importExercise(JsonNode exercise,byte[] audio) {
        validate(exercise,audio); UUID id=UUID.fromString(exercise.path("id").asText());
        var immutable=exercise.deepCopy(); ((ObjectNode)immutable).remove("reviewed");
        String fingerprint=sha(json.write(canonical(immutable)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var existing=jdbc.queryForList("SELECT content_fingerprint,status FROM simulator.question_versions WHERE id=?",id);
        if(!existing.isEmpty()) {
            if(!fingerprint.equals(existing.getFirst().get("content_fingerprint"))) throw new IllegalArgumentException("Published revisions are immutable; use a new revision ID.");
            if(exercise.path("reviewed").asBoolean() && existing.getFirst().get("status").equals("DRAFT")) jdbc.update("UPDATE simulator.question_versions SET status='APPROVED' WHERE id=?",id);
            storage.put("listening/"+exercise.path("sha256").asText()+".wav",audio); return id;
        }
        String objectKey="listening/"+exercise.path("sha256").asText()+".wav"; storage.put(objectKey,audio);
        // Transcript and answer keys are never embedded in public content.
        ObjectNode content=(ObjectNode)json.tree(Map.of("taskType",exercise.path("taskType").asText(),"title",exercise.path("title").asText(),"difficulty",exercise.path("difficulty").asText(),"instructions","Listen to the audio, then answer the questions.","questions",exercise.path("questions")));
        content.set("stimulus",json.tree(Map.of("id",id,"durationMs",exercise.path("durationMs").asInt(),"synthetic",exercise.path("synthetic").asBoolean(),"speakers",exercise.path("speakers"))));
        jdbc.update("INSERT INTO simulator.audio_assets(id,object_key,sha256,media_type,size_bytes,duration_ms,synthetic,speakers) VALUES (?,?,?,'audio/wav',?,?,?,?::jsonb) ON CONFLICT(object_key) DO NOTHING",id,objectKey,exercise.path("sha256").asText(),audio.length,exercise.path("durationMs").asInt(),exercise.path("synthetic").asBoolean(),json.write(exercise.path("speakers")));
        jdbc.update("INSERT INTO simulator.question_versions(id,question_id,version,section,task_type,status,content,content_fingerprint) VALUES (?,?,?,'LISTENING',?,?,?::jsonb,?)",id,id,exercise.path("version").asInt(),exercise.path("taskType").asText(),exercise.path("reviewed").asBoolean()?"APPROVED":"DRAFT",json.write(content),fingerprint);
        jdbc.update("INSERT INTO simulator.answer_keys(question_version_id,answers,explanation,feedback) VALUES (?,?::jsonb,?,?::jsonb)",id,json.write(exercise.path("answers")),"Review each question and its explanation.",json.write(exercise.path("feedback")));
        jdbc.update("INSERT INTO simulator.listening_stimuli(question_version_id,audio_asset_id,transcript) VALUES (?,?,?::jsonb)",id,jdbc.queryForObject("SELECT id FROM simulator.audio_assets WHERE object_key=?",UUID.class,objectKey),json.write(exercise.path("turns"))); return id;
    }
    public static String sha(byte[] data) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); } catch(Exception e) { throw new IllegalStateException(e); } }
    private JsonNode canonical(JsonNode node) {
        if(node.isObject()) { var result=JsonNodeFactory.instance.objectNode(); var names=new TreeSet<String>(); node.fieldNames().forEachRemaining(names::add); names.forEach(name->result.set(name,canonical(node.get(name)))); return result; }
        if(node.isArray()) { var result=JsonNodeFactory.instance.arrayNode(); node.forEach(value->result.add(canonical(value))); return result; } return node;
    }
    private void validate(JsonNode e,byte[] audio) {
        if(!Set.of("LISTEN_AND_CHOOSE_A_RESPONSE","LISTEN_TO_A_CONVERSATION","LISTEN_TO_AN_ANNOUNCEMENT","LISTEN_TO_AN_ACADEMIC_TALK").contains(e.path("taskType").asText()) || !Set.of("A2","B1","B2","C1").contains(e.path("difficulty").asText()) || e.path("version").asInt()<1 || e.path("title").asText().isBlank() || audio.length>10485760 || !sha(audio).equals(e.path("sha256").asText())) throw new IllegalArgumentException("Invalid exercise or audio checksum");
        try(var wav=AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio))) {
            long duration=Math.round(1000d*wav.getFrameLength()/wav.getFormat().getFrameRate());
            if(Math.abs(duration-e.path("durationMs").asLong())>2 || duration<100) throw new IllegalArgumentException("Audio duration mismatch");
        } catch(Exception ex) { throw new IllegalArgumentException("Valid bounded WAV audio required",ex); }
        Set<String> speakerIds=new HashSet<>(); e.path("speakers").forEach(s->{ if(!speakerIds.add(s.path("id").asText()) || s.path("label").asText().isBlank()) throw new IllegalArgumentException("Invalid speaker"); });
        if(speakerIds.isEmpty() || e.path("turns").isEmpty()) throw new IllegalArgumentException("Transcript and speakers required");
        e.path("turns").forEach(t->{ if(!speakerIds.contains(t.path("speakerId").asText()) || t.path("text").asText().isBlank()) throw new IllegalArgumentException("Invalid transcript association"); });
        Set<String> ids=new HashSet<>();
        for(var q:e.path("questions")) {
            String id=q.path("id").asText(); if(id.isBlank() || !ids.add(id) || q.path("prompt").asText().isBlank() || q.path("skill").asText().isBlank() || q.path("options").size()!=4) throw new IllegalArgumentException("Invalid question group");
            Set<String> options=new HashSet<>(); q.path("options").forEach(o->{ if(!options.add(o.path("id").asText()) || o.path("text").asText().isBlank()) throw new IllegalArgumentException("Invalid option"); });
            if(!options.contains(e.path("answers").path(id).asText()) || e.path("feedback").path(id).asText().isBlank()) throw new IllegalArgumentException("Answer and explanation do not match question");
        }
        if(ids.isEmpty() || ids.size()!=e.path("answers").size() || ids.size()!=e.path("feedback").size()) throw new IllegalArgumentException("Question/key count mismatch");
    }
}
