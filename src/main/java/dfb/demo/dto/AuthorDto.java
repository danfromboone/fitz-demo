package dfb.demo.dto;

import dfb.demo.domain.Author;

public record AuthorDto(String name) {

    public static AuthorDto from(Author author) {
        return new AuthorDto(author.name());
    }

    public Author toDomain() {
        return Author.of(name);
    }
}
