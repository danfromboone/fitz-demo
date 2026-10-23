package dfb.demo;

import dfb.demo.domain.Book;
import dfb.demo.dto.BookDto;
import dfb.demo.web.RestResponses;
import dfb.fitz.Validate;
import dfb.fitz.http.HttpMethod;
import dfb.fitz.http.HttpRequest;
import dfb.fitz.http.HttpResponse;
import dfb.fitz.web.Controller;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Endpoint controller for {@code GET /books} (optionally filtered by {@code ?author=...}).
 */
public class GetBooksController implements Controller {
    private static final Pattern PATTERN = Pattern.compile("(.*/)?books/?");

    private final BookRepository bookRepository;
    private final RestResponses restResponses;

    public GetBooksController(BookRepository bookRepository, RestResponses restResponses) {
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
        String authorParam = request.getQueryParams().getFirst("author");
        List<Book> books;
        if (authorParam != null && !authorParam.isBlank()) {
            books = bookRepository.findByAuthor(authorParam.trim());
        } else {
            books = bookRepository.findAll();
        }
        List<BookDto> dtos = books.stream().map(BookDto::from).toList();
        return restResponses.ok(dtos);
    }
}
