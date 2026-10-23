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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST controller for managing {@link Author} resources.
 * Supports:
 * <ul>
 *   <li>{@code GET /authors} - list all authors</li>
 *   <li>{@code GET /authors/{name}} - get author by name</li>
 *   <li>{@code POST /authors} - create a new author</li>
 *   <li>{@code DELETE /authors/{name}} - delete an author by name</li>
 * </ul>
 */
public class AuthorController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(AuthorController.class);

    private static final Pattern AUTHORS_COLLECTION = Pattern.compile("(.*/)?authors/?");
    private static final Pattern AUTHOR_ITEM = Pattern.compile("(.*/)?authors/(?<name>[^/]+)/?");

    private final AuthorRepository authorRepository;

    public AuthorController(AuthorRepository authorRepository) {
        this.authorRepository = Validate.nonNull(authorRepository, "authorRepository");
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        String path = request.getHttpUri().getPath();
        return AUTHORS_COLLECTION.matcher(path).matches() || AUTHOR_ITEM.matcher(path).matches();
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        String path = request.getHttpUri().getPath();
        HttpMethod method = request.getMethod();

        Matcher itemMatcher = AUTHOR_ITEM.matcher(path);
        if (itemMatcher.matches()) {
            String rawName = itemMatcher.group("name");
            String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
            return handleItem(method, name, request);
        }

        if (AUTHORS_COLLECTION.matcher(path).matches()) {
            return handleCollection(method, request);
        }

        return RestResponses.notFound("Resource not found");
    }

    private HttpResponse handleCollection(HttpMethod method, HttpRequest request) {
        if (HttpMethod.GET.equals(method)) {
            List<Author> authors = authorRepository.findAll();
            List<AuthorDto> dtos = authors.stream().map(AuthorDto::from).toList();
            return RestResponses.ok(dtos);
        }

        if (HttpMethod.POST.equals(method)) {
            try {
                String body = request.getBody().asString();
                if (body == null || body.isBlank()) {
                    return RestResponses.badRequest("Request body must not be empty");
                }
                AuthorDto dto = RestResponses.fromJson(body, AuthorDto.class);
                if (dto.name() == null || dto.name().isBlank()) {
                    return RestResponses.badRequest("Author name must not be null or blank");
                }
                if (authorRepository.existsByName(dto.name())) {
                    return RestResponses.conflict("Author with name '" + dto.name() + "' already exists");
                }
                Author author = dto.toDomain();
                authorRepository.save(author);
                log.info("Created author: {}", author.name());
                return RestResponses.created(AuthorDto.from(author));
            } catch (IllegalArgumentException e) {
                return RestResponses.badRequest(e.getMessage());
            } catch (Exception e) {
                log.error("Failed to parse author payload", e);
                return RestResponses.badRequest("Malformed JSON: " + e.getMessage());
            }
        }

        return RestResponses.methodNotAllowed("Method " + method + " not allowed on /authors");
    }

    private HttpResponse handleItem(HttpMethod method, String name, HttpRequest request) {
        if (HttpMethod.GET.equals(method)) {
            Optional<Author> authorOpt = authorRepository.findByName(name);
            return authorOpt.map(author -> RestResponses.ok(AuthorDto.from(author)))
                    .orElseGet(() -> RestResponses.notFound("Author not found for name: " + name));
        }

        if (HttpMethod.DELETE.equals(method)) {
            boolean deleted = authorRepository.deleteByName(name);
            if (deleted) {
                log.info("Deleted author: {}", name);
                return RestResponses.noContent();
            }
            return RestResponses.notFound("Author not found for name: " + name);
        }

        return RestResponses.methodNotAllowed("Method " + method + " not allowed on /authors/" + name);
    }
}
