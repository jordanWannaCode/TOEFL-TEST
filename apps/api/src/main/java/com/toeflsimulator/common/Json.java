package com.toeflsimulator.common;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
@Component
public class Json {
    private final ObjectMapper mapper;
    public Json(ObjectMapper mapper) { this.mapper = mapper; }
    public JsonNode read(String value) { try { return mapper.readTree(value); } catch (Exception e) { throw new IllegalStateException("Invalid persisted JSON", e); } }
    public String write(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("Cannot serialize response", e); } }
    public JsonNode tree(Object value) { return mapper.valueToTree(value); }
}
