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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Endpoint controller for {@code PUT /books/{code}}.
 */
public class UpdateBookController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(UpdateBookController.class);
    private static final Pattern PATTERN = Pattern.compile("(.*/)?books/(?<code>[^/]+)/?");

    private final BookRepository bookRepository;
    private final RestResponses restResponses;

    public UpdateBookController(BookRepository bookRepository, RestResponses restResponses) {
        this.bookRepository = Validate.nonNull(bookRepository, "bookRepository");
        this.restResponses = Validate.nonNull(restResponses, "restResponses");
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        return HttpMethod.PUT.equals(request.getMethod())
                && PATTERN.matcher(request.getHttpUri().getPath()).matches();
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        Matcher matcher = PATTERN.matcher(request.getHttpUri().getPath());
        if (!matcher.matches()) {
            return restResponses.notFound("Not found");
        }
        String rawCode = matcher.group("code");
        String code = URLDecoder.decode(rawCode, StandardCharsets.UTF_8);

        if (!bookRepository.existsByCode(code)) {
            return restResponses.notFound("Book not found for code: " + code);
        }

        try {
            String body = request.getBody().asString();
            if (body == null || body.isBlank()) {
                return restResponses.badRequest("Request body must not be empty");
            }
            BookDto dto = restResponses.fromJson(body, BookDto.class);
            Book book = dto.toDomain(code);
            bookRepository.save(book);
            log.info("Updated book with code: {}", code);
            return restResponses.ok(BookDto.from(book));
        } catch (IllegalArgumentException e) {
            return restResponses.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update book with code: {}", code, e);
            return restResponses.badRequest("Malformed JSON: " + e.getMessage());
        }
    }
}
