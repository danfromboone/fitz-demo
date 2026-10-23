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

class BookControllerSpec extends Specification {

    DataContext dataContext
    AuthorRepository authorRepository
    BookRepository bookRepository
    GetBooksController getBooksController
    GetBookByCodeController getBookByCodeController
    CreateBookController createBookController
    UpdateBookController updateBookController
    DeleteBookController deleteBookController

    def setup() {
        def h2Url = "jdbc:h2:mem:book_ctrl_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1"
        def props = [
                "database.url"     : h2Url,
                "database.username": "sa",
                "database.password": "sa"
        ]
        PropertySource mapSource = { -> new LinkedHashMap<>(props) } as PropertySource
        def pollingSource = new PollingPropertySource(mapSource, Duration.ofMinutes(1))

        dataContext = new DataContext(pollingSource)
        authorRepository = new AuthorRepository(dataContext.dataSource)
        bookRepository = new BookRepository(dataContext.dataSource, authorRepository)
        def restResponses = new RestResponses(new ObjectMapper())

        getBooksController = new GetBooksController(bookRepository, restResponses)
        getBookByCodeController = new GetBookByCodeController(bookRepository, restResponses)
        createBookController = new CreateBookController(bookRepository, restResponses)
        updateBookController = new UpdateBookController(bookRepository, restResponses)
        deleteBookController = new DeleteBookController(bookRepository, restResponses)
    }

    def cleanup() {
        dataContext?.close()
    }

    def "canHandle routes requests to specific endpoint controllers"() {
        expect:
        getBooksController.canHandle(new HttpRequest(HttpMethod.GET, "/books"))
        !getBooksController.canHandle(new HttpRequest(HttpMethod.POST, "/books"))
        !getBooksController.canHandle(new HttpRequest(HttpMethod.GET, "/books/ISBN-123"))

        getBookByCodeController.canHandle(new HttpRequest(HttpMethod.GET, "/books/ISBN-123"))
        !getBookByCodeController.canHandle(new HttpRequest(HttpMethod.GET, "/books"))
        !getBookByCodeController.canHandle(new HttpRequest(HttpMethod.PUT, "/books/ISBN-123"))

        createBookController.canHandle(new HttpRequest(HttpMethod.POST, "/books"))
        !createBookController.canHandle(new HttpRequest(HttpMethod.GET, "/books"))

        updateBookController.canHandle(new HttpRequest(HttpMethod.PUT, "/books/ISBN-123"))
        !updateBookController.canHandle(new HttpRequest(HttpMethod.GET, "/books/ISBN-123"))

        deleteBookController.canHandle(new HttpRequest(HttpMethod.DELETE, "/books/ISBN-123"))
        !deleteBookController.canHandle(new HttpRequest(HttpMethod.GET, "/books/ISBN-123"))

        when: "null request is passed"
        getBooksController.canHandle(null)

        then: "blows up with NullPointerException"
        thrown(NullPointerException)
    }

    def "handles complete REST lifecycle for books with authors"() {
        when: "getting all books initially"
        def emptyResponse = getBooksController.handle(new HttpRequest(HttpMethod.GET, "/books"))

        then:
        emptyResponse.statusCode == HttpStatus.OK
        emptyResponse.body.asString() == "[]"

        when: "creating a new book via POST"
        def postReq = new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('{"code":"ISBN-01","title":"Clean Code","authors":["Robert C. Martin"]}')
        )
        def createResponse = createBookController.handle(postReq)

        then:
        createResponse.statusCode == HttpStatus.CREATED
        createResponse.body.asString().contains('"code":"ISBN-01"')
        createResponse.body.asString().contains('"title":"Clean Code"')
        createResponse.body.asString().contains('"Robert C. Martin"')
        bookRepository.existsByCode("ISBN-01")
        authorRepository.existsByName("Robert C. Martin")

        when: "creating duplicate book returns 409 CONFLICT"
        def duplicateResponse = createBookController.handle(postReq)

