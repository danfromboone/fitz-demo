package dfb.demo;

import dfb.demo.domain.Author;
import dfb.demo.dto.AuthorDto;
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
 * Endpoint controller for {@code POST /authors}.
 */
public class CreateAuthorController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(CreateAuthorController.class);
    private static final Pattern PATTERN = Pattern.compile("(.*/)?authors/?");

    private final AuthorRepository authorRepository;
    private final RestResponses restResponses;

    public CreateAuthorController(AuthorRepository authorRepository, RestResponses restResponses) {
        this.authorRepository = Validate.nonNull(authorRepository, "authorRepository");
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
            AuthorDto dto = restResponses.fromJson(body, AuthorDto.class);
            if (dto.name() == null || dto.name().isBlank()) {
                return restResponses.badRequest("Author name must not be null or blank");
            }
            if (authorRepository.existsByName(dto.name())) {
                return restResponses.conflict("Author with name '" + dto.name() + "' already exists");
            }
            Author author = dto.toDomain();
            authorRepository.save(author);
            log.info("Created author: {}", author.name());
            return restResponses.created(AuthorDto.from(author));
        } catch (IllegalArgumentException e) {
            return restResponses.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse author payload", e);
            return restResponses.badRequest("Malformed JSON: " + e.getMessage());
        }
    }
}
