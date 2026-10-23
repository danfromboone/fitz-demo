package dfb.demo.web;

/**
 * Alias for {@link SerializationException} to support alternate spelling.
 */
public class SerialiazationException extends SerializationException {

    public SerialiazationException(String message) {
        super(message);
    }

    public SerialiazationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerialiazationException(Throwable cause) {
        super(cause);
    }
}