        then:
        duplicateResponse.statusCode == HttpStatus.CONFLICT

        when: "getting existing book by code"
        def getResponse = getBookByCodeController.handle(new HttpRequest(HttpMethod.GET, "/books/ISBN-01"))

        then:
        getResponse.statusCode == HttpStatus.OK
        getResponse.body.asString().contains('"title":"Clean Code"')

        when: "getting non-existent book"
        def notFoundResponse = getBookByCodeController.handle(new HttpRequest(HttpMethod.GET, "/books/NON-EXISTENT"))

        then:
        notFoundResponse.statusCode == HttpStatus.NOT_FOUND

        when: "creating another book with multiple authors"
        createBookController.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('{"code":"ISBN-02","title":"Refactoring","authors":["Martin Fowler","Kent Beck"]}')
        ))

        and: "querying all books"
        def allResponse = getBooksController.handle(new HttpRequest(HttpMethod.GET, "/books"))

        then:
        allResponse.statusCode == HttpStatus.OK
        allResponse.body.asString().contains('"ISBN-01"')
        allResponse.body.asString().contains('"ISBN-02"')

        when: "filtering books by author"
        def filterResponse = getBooksController.handle(new HttpRequest(HttpMethod.GET, "/books?author=Kent%20Beck"))

        then:
        filterResponse.statusCode == HttpStatus.OK
        filterResponse.body.asString().contains('"ISBN-02"')
        !filterResponse.body.asString().contains('"ISBN-01"')

        when: "updating book via PUT"
        def putReq = new HttpRequest(
                HttpMethod.PUT,
                new HttpUri("/books/ISBN-01"),
                new HttpHeaders(),
                new HttpRequestBody('{"title":"Clean Code: A Handbook of Agile Software Craftsmanship","authors":["Robert C. Martin","Dean Wampler"]}')
        )
        def updateResponse = updateBookController.handle(putReq)

        then:
        updateResponse.statusCode == HttpStatus.OK
        updateResponse.body.asString().contains('"Dean Wampler"')
        authorRepository.existsByName("Dean Wampler")

        when: "updating non-existent book"
        def putNotFound = updateBookController.handle(new HttpRequest(
                HttpMethod.PUT,
                new HttpUri("/books/MISSING"),
                new HttpHeaders(),
                new HttpRequestBody('{"title":"No Title","authors":["Someone"]}')
        ))

        then:
        putNotFound.statusCode == HttpStatus.NOT_FOUND

        when: "deleting existing book"
        def deleteResponse = deleteBookController.handle(new HttpRequest(HttpMethod.DELETE, "/books/ISBN-01"))

        then:
        deleteResponse.statusCode == HttpStatus.NO_CONTENT
        !bookRepository.existsByCode("ISBN-01")

        when: "deleting non-existent book"
        def deleteAgain = deleteBookController.handle(new HttpRequest(HttpMethod.DELETE, "/books/ISBN-01"))

        then:
        deleteAgain.statusCode == HttpStatus.NOT_FOUND
    }

    def "validates request payload and rejects invalid inputs"() {
        when: "POST with empty body"
        def emptyBody = createBookController.handle(new HttpRequest(HttpMethod.POST, "/books"))

        then:
        emptyBody.statusCode == HttpStatus.BAD_REQUEST

        when: "POST with missing title"
        def missingTitle = createBookController.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('{"code":"C-1","authors":["Author"]}')
        ))

        then:
        missingTitle.statusCode == HttpStatus.BAD_REQUEST

        when: "POST with zero authors"
        def noAuthors = createBookController.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('{"code":"C-1","title":"No Authors","authors":[]}')
        ))

        then:
        noAuthors.statusCode == HttpStatus.BAD_REQUEST

        when: "POST with malformed JSON"
        def malformed = createBookController.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('NOT_JSON')
        ))

        then:
        malformed.statusCode == HttpStatus.BAD_REQUEST
    }
}
