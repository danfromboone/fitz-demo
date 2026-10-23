package dfb.demo

import dfb.demo.domain.Author
import dfb.fitz.http.HttpHeaders
import dfb.fitz.http.HttpMethod
import dfb.fitz.http.HttpRequest
import dfb.fitz.http.HttpRequestBody
import dfb.fitz.http.HttpUri
import dfb.fitz.http.HttpStatus
import dfb.fitz.property.PollingPropertySource
import dfb.fitz.property.PropertySource
import spock.lang.Specification

import java.time.Duration

class AuthorControllerSpec extends Specification {

    DataContext dataContext
    AuthorRepository authorRepository
    AuthorController controller

    def setup() {
        def h2Url = "jdbc:h2:mem:author_ctrl_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1"
        def props = [
                "database.url"     : h2Url,
                "database.username": "sa",
                "database.password": "sa"
        ]
        PropertySource mapSource = { -> new LinkedHashMap<>(props) } as PropertySource
        def pollingSource = new PollingPropertySource(mapSource, Duration.ofMinutes(1))

        dataContext = new DataContext(pollingSource)
        authorRepository = new AuthorRepository(dataContext.dataSource)
        controller = new AuthorController(authorRepository)
    }

    def cleanup() {
        dataContext?.close()
    }

    def "canHandle recognizes /authors and /authors/{name} endpoints"() {
        expect:
        controller.canHandle(new HttpRequest(HttpMethod.GET, "/authors"))
        controller.canHandle(new HttpRequest(HttpMethod.POST, "/authors"))
        controller.canHandle(new HttpRequest(HttpMethod.GET, "/authors/Martin%20Fowler"))
        controller.canHandle(new HttpRequest(HttpMethod.DELETE, "/authors/Kent%20Beck"))
        controller.canHandle(new HttpRequest(HttpMethod.GET, "/demo/authors"))
        !controller.canHandle(new HttpRequest(HttpMethod.GET, "/books"))

        when: "null request is passed"
        controller.canHandle(null)

        then: "blows up with NullPointerException"
        thrown(NullPointerException)
    }

    def "handles complete REST lifecycle for authors"() {
        when: "getting all authors initially"
        def emptyResponse = controller.handle(new HttpRequest(HttpMethod.GET, "/authors"))

        then:
        emptyResponse.statusCode == HttpStatus.OK
        emptyResponse.body.asString() == "[]"

        when: "creating a new author via POST"
        def postReq = new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/authors"),
                new HttpHeaders(),
                new HttpRequestBody('{"name":"Martin Fowler"}')
        )
        def createResponse = controller.handle(postReq)

        then:
        createResponse.statusCode == HttpStatus.CREATED
        createResponse.body.asString().contains('"name":"Martin Fowler"')
        authorRepository.existsByName("Martin Fowler")

        when: "attempting to create duplicate author"
        def duplicateResponse = controller.handle(postReq)

        then:
        duplicateResponse.statusCode == HttpStatus.CONFLICT

        when: "getting existing author by name"
        def getResponse = controller.handle(new HttpRequest(HttpMethod.GET, "/authors/Martin%20Fowler"))

        then:
        getResponse.statusCode == HttpStatus.OK
        getResponse.body.asString().contains('"name":"Martin Fowler"')

        when: "getting non-existent author"
        def notFoundResponse = controller.handle(new HttpRequest(HttpMethod.GET, "/authors/Unknown"))

        then:
        notFoundResponse.statusCode == HttpStatus.NOT_FOUND

        when: "deleting existing author"
        def deleteResponse = controller.handle(new HttpRequest(HttpMethod.DELETE, "/authors/Martin%20Fowler"))

        then:
        deleteResponse.statusCode == HttpStatus.NO_CONTENT
        !authorRepository.existsByName("Martin Fowler")

        when: "deleting already deleted author"
        def deleteAgainResponse = controller.handle(new HttpRequest(HttpMethod.DELETE, "/authors/Martin%20Fowler"))

        then:
        deleteAgainResponse.statusCode == HttpStatus.NOT_FOUND
    }

    def "handles invalid requests gracefully"() {
        when: "POST with empty body"
        def emptyPost = controller.handle(new HttpRequest(HttpMethod.POST, "/authors"))

        then:
        emptyPost.statusCode == HttpStatus.BAD_REQUEST

        when: "POST with blank author name"
        def blankNamePost = controller.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/authors"),
                new HttpHeaders(),
                new HttpRequestBody('{"name":"   "}')
        ))

        then:
        blankNamePost.statusCode == HttpStatus.BAD_REQUEST
    }
}
