package dfb.demo

import dfb.fitz.http.HttpHeaders
import dfb.fitz.http.HttpMethod
import dfb.fitz.http.HttpRequest
import dfb.fitz.http.HttpRequestBody
import dfb.fitz.http.HttpStatus
import dfb.fitz.http.HttpUri
import dfb.demo.FooController
import dfb.demo.FooService
import spock.lang.Specification

class FooControllerSpec extends Specification {

    FooService fooService = Mock()
    def controller = new FooController(fooService)

    def "canHandle returns true for GET requests to /foo"() {
        given:
        def request = new HttpRequest(method, new HttpUri(url, query), new HttpHeaders(), new HttpRequestBody(""))

        expect:
        controller.canHandle(request) == expected

        where:
        method          | url                              | query     | expected
        HttpMethod.GET | "http://localhost:8080/foo" | null | true
        HttpMethod.GET  | "http://localhost:8080/foo"      | "a=1&b=2" | true
        HttpMethod.POST | "http://localhost:8080/foo"      | null      | false
        HttpMethod.GET  | "http://localhost:8080/bar"      | null      | false
        HttpMethod.GET  | "http://localhost:8080/foo/bar"  | null      | false
    }

    def "canHandle returns false for null request"() {
        expect:
        !controller.canHandle(null)
    }

    def "handle returns 200 OK response containing decoded query parameters"() {
        given:
        def request = new HttpRequest(HttpMethod.GET, "http://localhost:8080/foo?greeting=Hello%20World&flag")

        when:
        def response = controller.handle(request)

        then:
        response.statusCode == HttpStatus.OK
        response.headers.getFirst("Content-Type") == "text/plain; charset=UTF-8"
        response.body.asString() == "Request params: {greeting=[Hello World], flag=[]}"
    }
}
