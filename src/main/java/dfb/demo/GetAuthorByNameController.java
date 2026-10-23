package dfb.demo;

import dfb.demo.domain.Author;
import dfb.demo.dto.AuthorDto;
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
 * Endpoint controller for {@code GET /authors/{name}}.
 */
public class GetAuthorByNameController implements Controller {
    private static final Pattern PATTERN = Pattern.compile("(.*/)?authors/(?<name>[^/]+)/?");

    private final AuthorRepository authorRepository;
    private final RestResponses restResponses;

    public GetAuthorByNameController(AuthorRepository authorRepository, RestResponses restResponses) {
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
        Matcher matcher = PATTERN.matcher(request.getHttpUri().getPath());
        if (!matcher.matches()) {
            return restResponses.notFound("Not found");
        }
        String rawName = matcher.group("name");
        String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);

        Optional<Author> authorOpt = authorRepository.findByName(name);
        return authorOpt.map(author -> restResponses.ok(AuthorDto.from(author)))
                .orElseGet(() -> restResponses.notFound("Author not found for name: " + name));
    }
}
