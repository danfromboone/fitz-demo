package dfb.demo.domain;

import java.util.Objects;

/**
 * Value object representing an author.
 */
public record Author(String name) {

    public Author {
        Objects.requireNonNull(name, "Author name must not be null");
        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Author name must not be blank");
        }
    }

    public static Author of(String name) {
        return new Author(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
