package dfb.demo;

import dfb.demo.domain.Author;
import dfb.demo.dto.AuthorDto;
import dfb.demo.web.RestResponses;
import dfb.fitz.Validate;
import dfb.fitz.http.HttpMethod;
import dfb.fitz.http.HttpRequest;
import dfb.fitz.http.HttpResponse;
import dfb.fitz.web.Controller;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Endpoint controller for {@code GET /authors}.
 */
public class GetAuthorsController implements Controller {
    private static final Pattern PATTERN = Pattern.compile("(.*/)?authors/?");

    private final AuthorRepository authorRepository;
    private final RestResponses restResponses;

    public GetAuthorsController(AuthorRepository authorRepository, RestResponses restResponses) {
        this.authorRepository = Validate.nonNull(authorRepository, "authorRepository");
        this.restResponses = Validate.nonNull(restResponses, "restResponses");
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        return HttpMethod.GET.equals(request.getMethod())
                && PATTERN.matcher(request.getHttpUri().getPath()).matches();
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        List<Author> authors = authorRepository.findAll();
        List<AuthorDto> dtos = authors.stream().map(AuthorDto::from).toList();
        return restResponses.ok(dtos);
    }
}
