package dfb.demo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dfb.fitz.http.HttpHeaders;
import dfb.fitz.http.HttpResponse;
import dfb.fitz.http.HttpResponseBody;
import dfb.fitz.http.HttpStatus;
import dfb.fitz.http.MimeType;

import java.util.Map;

public final class RestResponses {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RestResponses() {
    }

    public static HttpResponse json(HttpStatus status, Object body) {
        try {
            String json = MAPPER.writeValueAsString(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MimeType.APPLICATION_JSON_UTF8);
            return new HttpResponse(status, HttpResponseBody.of(json), headers);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize response to JSON", e);
        }
    }

    public static HttpResponse ok(Object body) {
        return json(HttpStatus.OK, body);
    }

    public static HttpResponse created(Object body) {
        return json(HttpStatus.CREATED, body);
    }

    public static HttpResponse noContent() {
        return new HttpResponse(HttpStatus.NO_CONTENT);
    }

    public static HttpResponse notFound(String message) {
        return json(HttpStatus.NOT_FOUND, Map.of("error", message));
    }

    public static HttpResponse badRequest(String message) {
        return json(HttpStatus.BAD_REQUEST, Map.of("error", message));
    }

    public static HttpResponse conflict(String message) {
        return json(HttpStatus.CONFLICT, Map.of("error", message));
    }

    public static HttpResponse methodNotAllowed(String message) {
        return json(HttpStatus.METHOD_NOT_ALLOWED, Map.of("error", message));
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return MAPPER.readValue(json, clazz);
    }
}
