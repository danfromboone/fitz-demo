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
 * Endpoint controller for {@code DELETE /authors/{name}}.
 */
public class DeleteAuthorController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(DeleteAuthorController.class);
    private static final Pattern PATTERN = Pattern.compile("(.*/)?authors/(?<name>[^/]+)/?");

    private final AuthorRepository authorRepository;
    private final RestResponses restResponses;

    public DeleteAuthorController(AuthorRepository authorRepository, RestResponses restResponses) {
        this.authorRepository = Validate.nonNull(authorRepository, "authorRepository");
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
        String rawName = matcher.group("name");
        String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);

        boolean deleted = authorRepository.deleteByName(name);
        if (deleted) {
            log.info("Deleted author: {}", name);
            return restResponses.noContent();
        }
        return restResponses.notFound("Author not found for name: " + name);
    }
}
