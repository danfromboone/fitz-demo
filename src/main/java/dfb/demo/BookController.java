package dfb.demo;

import dfb.demo.domain.Book;
import dfb.demo.dto.BookDto;
import dfb.demo.web.RestResponses;
import dfb.fitz.Validate;
import dfb.fitz.http.HttpMethod;
import dfb.fitz.http.HttpRequest;
import dfb.fitz.http.HttpResponse;
import dfb.fitz.web.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST controller for managing {@link Book} resources.
 * Supports:
 * <ul>
 *   <li>{@code GET /books} - list all books (optional {@code ?author=...} filter)</li>
 *   <li>{@code GET /books/{code}} - get book by code</li>
 *   <li>{@code POST /books} - create a new book</li>
 *   <li>{@code PUT /books/{code}} - update an existing book</li>
 *   <li>{@code DELETE /books/{code}} - delete a book by code</li>
 * </ul>
 */
public class BookController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private static final Pattern BOOKS_COLLECTION = Pattern.compile("(.*/)?books/?");
    private static final Pattern BOOK_ITEM = Pattern.compile("(.*/)?books/(?<code>[^/]+)/?");

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = Validate.nonNull(bookRepository, "bookRepository");
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        String path = request.getHttpUri().getPath();
        return BOOKS_COLLECTION.matcher(path).matches() || BOOK_ITEM.matcher(path).matches();
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        String path = request.getHttpUri().getPath();
        HttpMethod method = request.getMethod();

        Matcher itemMatcher = BOOK_ITEM.matcher(path);
        if (itemMatcher.matches()) {
            String rawCode = itemMatcher.group("code");
            String code = URLDecoder.decode(rawCode, StandardCharsets.UTF_8);
            return handleItem(method, code, request);
        }

        if (BOOKS_COLLECTION.matcher(path).matches()) {
            return handleCollection(method, request);
        }

        return RestResponses.notFound("Resource not found");
    }

    private HttpResponse handleCollection(HttpMethod method, HttpRequest request) {
        if (HttpMethod.GET.equals(method)) {
            String authorParam = request.getQueryParams().getFirst("author");
            List<Book> books;
            if (authorParam != null && !authorParam.isBlank()) {
                books = bookRepository.findByAuthor(authorParam.trim());
            } else {
                books = bookRepository.findAll();
            }
            List<BookDto> dts = books.stream().map(BookDto::from).toList();
            return RestResponses.ok(dts);
        }

        if (HttpMethod.POST.equals(method)) {
            try {
                String body = request.getBody().asString();
                if (body == null || body.isBlank()) {
                    return RestResponses.badRequest("Request body must not be empty");
                }
                BookDto dto = RestResponses.fromJson(body, BookDto.class);
                if (dto.code() == null || dto.code().isBlank()) {
                    return RestResponses.badRequest("Book code must not be null or blank");
                }
                if (bookRepository.existsByCode(dto.code())) {
                    return RestResponses.conflict("Book with code '" + dto.code() + "' already exists");
                }
                Book book = dto.toDomain(null);
                bookRepository.save(book);
                log.info("Created book with code: {}", book.code());
                return RestResponses.created(BookDto.from(book));
            } catch (IllegalArgumentException e) {
                return RestResponses.badRequest(e.getMessage());
            } catch (Exception e) {
                log.error("Failed to parse book payload", e);
                return RestResponses.badRequest("Malformed JSON: " + e.getMessage());
            }
        }

        return RestResponses.methodNotAllowed("Method " + method + " not allowed on /books");
    }

    private HttpResponse handleItem(HttpMethod method, String code, HttpRequest request) {
        if (HttpMethod.GET.equals(method)) {
            Optional<Book> bookOpt = bookRepository.findByCode(code);
            return bookOpt.map(book -> RestResponses.ok(BookDto.from(book)))
                    .orElseGet(() -> RestResponses.notFound("Book not found for code: " + code));
        }

        if (HttpMethod.PUT.equals(method)) {
            if (!bookRepository.existsByCode(code)) {
                return RestResponses.notFound("Book not found for code: " + code);
            }
            try {
                String body = request.getBody().asString();
                if (body == null || body.isBlank()) {
                    return RestResponses.badRequest("Request body must not be empty");
                }
                BookDto dto = RestResponses.fromJson(body, BookDto.class);
                Book book = dto.toDomain(code);
                bookRepository.save(book);
                log.info("Updated book with code: {}", code);
                return RestResponses.ok(BookDto.from(book));
            } catch (IllegalArgumentException e) {
                return RestResponses.badRequest(e.getMessage());
            } catch (Exception e) {
                log.error("Failed to update book with code: {}", code, e);
                return RestResponses.badRequest("Malformed JSON: " + e.getMessage());
            }
        }

        if (HttpMethod.DELETE.equals(method)) {
            boolean deleted = bookRepository.deleteByCode(code);
            if (deleted) {
                log.info("Deleted book with code: {}", code);
                return RestResponses.noContent();
            }
            return RestResponses.notFound("Book not found for code: " + code);
        }

        return RestResponses.methodNotAllowed("Method " + method + " not allowed on /books/" + code);
    }
}
