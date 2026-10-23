package dfb.demo

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
    BookController controller

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
        controller = new BookController(bookRepository)
    }

    def cleanup() {
        dataContext?.close()
    }

    def "canHandle recognizes /books and /books/{code} endpoints"() {
        expect:
        controller.canHandle(new HttpRequest(HttpMethod.GET, "/books"))
        controller.canHandle(new HttpRequest(HttpMethod.POST, "/books"))
        controller.canHandle(new HttpRequest(HttpMethod.GET, "/books/ISBN-123"))
        controller.canHandle(new HttpRequest(HttpMethod.PUT, "/books/ISBN-123"))
        controller.canHandle(new HttpRequest(HttpMethod.DELETE, "/books/ISBN-123"))
        controller.canHandle(new HttpRequest(HttpMethod.GET, "/demo/books"))
        !controller.canHandle(new HttpRequest(HttpMethod.GET, "/authors"))

        when: "null request is passed"
        controller.canHandle(null)

        then: "blows up with NullPointerException"
        thrown(NullPointerException)
    }

    def "handles complete REST lifecycle for books with authors"() {
        when: "getting all books initially"
        def emptyResponse = controller.handle(new HttpRequest(HttpMethod.GET, "/books"))

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
        def createResponse = controller.handle(postReq)

        then:
        createResponse.statusCode == HttpStatus.CREATED
        createResponse.body.asString().contains('"code":"ISBN-01"')
        createResponse.body.asString().contains('"title":"Clean Code"')
        createResponse.body.asString().contains('"Robert C. Martin"')
        bookRepository.existsByCode("ISBN-01")
        authorRepository.existsByName("Robert C. Martin")

        when: "creating duplicate book returns 409 CONFLICT"
        def duplicateResponse = controller.handle(postReq)

        then:
        duplicateResponse.statusCode == HttpStatus.CONFLICT

        when: "getting existing book by code"
        def getResponse = controller.handle(new HttpRequest(HttpMethod.GET, "/books/ISBN-01"))

        then:
        getResponse.statusCode == HttpStatus.OK
        getResponse.body.asString().contains('"title":"Clean Code"')

        when: "getting non-existent book"
        def notFoundResponse = controller.handle(new HttpRequest(HttpMethod.GET, "/books/NON-EXISTENT"))

        then:
        notFoundResponse.statusCode == HttpStatus.NOT_FOUND

        when: "creating another book with multiple authors"
        controller.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('{"code":"ISBN-02","title":"Refactoring","authors":["Martin Fowler","Kent Beck"]}')
        ))

        and: "querying all books"
        def allResponse = controller.handle(new HttpRequest(HttpMethod.GET, "/books"))

        then:
        allResponse.statusCode == HttpStatus.OK
        allResponse.body.asString().contains('"ISBN-01"')
        allResponse.body.asString().contains('"ISBN-02"')

        when: "filtering books by author"
        def filterResponse = controller.handle(new HttpRequest(HttpMethod.GET, "/books?author=Kent%20Beck"))

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
        def updateResponse = controller.handle(putReq)

        then:
        updateResponse.statusCode == HttpStatus.OK
        updateResponse.body.asString().contains('"Dean Wampler"')
        authorRepository.existsByName("Dean Wampler")

        when: "updating non-existent book"
        def putNotFound = controller.handle(new HttpRequest(
                HttpMethod.PUT,
                new HttpUri("/books/MISSING"),
                new HttpHeaders(),
                new HttpRequestBody('{"title":"No Title","authors":["Someone"]}')
        ))

        then:
        putNotFound.statusCode == HttpStatus.NOT_FOUND

        when: "deleting existing book"
        def deleteResponse = controller.handle(new HttpRequest(HttpMethod.DELETE, "/books/ISBN-01"))

        then:
        deleteResponse.statusCode == HttpStatus.NO_CONTENT
        !bookRepository.existsByCode("ISBN-01")

        when: "deleting non-existent book"
        def deleteAgain = controller.handle(new HttpRequest(HttpMethod.DELETE, "/books/ISBN-01"))

        then:
        deleteAgain.statusCode == HttpStatus.NOT_FOUND
    }

    def "validates request payload and rejects invalid inputs"() {
        when: "POST with empty body"
        def emptyBody = controller.handle(new HttpRequest(HttpMethod.POST, "/books"))

        then:
        emptyBody.statusCode == HttpStatus.BAD_REQUEST

        when: "POST with missing title"
        def missingTitle = controller.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('{"code":"C-1","authors":["Author"]}')
        ))

        then:
        missingTitle.statusCode == HttpStatus.BAD_REQUEST

        when: "POST with zero authors"
        def noAuthors = controller.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('{"code":"C-1","title":"No Authors","authors":[]}')
        ))

        then:
        noAuthors.statusCode == HttpStatus.BAD_REQUEST

        when: "POST with malformed JSON"
        def malformed = controller.handle(new HttpRequest(
                HttpMethod.POST,
                new HttpUri("/books"),
                new HttpHeaders(),
                new HttpRequestBody('NOT_JSON')
        ))

        then:
        malformed.statusCode == HttpStatus.BAD_REQUEST
    }
}
