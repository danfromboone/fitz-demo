package dfb.demo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dfb.fitz.Validate;
import dfb.fitz.http.HttpHeaders;
import dfb.fitz.http.HttpResponse;
import dfb.fitz.http.HttpResponseBody;
import dfb.fitz.http.HttpStatus;
import dfb.fitz.http.MimeType;

import java.util.Map;

/**
 * Service for serializing domain DTOs to HTTP JSON responses and deserializing JSON payloads.
 */
public class RestResponses {

    private final ObjectMapper objectMapper;
    private final ObjectSerializer objectSerializer;
    private final ObjectDeserializer objectDeserializer;

    /**
     * Injects the configured {@link ObjectMapper} via constructor.
     *
     * @param objectMapper the object mapper instance (must not be null)
     */
    public RestResponses(ObjectMapper objectMapper) {
        this.objectMapper = Validate.nonNull(objectMapper, "objectMapper");
        this.objectSerializer = new ObjectSerializer(this.objectMapper);
        this.objectDeserializer = new ObjectDeserializer(this.objectMapper);
    }

    public HttpResponse json(HttpStatus status, Object body) {
        try {
            String json = objectSerializer.serialize(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MimeType.APPLICATION_JSON_UTF8);
            return new HttpResponse(status, HttpResponseBody.of(json), headers);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize response to JSON", e);
        }
    }

    public HttpResponse ok(Object body) {
        return json(HttpStatus.OK, body);
    }

    public HttpResponse created(Object body) {
        return json(HttpStatus.CREATED, body);
    }

    public HttpResponse noContent() {
        return new HttpResponse(HttpStatus.NO_CONTENT);
    }

    public HttpResponse notFound(String message) {
        return json(HttpStatus.NOT_FOUND, Map.of("error", message));
    }

    public HttpResponse badRequest(String message) {
        return json(HttpStatus.BAD_REQUEST, Map.of("error", message));
    }

    public HttpResponse conflict(String message) {
        return json(HttpStatus.CONFLICT, Map.of("error", message));
    }

    public HttpResponse methodNotAllowed(String message) {
        return json(HttpStatus.METHOD_NOT_ALLOWED, Map.of("error", message));
    }

    public <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectDeserializer.deserialize(json, clazz);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
