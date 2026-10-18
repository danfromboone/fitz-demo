package dfb.demo

import dfb.fitz.web.Controller
import dfb.fitz.web.DispatcherServlet
import dfb.fitz.http.HttpRequest
import dfb.fitz.http.HttpResponse
import dfb.fitz.http.HttpStatus
import dfb.demo.ApplicationContext
import dfb.demo.FooController
import dfb.demo.FooService
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class DispatcherServletIntegrationSpec extends Specification {

    def "ApplicationContext creates DispatcherServlet with its configured controllers"() {
        given:
        def applicationContext = new ApplicationContext()
        def dispatcherServlet = applicationContext.dispatcherServlet
        def outputBuffer = new ByteArrayOutputStream()
        def servletOutputStream = new CapturingServletOutputStream(outputBuffer)

        def request = Mock(HttpServletRequest) {
            getMethod() >> "GET"
            getRequestURL() >> new StringBuffer("http://localhost:8080/foo")
            getQueryString() >> "user=alice"
        }
        def response = Mock(HttpServletResponse) {
            getOutputStream() >> servletOutputStream
        }

        when:
        dispatcherServlet.init()
        dispatcherServlet.service(request, response)

        then:
        dispatcherServlet.controllers.size() == 1
        1 * response.setStatus(200)
        outputBuffer.toString(StandardCharsets.UTF_8) == "Request params: {user=[alice]}"
    }

    def "dispatches /foo request to FooController and writes decoded request params to response"() {
        given:
        def dispatcherServlet = new DispatcherServlet([new FooController(Mock(FooService))] as Set)
        def outputBuffer = new ByteArrayOutputStream()
        def servletOutputStream = new CapturingServletOutputStream(outputBuffer)

        def request = Mock(HttpServletRequest) {
            getMethod() >> "GET"
            getRequestURL() >> new StringBuffer("http://localhost:8080/foo")
            getQueryString() >> "name=John%20Doe&role=admin&role=user&flag"
            getHeaderNames() >> Collections.enumeration(["Accept"])
            getHeaders("Accept") >> Collections.enumeration(["text/plain"])
        }
        def response = Mock(HttpServletResponse) {
            getOutputStream() >> servletOutputStream
        }

        when:
        dispatcherServlet.service(request, response)

        then:
        1 * response.setStatus(200)
        1 * response.addHeader("Content-Type", "text/plain; charset=UTF-8")
        outputBuffer.toString(StandardCharsets.UTF_8) ==
                "Request params: [name:[John Doe], role:[admin, user], flag:[]]" ||
                outputBuffer.toString(StandardCharsets.UTF_8) ==
                "Request params: {name=[John Doe], role=[admin, user], flag=[]}"
    }

    def "returns 404 Not Found when no controller matches the request URI"() {
        given:
        def dispatcherServlet = new DispatcherServlet([new FooController(Mock(FooService))] as Set)
        def request = Mock(HttpServletRequest) {
            getMethod() >> "GET"
            getRequestURL() >> new StringBuffer("http://localhost:8080/demo/unknown")
            getQueryString() >> "a=1"
        }
        def response = Mock(HttpServletResponse)

        when:
        dispatcherServlet.service(request, response)

        then:
        1 * response.setStatus(404)
        0 * response.getOutputStream()
    }

    def "iterates through controllers in order and delegates to the first matching controller"() {
        given:
        def barController = new Controller() {
            @Override
            boolean canHandle(HttpRequest req) {
                return req.httpUri.matches(".*/bar")
            }

            @Override
            HttpResponse handle(HttpRequest req) {
                return new HttpResponse(HttpStatus.CREATED, "Handled by bar: " + req.body.asString())
            }
        }
        def dispatcherServlet = new DispatcherServlet([new FooController(Mock(FooService)), barController] as Set)
        def outputBuffer = new ByteArrayOutputStream()
        def servletOutputStream = new CapturingServletOutputStream(outputBuffer)

        def request = Mock(HttpServletRequest) {
            getMethod() >> "POST"
            getRequestURL() >> new StringBuffer("http://localhost:8080/demo/bar")
            getReader() >> new BufferedReader(new StringReader("payload-data"))
        }
        def response = Mock(HttpServletResponse) {
            getOutputStream() >> servletOutputStream
        }

        when:
        dispatcherServlet.service(request, response)

        then:
        1 * response.setStatus(201)
        outputBuffer.toString(StandardCharsets.UTF_8) == "Handled by bar: payload-data"
    }

    def "returns 500 Internal Server Error when a controller throws an unhandled exception"() {
        given:
        def failingController = new Controller() {
            @Override
            boolean canHandle(HttpRequest req) {
                return req.httpUri.matches(".*/boom")
            }

            @Override
            HttpResponse handle(HttpRequest req) {
                throw new IllegalStateException("Database connection failed")
            }
        }
        def dispatcherServlet = new DispatcherServlet([failingController])
        def outputBuffer = new ByteArrayOutputStream()
        def servletOutputStream = new CapturingServletOutputStream(outputBuffer)

        def request = Mock(HttpServletRequest) {
            getMethod() >> "GET"
            getRequestURL() >> new StringBuffer("http://localhost:8080/demo/boom")
        }
        def response = Mock(HttpServletResponse) {
            getOutputStream() >> servletOutputStream
        }

        when:
        dispatcherServlet.service(request, response)

        then:
        1 * response.setStatus(500)
        1 * response.addHeader("Content-Type", "text/plain; charset=UTF-8")
        outputBuffer.toString(StandardCharsets.UTF_8) == "Database connection failed"
    }

    private static class CapturingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream delegate

        CapturingServletOutputStream(ByteArrayOutputStream delegate) {
            this.delegate = delegate
        }

        @Override
        boolean isReady() {
            return true
        }

        @Override
        void setWriteListener(WriteListener writeListener) {
        }

        @Override
        void write(int b) throws IOException {
            delegate.write(b)
        }

        @Override
        void write(byte[] b) throws IOException {
            delegate.write(b)
        }

        @Override
        void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len)
        }
    }
}
