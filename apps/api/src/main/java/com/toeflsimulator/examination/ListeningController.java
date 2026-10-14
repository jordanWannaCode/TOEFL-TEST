package com.toeflsimulator.examination;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.toeflsimulator.identity.StudentPrincipal;
import com.toeflsimulator.common.Json;
@RestController
@RequestMapping("/api/v1")
public class ListeningController {
    private final ListeningPlayback playback; private final JdbcTemplate jdbc; private final Json json;
    public ListeningController(ListeningPlayback playback,JdbcTemplate jdbc,Json json) { this.playback=playback; this.jdbc=jdbc; this.json=json; }
    @GetMapping("/listening/exercises") Object exercises(@RequestParam(defaultValue="") String taskType,@RequestParam(defaultValue="") String difficulty,@RequestParam(defaultValue="") String skill) {
        return jdbc.query("SELECT q.id,q.content,b.id blueprint_id FROM simulator.question_versions q JOIN simulator.blueprint_versions b ON b.section='LISTENING' AND b.mode='PRACTICE' AND b.question_version_ids=jsonb_build_array(q.id::text) WHERE q.section='LISTENING' AND q.status='APPROVED' AND (?='' OR q.task_type=?) AND (?='' OR q.content->>'difficulty'=?) AND (?='' OR EXISTS (SELECT 1 FROM jsonb_array_elements(q.content->'questions') x WHERE x->>'skill'=?)) AND b.version=(SELECT max(v.version) FROM simulator.blueprint_versions v WHERE v.name=b.name) ORDER BY q.task_type",(rs,n)->{
            var content=json.read(rs.getString("content")); return Map.of("id",rs.getObject("id",UUID.class),"blueprintVersionId",rs.getObject("blueprint_id",UUID.class),"title",content.path("title"),"taskType",content.path("taskType"),"difficulty",content.path("difficulty"),"stimulus",content.path("stimulus"),"skills",content.path("questions").findValues("skill"));
        },taskType,taskType,difficulty,difficulty,skill,skill);
    }
    @GetMapping("/sessions/{id}/items/{item}/playback") Object state(@AuthenticationPrincipal StudentPrincipal user,@PathVariable UUID id,@PathVariable UUID item) { return playback.state(user.id(),id,item); }
    @PostMapping("/sessions/{id}/items/{item}/playback") Object start(@AuthenticationPrincipal StudentPrincipal user,@PathVariable UUID id,@PathVariable UUID item,@RequestHeader("Idempotency-Key") String key) { return playback.start(user.id(),id,item,key); }
    public record Progress(@NotNull UUID grantId,@Min(0) int positionMs,boolean completed) {}
    @PutMapping("/sessions/{id}/items/{item}/playback") Object progress(@AuthenticationPrincipal StudentPrincipal user,@PathVariable UUID id,@PathVariable UUID item,@Valid @RequestBody Progress body) { return playback.progress(user.id(),id,item,body.grantId(),body.positionMs(),body.completed()); }
    @GetMapping("/sessions/{id}/items/{item}/audio/{grant}") ResponseEntity<Resource> audio(@AuthenticationPrincipal StudentPrincipal user,@PathVariable UUID id,@PathVariable UUID item,@PathVariable UUID grant) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav")).cacheControl(CacheControl.noStore()).header("Accept-Ranges","bytes").body(new ByteArrayResource(playback.audio(user.id(),id,item,grant)));
    }
    @GetMapping("/sessions/{id}/items/{item}/transcript") Object transcript(@AuthenticationPrincipal StudentPrincipal user,@PathVariable UUID id,@PathVariable UUID item) { return playback.transcript(user.id(),id,item); }
}
