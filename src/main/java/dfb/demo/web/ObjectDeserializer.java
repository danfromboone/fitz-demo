package dfb.demo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dfb.fitz.Validate;

/**
 * Deserializes JSON strings to Java objects using an underlying {@link ObjectMapper}.
 */
public class ObjectDeserializer {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new {@code ObjectDeserializer} using a default {@link ObjectMapper}.
     */
    public ObjectDeserializer() {
        this(new ObjectMapper());
    }

    /**
     * Creates a new {@code ObjectDeserializer} with the specified object mapper.
     *
     * @param objectMapper the object mapper instance (must not be null)
     */
    public ObjectDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = Validate.nonNull(objectMapper, "objectMapper");
    }

    /**
     * Deserializes the given JSON string to an instance of the specified class.
     *
     * @param <T>   the target type
     * @param json  the JSON string to deserialize
     * @param clazz the target class
     * @return the deserialized object
     * @throws DeserializationException if parsing or deserialization fails
     */
    public <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new DeserializationException("Failed to deserialize JSON to " + (clazz != null ? clazz.getSimpleName() : "null"), e);
        }
    }
}
