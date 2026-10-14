package com.toeflsimulator.examination;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="server.servlet.session.cookie.secure=false")
class ReadingIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    UUID owner, other; Client student, stranger; String email,password;
    List<UUID> testBlueprints=new ArrayList<>();
    @BeforeEach void setup() throws Exception {
        owner=UUID.randomUUID(); other=UUID.randomUUID(); email=owner+"@example.test"; password=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO simulator.users(id,email,password_hash) VALUES (?,?,?),(?,?,?)",owner,email,encoder.encode(password),other,other+"@example.test",encoder.encode(password));
        student=new Client(); student.login(email,password); stranger=new Client(); stranger.login(other+"@example.test",password);
    }
    @AfterEach void cleanup() { jdbc.update("DELETE FROM simulator.users WHERE id IN (?,?)",owner,other); testBlueprints.forEach(id->jdbc.update("DELETE FROM simulator.blueprint_versions WHERE id=?",id)); }
    @Test void fullReadingFlowKeepsKeysPrivateAndPersistsResults() throws Exception {
        var session=start(); String id=session.path("id").asText(); int version=session.path("version").asInt();
        assertThat(session.path("items").size()).isEqualTo(3);
        assertThat(session.toString()).doesNotContain("correctAnswers","explanation","rubricVersion","password_hash");
        assertThat(student.get("/sessions/"+id+"/results").statusCode()).isEqualTo(409);
        var responses=List.of(Map.of("g1"," ROW ","g2","turn"),Map.of("choice","a"),Map.of("choice","b"));
        for(int i=0;i<3;i++) {
            String item=session.path("items").get(i).path("id").asText();
            var saved=student.request("PUT","/sessions/"+id+"/items/"+item+"/response",Map.of("answers",responses.get(i)),version,UUID.randomUUID().toString());
            assertThat(saved.statusCode()).isEqualTo(200); version=tree(saved).path("version").asInt();
        }
        var resumed=tree(student.get("/sessions/"+id));
        assertThat(resumed.path("items").get(0).path("response").path("answers").path("g2").asText()).isEqualTo("turn");
        assertThat(resumed.toString()).doesNotContain("correctAnswers","explanation");
        String submitKey=UUID.randomUUID().toString();
        assertThat(student.request("POST","/sessions/"+id+"/submit",Map.of(),version,submitKey).statusCode()).isEqualTo(200);
        assertThat(student.request("POST","/sessions/"+id+"/submit",Map.of(),version,submitKey).statusCode()).isEqualTo(200);
        var result=tree(student.get("/sessions/"+id+"/results"));
        assertThat(result.path("objectiveScore").path("correct").asInt()).isEqualTo(4);
        assertThat(result.path("objectiveScore").path("total").asInt()).isEqualTo(4);
        assertThat(result.path("items").get(0).path("explanation").asText()).contains("Borrow");
        assertThat(result.path("estimated").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM simulator.response_events WHERE item_id IN (SELECT id FROM simulator.session_items WHERE session_id=?)",Integer.class,UUID.fromString(id))).isEqualTo(3);
        var dashboard=tree(student.get("/dashboard")); assertThat(dashboard.path("completedSessions").asInt()).isEqualTo(1);
        assertThat(dashboard.path("sectionMetrics").get(0).path("objectiveAccuracy").asDouble()).isEqualTo(1.0);
        assertThat(tree(stranger.get("/dashboard")).path("completedSessions").asInt()).isZero();
    }
    @Test void isolationAppliesToReadsWritesAndResults() throws Exception {
        var session=start(); String id=session.path("id").asText(); String item=session.path("items").get(0).path("id").asText();
        assertThat(stranger.get("/sessions/"+id).statusCode()).isEqualTo(404);
        assertThat(stranger.get("/sessions/"+id+"/results").statusCode()).isEqualTo(404);
        assertThat(stranger.request("PUT","/sessions/"+id+"/items/"+item+"/response",Map.of("answers",Map.of()),1,UUID.randomUUID().toString()).statusCode()).isEqualTo(404);
        assertThat(stranger.request("POST","/sessions/"+id+"/submit",Map.of(),1,UUID.randomUUID().toString()).statusCode()).isEqualTo(404);
    }
    @Test void retriesAreIdempotentAndStaleOrMalformedWritesAreRejected() throws Exception {
        var session=start(); String id=session.path("id").asText(); String item=session.path("items").get(0).path("id").asText(); String path="/sessions/"+id+"/items/"+item+"/response";
        String key=UUID.randomUUID().toString(); var body=Map.of("answers",Map.of("g1","row"));
        var first=student.request("PUT",path,body,1,key); assertThat(first.statusCode()).isEqualTo(200);
        assertThat(tree(student.request("PUT",path,body,1,key))).isEqualTo(tree(first));
        assertThat(student.request("PUT",path,Map.of("answers",Map.of("g1","bad")),2,key).statusCode()).isEqualTo(409);
        assertThat(student.request("PUT",path,body,1,UUID.randomUUID().toString()).statusCode()).isEqualTo(409);
        assertThat(student.request("PUT",path,Map.of("answers",Map.of("unknown","x")),2,UUID.randomUUID().toString()).statusCode()).isEqualTo(422);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM simulator.response_events WHERE item_id=?",Integer.class,UUID.fromString(item))).isEqualTo(1);
        assertThat(student.request("POST","/sessions/"+id+"/submit",Map.of(),2,UUID.randomUUID().toString()).statusCode()).isEqualTo(200);
        assertThat(student.request("PUT",path,body,3,UUID.randomUUID().toString()).statusCode()).isEqualTo(409);
    }
    @Test void expirationCommitsAndLateAnswersNeverScore() throws Exception {
        var session=start(); String id=session.path("id").asText(); String item=session.path("items").get(0).path("id").asText();
        jdbc.update("UPDATE simulator.exam_sessions SET expires_at=now()-interval '1 second' WHERE id=?",UUID.fromString(id));
        var late=student.request("PUT","/sessions/"+id+"/items/"+item+"/response",Map.of("answers",Map.of("g1","row")),1,UUID.randomUUID().toString());
        assertThat(late.statusCode()).isEqualTo(410);
        assertThat(jdbc.queryForObject("SELECT state FROM simulator.exam_sessions WHERE id=?",String.class,UUID.fromString(id))).isEqualTo("EXPIRED");
        assertThat(tree(student.get("/sessions/"+id+"/results")).path("objectiveScore").path("correct").asInt()).isZero();
    }
    @Test void concurrentRetriesCreateOneSessionAndOneResponseRevision() throws Exception {
        var blueprint=tree(student.get("/blueprints")).get(0).path("id").asText(); var key=UUID.randomUUID().toString();
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<HttpResponse<String>> creation=()->student.request("POST","/sessions",Map.of("blueprintVersionId",blueprint,"mode","PRACTICE"),null,key);
            var created=pool.invokeAll(List.of(creation,creation)); var left=tree(created.get(0).get()); var right=tree(created.get(1).get());
            assertThat(left.path("id")).isEqualTo(right.path("id"));
            String id=left.path("id").asText(), item=left.path("items").get(0).path("id").asText(), saveKey=UUID.randomUUID().toString();
            Callable<HttpResponse<String>> save=()->student.request("PUT","/sessions/"+id+"/items/"+item+"/response",Map.of("answers",Map.of("g1","row")),1,saveKey);
            for(var future:pool.invokeAll(List.of(save,save))) assertThat(future.get().statusCode()).isEqualTo(200);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM simulator.response_events WHERE item_id=?",Integer.class,UUID.fromString(item))).isEqualTo(1);
        }
    }
    @Test void authenticationRequiresCsrfAndLogoutRevokesAccess() throws Exception {
        var anonymous=new Client(); assertThat(anonymous.get("/dashboard").statusCode()).isEqualTo(401);
        var unsafe=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/auth/login")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email",email,"password",password)))).build();
        assertThat(HttpClient.newHttpClient().send(unsafe,HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        assertThat(student.request("POST","/auth/logout",Map.of(),null,null).statusCode()).isEqualTo(204);
        assertThat(student.get("/dashboard").statusCode()).isEqualTo(401);
    }
    @Test void onlyApprovedContentCanEnterNewSessions() throws Exception {
        UUID question=UUID.randomUUID(), blueprint=UUID.randomUUID();
        jdbc.update("INSERT INTO simulator.question_versions(id,question_id,version,section,task_type,status,content) VALUES (?,?,1,'READING','READ_IN_DAILY_LIFE','DRAFT','{}')",question,UUID.randomUUID());
        jdbc.update("INSERT INTO simulator.blueprint_versions(id,name,version,duration_seconds,verification_status,question_version_ids) VALUES (?,'test draft',1,0,'CUSTOM_PRACTICE',?::jsonb)",blueprint,json.writeValueAsString(List.of(question)));
        try { assertThat(student.request("POST","/sessions",Map.of("blueprintVersionId",blueprint,"mode","PRACTICE"),null,UUID.randomUUID().toString()).statusCode()).isEqualTo(422); }
        finally { jdbc.update("DELETE FROM simulator.blueprint_versions WHERE id=?",blueprint); jdbc.update("DELETE FROM simulator.question_versions WHERE id=?",question); }
    }
    @Test void configuredServerDeadlineSurvivesResumeAndIgnoresClientDeadline() throws Exception {
        UUID blueprint=UUID.randomUUID(); testBlueprints.add(blueprint);
        jdbc.update("INSERT INTO simulator.blueprint_versions(id,name,version,duration_seconds,verification_status,question_version_ids) SELECT ?,'test timing',1,42,'CUSTOM_PRACTICE',question_version_ids FROM simulator.blueprint_versions WHERE name='Reading practice' ORDER BY version DESC LIMIT 1",blueprint);
        var created=student.request("POST","/sessions",Map.of("blueprintVersionId",blueprint,"mode","PRACTICE","expiresAt","2100-01-01T00:00:00Z"),null,UUID.randomUUID().toString());
        assertThat(created.statusCode()).isEqualTo(201); var session=tree(created);
        var started=java.time.Instant.parse(session.path("startedAt").asText()); var expires=java.time.Instant.parse(session.path("expiresAt").asText());
        assertThat(java.time.Duration.between(started,expires).getSeconds()).isEqualTo(42);
        var resumed=tree(student.get("/sessions/"+session.path("id").asText()));
        assertThat(resumed.path("expiresAt")).isEqualTo(session.path("expiresAt"));
        assertThat(resumed.path("startedAt")).isEqualTo(session.path("startedAt"));
    }
    JsonNode start() throws Exception {
        var blueprint=tree(student.get("/blueprints")).get(0).path("id").asText();
        var response=student.request("POST","/sessions",Map.of("blueprintVersionId",blueprint,"mode","PRACTICE"),null,UUID.randomUUID().toString()); assertThat(response.statusCode()).isEqualTo(201); return tree(response);
    }
    JsonNode tree(HttpResponse<String> response) throws Exception { return json.readTree(response.body()); }
    class Client {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrf;
        void login(String email,String password) throws Exception { csrf=tree(get("/auth/csrf")).path("token").asText(); assertThat(request("POST","/auth/login",Map.of("email",email,"password",password),null,null).statusCode()).isEqualTo(200); csrf=tree(get("/auth/csrf")).path("token").asText(); }
        HttpResponse<String> get(String path) throws Exception { return request("GET",path,null,null,null); }
        HttpResponse<String> request(String method,String path,Object body,Integer version,String key) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1"+path));
            if(!method.equals("GET")) builder.header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf==null?"":csrf);
            if(version!=null) builder.header("If-Match","\""+version+"\"");
            if(key!=null) builder.header("Idempotency-Key",key);
            builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            return http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        }
    }
}
