package dfb.demo

import com.fasterxml.jackson.databind.ObjectMapper
import dfb.demo.web.RestResponses
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
    GetAuthorsController getAuthorsController
    GetAuthorByNameController getAuthorByNameController
    CreateAuthorController createAuthorController
    DeleteAuthorController deleteAuthorController

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
        def restResponses = new RestResponses(new ObjectMapper())
        getAuthorsController = new GetAuthorsController(authorRepository, restResponses)
        getAuthorByNameController = new GetAuthorByNameController(authorRepository, restResponses)
        createAuthorController = new CreateAuthorController(authorRepository, restResponses)
        deleteAuthorController = new DeleteAuthorController(authorRepository, restResponses)
    }

    def cleanup() {
        dataContext?.close()
    }

    def "canHandle routes requests to specific endpoint controllers"() {
        expect:
        getAuthorsController.canHandle(new HttpRequest(HttpMethod.GET, "/authors"))
        !getAuthorsController.canHandle(new HttpRequest(HttpMethod.POST, "/authors"))
        !getAuthorsController.canHandle(new HttpRequest(HttpMethod.GET, "/authors/Martin%20Fowler"))

        getAuthorByNameController.canHandle(new HttpRequest(HttpMethod.GET, "/authors/Martin%20Fowler"))
        !getAuthorByNameController.canHandle(new HttpRequest(HttpMethod.GET, "/authors"))
        !getAuthorByNameController.canHandle(new HttpRequest(HttpMethod.DELETE, "/authors/Martin%20Fowler"))

        createAuthorController.canHandle(new HttpRequest(HttpMethod.POST, "/authors"))
        !createAuthorController.canHandle(new HttpRequest(HttpMethod.GET, "/authors"))

        deleteAuthorController.canHandle(new HttpRequest(HttpMethod.DELETE, "/authors/Kent%20Beck"))
        !deleteAuthorController.canHandle(new HttpRequest(HttpMethod.GET, "/authors/Kent%20Beck"))

        when: "null request is passed"
        getAuthorsController.canHandle(null)

        then: "blows up with NullPointerException"
        thrown(NullPointerException)
    }

    def "handles complete REST lifecycle for authors"() {
        when: "getting all authors initially"
        def emptyResponse = getAuthorsController.handle(new HttpRequest(HttpMethod.GET, "/authors"))

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
        def createResponse = createAuthorController.handle(postReq)

        then:
        createResponse.statusCode == HttpStatus.CREATED
        createResponse.body.asString().contains('"name":"Martin Fowler"')
        authorRepository.existsByName("Martin Fowler")

        when: "attempting to create duplicate author"
        def duplicateResponse = createAuthorController.handle(postReq)

        then:
        duplicateResponse.statusCode == HttpStatus.CONFLICT

        when: "getting existing author by name"
        def getResponse = getAuthorByNameController.handle(new HttpRequest(HttpMethod.GET, "/authors/Martin%20Fowler"))

        then:
        getResponse.statusCode == HttpStatus.OK
        getResponse.body.asString().contains('"name":"Martin Fowler"')

        when: "getting non-existent author"
        def notFoundResponse = getAuthorByNameController.handle(new HttpRequest(HttpMethod.GET, "/authors/Unknown"))

        then:
        notFoundResponse.statusCode == HttpStatus.NOT_FOUND

        when: "deleting existing author"
        def deleteResponse = deleteAuthorController.handle(new HttpRequest(HttpMethod.DELETE, "/authors/Martin%20Fowler"))

        then:
        deleteResponse.statusCode == HttpStatus.NO_CONTENT
        !authorRepository.existsByName("Martin Fowler")

        when: "deleting already deleted author"
        def deleteAgainResponse = deleteAuthorController.handle(new HttpRequest(HttpMethod.DELETE, "/authors/Martin%20Fowler"))

        then:
        deleteAgainResponse.statusCode == HttpStatus.NOT_FOUND
    }

    def "handles invalid requests gracefully"() {
        when: "POST with empty body"
        def emptyPost = createAuthorController.handle(new HttpRequest(HttpMethod.POST, "/authors"))

        then:
        emptyPost.statusCode == HttpStatus.BAD_REQUEST

        when: "POST with blank author name"
        def blankNamePost = createAuthorController.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/authors"),
                new HttpHeaders(),
                new HttpRequestBody('{"name":"   "}')
        ))

        then:
        blankNamePost.statusCode == HttpStatus.BAD_REQUEST
    }
}
