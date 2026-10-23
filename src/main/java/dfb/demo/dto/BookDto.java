package dfb.demo.dto;

import dfb.demo.domain.Author;
import dfb.demo.domain.Book;

import java.util.List;

public record BookDto(String code, String title, List<String> authors) {

    public static BookDto from(Book book) {
        return new BookDto(
                book.code().value(),
                book.title().value(),
                book.authors().stream().map(Author::name).toList()
        );
    }

    public Book toDomain(String fallbackCode) {
        String resolvedCode = (code != null && !code.isBlank()) ? code : fallbackCode;
        String[] authorArray = (authors != null) ? authors.toArray(new String[0]) : new String[0];
        return Book.of(resolvedCode, title, authorArray);
    }
}
