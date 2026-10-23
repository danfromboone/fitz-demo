package dfb.demo.domain;

import java.util.Objects;

/**
 * Value object representing a book's title.
 */
public record Title(String value) {

    public Title {
        Objects.requireNonNull(value, "Title must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
    }

    public static Title of(String value) {
        return new Title(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
