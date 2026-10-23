package dfb.demo;

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
 * Endpoint controller for {@code DELETE /books/{code}}.
 */
public class DeleteBookController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(DeleteBookController.class);
    private static final Pattern PATTERN = Pattern.compile("(.*/)?books/(?<code>[^/]+)/?");

    private final BookRepository bookRepository;
    private final RestResponses restResponses;

    public DeleteBookController(BookRepository bookRepository, RestResponses restResponses) {
        this.bookRepository = Validate.nonNull(bookRepository, "bookRepository");
        this.restResponses = Validate.nonNull(restResponses, "restResponses");
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        return HttpMethod.DELETE.equals(request.getMethod())
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

        boolean deleted = bookRepository.deleteByCode(code);
        if (deleted) {
            log.info("Deleted book with code: {}", code);
            return restResponses.noContent();
        }
        return restResponses.notFound("Book not found for code: " + code);
    }
}
