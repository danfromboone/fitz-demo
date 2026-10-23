package dfb.demo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dfb.fitz.Validate;

/**
 * Serializes Java objects to JSON strings using an underlying {@link ObjectMapper}.
 */
public class ObjectSerializer {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new {@code ObjectSerializer} with the specified object mapper.
     *
     * @param objectMapper the object mapper instance (must not be null)
     */
    public ObjectSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Validate.nonNull(objectMapper, "objectMapper");
    }

    /**
     * Serializes the given object to a JSON string.
     *
     * @param object the object to serialize
     * @return the serialized JSON string
     * @throws SerializationException if serialization fails
     */
    public String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize object to JSON", e);
        }
    }
}
