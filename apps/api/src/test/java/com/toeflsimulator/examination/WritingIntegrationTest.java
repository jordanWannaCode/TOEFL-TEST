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
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={"server.servlet.session.cookie.secure=false","AI_INTERNAL_TOKEN=integration-test-only-token","WRITING_WORKER_DELAY_MS=3600000"})
class WritingIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired com.toeflsimulator.content.ListeningImporter importer;
    @Autowired org.springframework.core.env.Environment environment;
    UUID owner, other; Client student, stranger; String email,password;
    List<UUID> testBlueprints=new ArrayList<>();
    @BeforeEach void setup() throws Exception {
        owner=UUID.randomUUID(); other=UUID.randomUUID(); email=owner+"@example.test"; password=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO simulator.users(id,email,password_hash) VALUES (?,?,?),(?,?,?)",owner,email,encoder.encode(password),other,other+"@example.test",encoder.encode(password));
        student=new Client(); student.login(email,password); stranger=new Client(); stranger.login(other+"@example.test",password);
    }
    @AfterEach void cleanup() { jdbc.update("DELETE FROM simulator.users WHERE id IN (?,?)",owner,other); testBlueprints.forEach(id->jdbc.update("DELETE FROM simulator.blueprint_versions WHERE id=?",id)); }
    @Autowired com.toeflsimulator.evaluation.internal.WritingWorker worker;
    @Autowired ExamSessions sessions;
    static final java.util.concurrent.atomic.AtomicInteger providerStatus=new java.util.concurrent.atomic.AtomicInteger(200);
    static final com.sun.net.httpserver.HttpServer provider;
    static {
        try {
            provider=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
            provider.createContext("/api/v1/ai/evaluations/writing",exchange->{
                try {
                    var mapper=new ObjectMapper(); var body=mapper.readTree(exchange.getRequestBody());
                    var criteria=new ArrayList<Object>();
                    for(var c:body.path("rubric").path("criteria")) criteria.add(Map.of("id",c.path("id").asText(),"score",3,"strengths",List.of("Clear request"),"weaknesses",List.of(),"evidence",List.of(body.path("response").asText()),"improvements",List.of("Support the request")));
                    var feedback=Map.of("estimated",true,"internalScore",3,"scoreMax",5,"proficiencyLevel","TEST_ONLY","criteria",criteria,"findings",List.of(),"recommendations",List.of(),"improvedResponse",body.path("response").asText(),"flags",List.of("TEST_ONLY"));
                    String text=mapper.writeValueAsString(Map.of("evaluationId",body.path("evaluationId").asText(),"rubricVersion",body.path("rubric").path("id").asText(),"promptVersion","writing-feedback-v1","feedback",feedback,"provider","test","model","test-only","usage",Map.of("inputTokens",0)));
                    exchange.sendResponseHeaders(providerStatus.get(),text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);exchange.getResponseBody().write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));exchange.close();
                } catch(Exception e) {exchange.close();}
            });provider.start();
        } catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    @org.springframework.test.context.DynamicPropertySource static void config(org.springframework.test.context.DynamicPropertyRegistry registry) { registry.add("AI_SERVICE_URL",()->"http://127.0.0.1:"+provider.getAddress().getPort()); }
    @Test void sentenceVariantsScoreDeterministicallyAndKeysStayPrivate() throws Exception {
        var s=startWriting("BUILD_A_SENTENCE");String id=s.path("id").asText(),item=s.path("items").get(0).path("id").asText();
        assertThat(s.toString()).doesNotContain("\"variants\"","explanation","correctAnswers");
        assertThat(student.get("/sessions/"+id+"/results").statusCode()).isEqualTo(409);
        assertThat(student.request("PUT","/sessions/"+id+"/items/"+item+"/response",Map.of("elementIds",List.of("a","a")),1,UUID.randomUUID().toString()).statusCode()).isEqualTo(422);
        saveWriting(s,Map.of("elementIds",List.of("a","c","b")));closeWriting(s);
        var report=tree(student.get("/sessions/"+id+"/results"));assertThat(report.path("items").get(0).path("objectiveScore").path("correct").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM simulator.evaluation_jobs WHERE item_id=?",Integer.class,UUID.fromString(item))).isZero();
    }
    @Test void draftsJobsRetryPersistenceAndOwnershipWork() throws Exception {
        providerStatus.set(502);var s=startWriting("WRITE_AN_EMAIL");String id=s.path("id").asText(),item=s.path("items").get(0).path("id").asText();
        String text="Could we reserve a quiet room on Thursday? Friday would also work.";
        saveWriting(s,Map.of("text",text));var resumed=tree(student.get("/sessions/"+id));assertThat(resumed.path("items").get(0).path("response").path("text").asText()).isEqualTo(text);
        assertThat(stranger.get("/sessions/"+id).statusCode()).isEqualTo(404);assertThat(stranger.request("POST","/sessions/"+id+"/evaluations/retry",Map.of(),null,null).statusCode()).isEqualTo(404);
        closeWriting(s);worker.run();
        var failed=tree(student.get("/sessions/"+id+"/results"));assertThat(failed.path("items").get(0).path("status").asText()).isEqualTo("PENDING");
        jdbc.update("UPDATE simulator.evaluation_jobs SET next_attempt_at=now()-interval '1 second' WHERE item_id=?",UUID.fromString(item));providerStatus.set(200);worker.run();worker.run();
        var report=tree(student.get("/sessions/"+id+"/results"));assertThat(report.path("items").get(0).path("status").asText()).isEqualTo("COMPLETED");assertThat(report.path("items").get(0).path("feedback").path("internalScore").asInt()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM simulator.writing_evaluations e JOIN simulator.evaluation_jobs j ON j.id=e.job_id WHERE j.item_id=?",Integer.class,UUID.fromString(item))).isEqualTo(1);
        assertThat(stranger.get("/sessions/"+id+"/results").statusCode()).isEqualTo(404);
        assertThat(tree(student.get("/dashboard")).path("writingProgress").path("byTaskType").get(0).path("evaluatedCount").asInt()).isEqualTo(1);
    }
    @Test void serverExpirationClosesDraftAndCreatesOnlyOneJob() throws Exception {
        var s=startWriting("WRITE_FOR_AN_ACADEMIC_DISCUSSION");String id=s.path("id").asText(),item=s.path("items").get(0).path("id").asText();saveWriting(s,Map.of("text","Repairing items can reduce waste and teach useful skills."));
        jdbc.update("UPDATE simulator.exam_sessions SET expires_at=now()-interval '1 second' WHERE id=?",UUID.fromString(id));sessions.expireDueSessions();sessions.expireDueSessions();
        assertThat(tree(student.get("/sessions/"+id)).path("state").asText()).isEqualTo("EXPIRED");
        assertThat(student.request("PUT","/sessions/"+id+"/items/"+item+"/response",Map.of("text","late"),2,UUID.randomUUID().toString()).statusCode()).isEqualTo(410);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM simulator.evaluation_jobs WHERE item_id=?",Integer.class,UUID.fromString(item))).isEqualTo(1);
    }
    @Test void unavailableAndExpiredWorkerLeasesCanRecover() throws Exception {
        providerStatus.set(503);var s=startWriting("WRITE_AN_EMAIL");String item=s.path("items").get(0).path("id").asText(),id=s.path("id").asText();saveWriting(s,Map.of("text","Could you please reserve a room?"));closeWriting(s);worker.run();
        assertThat(tree(student.get("/sessions/"+id+"/results")).path("items").get(0).path("status").asText()).isEqualTo("UNAVAILABLE");
        assertThat(student.request("POST","/sessions/"+id+"/evaluations/retry",Map.of(),null,null).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE simulator.evaluation_jobs SET status='PROCESSING',lease_until=now()-interval '1 second' WHERE item_id=?",UUID.fromString(item));providerStatus.set(200);worker.run();
        assertThat(tree(student.get("/sessions/"+id+"/results")).path("items").get(0).path("status").asText()).isEqualTo("COMPLETED");
    }
    @Test void writingCatalogApprovedAndInputLimited() throws Exception {
        var catalog=tree(student.get("/writing/exercises"));assertThat(catalog.size()).isEqualTo(3);assertThat(catalog.toString()).doesNotContain("\"variants\"","correctAnswers");var s=startWriting("WRITE_AN_EMAIL");
        assertThat(student.request("PUT","/sessions/"+s.path("id").asText()+"/items/"+s.path("items").get(0).path("id").asText()+"/response",Map.of("text","x".repeat(12001)),1,UUID.randomUUID().toString()).statusCode()).isEqualTo(422);
    }
    JsonNode startWriting(String task) throws Exception {
        for(var exercise:tree(student.get("/writing/exercises"))) if(exercise.path("content").path("taskType").asText().equals(task)) {
            var response=student.request("POST","/sessions",Map.of("blueprintVersionId",exercise.path("blueprintVersionId").asText(),"mode","PRACTICE"),null,UUID.randomUUID().toString());assertThat(response.statusCode()).isEqualTo(201);return tree(response);
        }
        throw new IllegalStateException("Missing exercise");
    }
    void saveWriting(JsonNode s,Object response) throws Exception {assertThat(student.request("PUT","/sessions/"+s.path("id").asText()+"/items/"+s.path("items").get(0).path("id").asText()+"/response",response,1,UUID.randomUUID().toString()).statusCode()).isEqualTo(200);}
    void closeWriting(JsonNode s) throws Exception {String key=UUID.randomUUID().toString();for(int i=0;i<2;i++)assertThat(student.request("POST","/sessions/"+s.path("id").asText()+"/submit",Map.of(),2,key).statusCode()).isEqualTo(200);}
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
