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

import java.util.regex.Pattern;

/**
 * Endpoint controller for {@code POST /books}.
 */
public class CreateBookController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(CreateBookController.class);
    private static final Pattern PATTERN = Pattern.compile("(.*/)?books/?");

    private final BookRepository bookRepository;
    private final RestResponses restResponses;

    public CreateBookController(BookRepository bookRepository, RestResponses restResponses) {
        this.bookRepository = Validate.nonNull(bookRepository, "bookRepository");
        this.restResponses = Validate.nonNull(restResponses, "restResponses");
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        return HttpMethod.POST.equals(request.getMethod())
                && PATTERN.matcher(request.getHttpUri().getPath()).matches();
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        try {
            String body = request.getBody().asString();
            if (body == null || body.isBlank()) {
                return restResponses.badRequest("Request body must not be empty");
            }
            BookDto dto = restResponses.fromJson(body, BookDto.class);
            if (dto.code() == null || dto.code().isBlank()) {
                return restResponses.badRequest("Book code must not be null or blank");
            }
            if (bookRepository.existsByCode(dto.code())) {
                return restResponses.conflict("Book with code '" + dto.code() + "' already exists");
            }
            Book book = dto.toDomain(null);
            bookRepository.save(book);
            log.info("Created book with code: {}", book.code());
            return restResponses.created(BookDto.from(book));
        } catch (IllegalArgumentException e) {
            return restResponses.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse book payload", e);
            return restResponses.badRequest("Malformed JSON: " + e.getMessage());
        }
    }
}
