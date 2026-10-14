package com.toeflsimulator.content;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import com.toeflsimulator.common.Json;
@RestController
@RequestMapping("/api/v1/writing")
public class WritingController {
    private final JdbcTemplate jdbc; private final Json json;
    public WritingController(JdbcTemplate jdbc,Json json) { this.jdbc=jdbc; this.json=json; }
    @GetMapping("/exercises") Object exercises(@RequestParam(defaultValue="PRACTICE") String mode) {
        return jdbc.query("SELECT DISTINCT ON(q.id) q.id,q.content,b.id blueprint FROM simulator.question_versions q JOIN simulator.blueprint_versions b ON b.question_version_ids @> jsonb_build_array(q.id::text) WHERE q.section='WRITING' AND q.status='APPROVED' AND b.mode=? ORDER BY q.id,b.version DESC",(rs,n)->Map.of("id",rs.getObject("id",UUID.class),"content",json.read(rs.getString("content")),"blueprintVersionId",rs.getObject("blueprint",UUID.class)),mode);
    }
}
