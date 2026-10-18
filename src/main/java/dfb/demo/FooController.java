package dfb.demo;

import dfb.fitz.web.Controller;
import dfb.fitz.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class FooController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(FooController.class);
    private static final Pattern FOO_PATTERN = Pattern.compile("/foo");


    private final FooService fooService;

    public FooController(FooService fooService){
        this.fooService = fooService;
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        return request != null
                && request.getHttpUri().matches(FOO_PATTERN)
                && HttpMethod.GET.equals(request.getMethod());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpQueryParams queryParams = request.getQueryParams();
        log.info("Handling /foo request with queryParams={}", queryParams.asMap());
        String message = "Request params: " + queryParams.asMap();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MimeType.TEXT_PLAIN_UTF8);

        return new HttpResponse(HttpStatus.OK, HttpResponseBody.of(message), headers);
    }
}
