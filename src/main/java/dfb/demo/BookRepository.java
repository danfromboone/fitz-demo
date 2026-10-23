package dfb.demo;

import dfb.demo.domain.Author;
import dfb.demo.domain.Book;
import dfb.demo.domain.BookCode;
import dfb.demo.domain.Title;
import dfb.fitz.Validate;
import dfb.fitz.jdbc.Jdbc;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for CRUD operations on {@link Book} aggregates.
 */
public class BookRepository {

    private final DataSource dataSource;
    private final AuthorRepository authorRepository;

    public BookRepository(DataSource dataSource) {
        this(dataSource, new AuthorRepository(dataSource));
    }

    public BookRepository(DataSource dataSource, AuthorRepository authorRepository) {
        this.dataSource = Validate.nonNull(dataSource, "dataSource");
        this.authorRepository = Validate.nonNull(authorRepository, "authorRepository");
    }

    /**
     * Saves a book (inserting or updating) along with its authors in a single transaction.
     *
     * @param book the book to save
     * @return the saved book
     */
    public Book save(Book book) {
        Validate.nonNull(book, "book");

        return Jdbc.inTransaction(dataSource, () -> {
            // 1. Ensure all authors exist in the authors table
            for (Author author : book.authors()) {
                authorRepository.save(author);
            }

            // 2. Insert or update the book row
            String code = book.code().value();
            String title = book.title().value();
            if (existsByCode(book.code())) {
                Jdbc.update(dataSource, "UPDATE books SET title = ? WHERE code = ?", title, code);
            } else {
                Jdbc.update(dataSource, "INSERT INTO books (code, title) VALUES (?, ?)", code, title);
            }

            // 3. Replace author association rows
            Jdbc.update(dataSource, "DELETE FROM book_authors WHERE book_code = ?", code);
            for (int i = 0; i < book.authors().size(); i++) {
                Author author = book.authors().get(i);
                Jdbc.update(
                        dataSource,
                        "INSERT INTO book_authors (book_code, author_name, author_order) VALUES (?, ?, ?)",
                        code,
                        author.name(),
                        i
                );
            }

            return book;
        });
    }

    /**
     * Finds a book by its {@link BookCode}.
     *
     * @param code the book code
     * @return optional containing the book if found
     */
    public Optional<Book> findByCode(BookCode code) {
        if (code == null) {
            return Optional.empty();
        }
        return findByCode(code.value());
    }

    /**
     * Finds a book by its code string.
     *
     * @param code the book code string
     * @return optional containing the book if found
     */
    public Optional<Book> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String cleanCode = code.trim();
        Optional<String> titleOpt = Jdbc.queryOne(
                dataSource,
                "SELECT title FROM books WHERE code = ?",
                rs -> rs.getString("title"),
                cleanCode
        );
        if (titleOpt.isEmpty()) {
            return Optional.empty();
        }

        List<Author> authors = Jdbc.query(
                dataSource,
                "SELECT author_name FROM book_authors WHERE book_code = ? ORDER BY author_order ASC",
                rs -> Author.of(rs.getString("author_name")),
                cleanCode
        );

        return Optional.of(new Book(BookCode.of(cleanCode), Title.of(titleOpt.get()), authors));
    }

    /**
     * Retrieves all books along with their authors.
     *
     * @return list of all books
     */
    public List<Book> findAll() {
        Map<String, String> bookTitles = new LinkedHashMap<>();
        Map<String, List<Author>> bookAuthors = new LinkedHashMap<>();

        Jdbc.query(
                dataSource,
                "SELECT b.code, b.title, ba.author_name, ba.author_order " +
                        "FROM books b " +
                        "LEFT JOIN book_authors ba ON b.code = ba.book_code " +
                        "ORDER BY b.code ASC, ba.author_order ASC",
                rs -> {
                    String code = rs.getString("code");
                    String title = rs.getString("title");
                    String authorName = rs.getString("author_name");

                    bookTitles.putIfAbsent(code, title);
                    bookAuthors.computeIfAbsent(code, k -> new ArrayList<>());
                    if (authorName != null) {
                        bookAuthors.get(code).add(Author.of(authorName));
                    }
                    return null;
                }
        );

        List<Book> books = new ArrayList<>(bookTitles.size());
        for (Map.Entry<String, String> entry : bookTitles.entrySet()) {
            String code = entry.getKey();
            String title = entry.getValue();
            List<Author> authors = bookAuthors.getOrDefault(code, List.of());
            if (!authors.isEmpty()) {
                books.add(new Book(BookCode.of(code), Title.of(title), authors));
            }
        }
        return books;
    }

    /**
     * Finds all books by an author's name.
     *
     * @param authorName the author name
     * @return list of books associated with that author
     */
    public List<Book> findByAuthor(String authorName) {
        if (authorName == null || authorName.isBlank()) {
            return List.of();
        }
        List<String> codes = Jdbc.query(
                dataSource,
                "SELECT DISTINCT book_code FROM book_authors WHERE author_name = ? ORDER BY book_code ASC",
                rs -> rs.getString("book_code"),
                authorName.trim()
        );
        List<Book> results = new ArrayList<>(codes.size());
        for (String code : codes) {
            findByCode(code).ifPresent(results::add);
        }
        return results;
    }

    /**
     * Checks if a book exists for the given {@link BookCode}.
     *
     * @param code the book code
     * @return true if exists, false otherwise
     */
    public boolean existsByCode(BookCode code) {
        if (code == null) {
            return false;
        }
        return existsByCode(code.value());
    }

    /**
     * Checks if a book exists for the given code string.
     *
     * @param code the book code string
     * @return true if exists, false otherwise
     */
    public boolean existsByCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        Integer count = Jdbc.queryOne(
                dataSource,
                "SELECT count(*) FROM books WHERE code = ?",
                rs -> rs.getInt(1),
                code.trim()
        ).orElse(0);
        return count > 0;
    }

    /**
     * Deletes a book by its {@link BookCode}.
     *
     * @param code the book code
     * @return true if deleted, false if not found
     */
    public boolean deleteByCode(BookCode code) {
        if (code == null) {
            return false;
        }
        return deleteByCode(code.value());
    }

    /**
     * Deletes a book by its code string.
     *
     * @param code the book code string
     * @return true if deleted, false if not found
     */
    public boolean deleteByCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return Jdbc.inTransaction(dataSource, () -> {
            String cleanCode = code.trim();
            Jdbc.update(dataSource, "DELETE FROM book_authors WHERE book_code = ?", cleanCode);
            int rows = Jdbc.update(dataSource, "DELETE FROM books WHERE code = ?", cleanCode);
            return rows > 0;
        });
    }

    /**
     * Returns the total count of books in the database.
     *
     * @return total count of books
     */
    public long count() {
        return Jdbc.queryOne(
                dataSource,
                "SELECT count(*) FROM books",
                rs -> rs.getLong(1)
        ).orElse(0L);
    }
}
