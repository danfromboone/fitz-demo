package dfb.demo;

import dfb.demo.domain.Book;
import dfb.demo.dto.BookDto;
import dfb.demo.web.RestResponses;
import dfb.fitz.Validate;
import dfb.fitz.http.HttpMethod;
import dfb.fitz.http.HttpRequest;
import dfb.fitz.http.HttpResponse;
import dfb.fitz.web.Controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Endpoint controller for {@code GET /books/{code}}.
 */
public class GetBookByCodeController implements Controller {
    private static final Pattern PATTERN = Pattern.compile("(.*/)?books/(?<code>[^/]+)/?");

    private final BookRepository bookRepository;
    private final RestResponses restResponses;

    public GetBookByCodeController(BookRepository bookRepository, RestResponses restResponses) {
        this.bookRepository = Validate.nonNull(bookRepository, "bookRepository");
        this.restResponses = Validate.nonNull(restResponses, "restResponses");
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        return HttpMethod.GET.equals(request.getMethod())
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

        Optional<Book> bookOpt = bookRepository.findByCode(code);
        return bookOpt.map(book -> restResponses.ok(BookDto.from(book)))
                .orElseGet(() -> restResponses.notFound("Book not found for code: " + code));
    }
}
