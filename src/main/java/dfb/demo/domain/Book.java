package dfb.demo.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Domain entity/aggregate representing a Book.
 * A book has a title, a code (primary key), and 1 or more authors.
 */
public record Book(BookCode code, Title title, List<Author> authors) {

    public Book {
        Objects.requireNonNull(code, "Book code must not be null");
        Objects.requireNonNull(title, "Title must not be null");
        Objects.requireNonNull(authors, "Authors must not be null");
        if (authors.isEmpty()) {
            throw new IllegalArgumentException("A book must have 1 or more authors");
        }
        authors = List.copyOf(authors);
    }

    public static Book of(String code, String title, String... authorNames) {
        Objects.requireNonNull(authorNames, "Authors must not be null");
        List<Author> authorList = Arrays.stream(authorNames)
                .map(Author::of)
                .toList();
        return new Book(BookCode.of(code), Title.of(title), authorList);
    }

    public static Book of(BookCode code, Title title, Author... authors) {
        Objects.requireNonNull(authors, "Authors must not be null");
        return new Book(code, title, List.of(authors));
    }

    public static Book of(BookCode code, Title title, List<Author> authors) {
        return new Book(code, title, authors);
    }

    public Book withTitle(Title newTitle) {
        return new Book(this.code, newTitle, this.authors);
    }

    public Book withTitle(String newTitle) {
        return new Book(this.code, Title.of(newTitle), this.authors);
    }

    public Book withAuthors(List<Author> newAuthors) {
        return new Book(this.code, this.title, newAuthors);
    }

    public Book withAuthors(String... newAuthorNames) {
        Objects.requireNonNull(newAuthorNames, "Authors must not be null");
        List<Author> authorList = Arrays.stream(newAuthorNames)
                .map(Author::of)
                .toList();
        return new Book(this.code, this.title, authorList);
    }
}
