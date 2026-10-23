package dfb.demo;

import dfb.demo.domain.Author;
import dfb.fitz.Validate;
import dfb.fitz.jdbc.Jdbc;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CRUD operations on {@link Author} entities.
 */
public class AuthorRepository {

    private final DataSource dataSource;

    public AuthorRepository(DataSource dataSource) {
        this.dataSource = Validate.nonNull(dataSource, "dataSource");
    }

    /**
     * Saves an author into the database if not already present.
     *
     * @param author the author to save
     * @return the saved author
     */
    public Author save(Author author) {
        Validate.nonNull(author, "author");
        String name = author.name();
        if (!existsByName(name)) {
            Jdbc.update(dataSource, "INSERT INTO authors (name) VALUES (?)", name);
        }
        return author;
    }

    /**
     * Finds an author by name.
     *
     * @param name the author name
     * @return optional containing the Author if found
     */
    public Optional<Author> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Jdbc.queryOne(
                dataSource,
                "SELECT name FROM authors WHERE name = ?",
                rs -> Author.of(rs.getString("name")),
                name.trim()
        );
    }

    /**
     * Returns all authors ordered alphabetically.
     *
     * @return list of all authors
     */
    public List<Author> findAll() {
        return Jdbc.query(
                dataSource,
                "SELECT name FROM authors ORDER BY name ASC",
                rs -> Author.of(rs.getString("name"))
        );
    }

    /**
     * Checks if an author with the given name exists.
     *
     * @param name the author name
     * @return true if exists, false otherwise
     */
    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        Integer count = Jdbc.queryOne(
                dataSource,
                "SELECT count(*) FROM authors WHERE name = ?",
                rs -> rs.getInt(1),
                name.trim()
        ).orElse(0);
        return count > 0;
    }

    /**
     * Deletes an author by name.
     *
     * @param name the author name
     * @return true if deleted, false if not found
     */
    public boolean deleteByName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        int rows = Jdbc.update(dataSource, "DELETE FROM authors WHERE name = ?", name.trim());
        return rows > 0;
    }

    /**
     * Returns the total count of authors in the database.
     *
     * @return total author count
     */
    public long count() {
        return Jdbc.queryOne(
                dataSource,
                "SELECT count(*) FROM authors",
                rs -> rs.getLong(1)
        ).orElse(0L);
    }
}
