package com.toeflsimulator.examination;
import java.util.*;
import java.net.URI;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.toeflsimulator.identity.StudentPrincipal;
import com.toeflsimulator.common.ApiException;
@RestController
@RequestMapping("/api/v1")
public class ReadingController {
    private final ReadingSessions sessions;
    public ReadingController(ReadingSessions sessions) { this.sessions=sessions; }
    public record Start(@NotNull UUID blueprintVersionId,@NotBlank String mode) {}
    @GetMapping("/blueprints") Object blueprints() { return sessions.blueprints(); }
    @PostMapping("/sessions") ResponseEntity<?> start(@AuthenticationPrincipal StudentPrincipal owner,@Valid @RequestBody Start body,@RequestHeader("Idempotency-Key") String key) {
        var view=sessions.create(owner.id(),body.blueprintVersionId(),body.mode(),key);
        return ResponseEntity.created(URI.create("/api/v1/sessions/"+view.id())).eTag(Integer.toString(view.version())).body(view);
    }
    @GetMapping("/sessions/{id}") ResponseEntity<?> get(@AuthenticationPrincipal StudentPrincipal owner,@PathVariable UUID id) {
        var view=sessions.get(owner.id(),id); return ResponseEntity.ok().eTag(Integer.toString(view.version())).body(view);
    }
    @PutMapping("/sessions/{id}/items/{itemId}/response") ResponseEntity<?> save(@AuthenticationPrincipal StudentPrincipal owner,@PathVariable UUID id,@PathVariable UUID itemId,@RequestHeader("If-Match") String version,@RequestHeader("Idempotency-Key") String key,@RequestBody JsonNode response) {
        return etag(sessions.save(owner.id(),id,itemId,version(version),key,response));
    }
    @PostMapping("/sessions/{id}/submit") ResponseEntity<?> submit(@AuthenticationPrincipal StudentPrincipal owner,@PathVariable UUID id,@RequestHeader("If-Match") String version,@RequestHeader("Idempotency-Key") String key) {
        return etag(sessions.submit(owner.id(),id,version(version),key));
    }
    @GetMapping("/sessions/{id}/results") Object results(@AuthenticationPrincipal StudentPrincipal owner,@PathVariable UUID id) { return sessions.results(owner.id(),id); }
    @GetMapping("/dashboard") Object dashboard(@AuthenticationPrincipal StudentPrincipal owner) { return sessions.dashboard(owner.id()); }
    private ResponseEntity<?> etag(JsonNode result) { return ResponseEntity.ok().eTag(result.path("version").asText()).body(result); }
    private int version(String value) {
        if(!value.matches("\"[1-9][0-9]*\"")) throw new ApiException(422,"INVALID_VERSION","Provide the quoted ETag from the session response.");
        try { return Integer.parseInt(value.substring(1,value.length()-1)); } catch(NumberFormatException e) { throw new ApiException(422,"INVALID_VERSION","Invalid ETag."); }
    }
}
