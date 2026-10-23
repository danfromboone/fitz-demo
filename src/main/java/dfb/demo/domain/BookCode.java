package dfb.demo.domain;

import java.util.Objects;

/**
 * Value object representing a book's unique identifier/code (e.g. ISBN or catalog code).
 */
public record BookCode(String value) {

    public BookCode {
        Objects.requireNonNull(value, "Book code must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Book code must not be blank");
        }
    }

    public static BookCode of(String value) {
        return new BookCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
