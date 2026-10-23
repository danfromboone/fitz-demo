package dfb.demo

import dfb.demo.domain.Author
import dfb.demo.domain.Book
import dfb.demo.domain.BookCode
import dfb.demo.domain.Title
import dfb.fitz.property.PollingPropertySource
import dfb.fitz.property.PropertySource
import spock.lang.Specification

import java.time.Duration

class BookRepositorySpec extends Specification {

    DataContext dataContext
    AuthorRepository authorRepository
    BookRepository bookRepository

    def setup() {
        def h2Url = "jdbc:h2:mem:book_repo_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1"
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
    }

    def cleanup() {
        dataContext?.close()
    }

    def "supports full CRUD operations for books with multiple authors"() {
        expect: "starts empty"
        bookRepository.count() == 0
        bookRepository.findAll().isEmpty()

        when: "saving a book with a single author"
        def ddd = Book.of("ISBN-0321125215", "Domain-Driven Design", "Eric Evans")
        bookRepository.save(ddd)

        then: "book and author are persisted"
        bookRepository.count() == 1
        bookRepository.existsByCode(BookCode.of("ISBN-0321125215"))
        authorRepository.existsByName("Eric Evans")

        when: "retrieving the book by code"
        def foundDdd = bookRepository.findByCode(BookCode.of("ISBN-0321125215"))

        then:
        foundDdd.isPresent()
        foundDdd.get().code() == BookCode.of("ISBN-0321125215")
        foundDdd.get().title() == Title.of("Domain-Driven Design")
        foundDdd.get().authors() == [Author.of("Eric Evans")]

        when: "saving a book with multiple authors"
        def refactoring = Book.of("ISBN-0134757599", "Refactoring", "Martin Fowler", "Kent Beck")
        bookRepository.save(refactoring)

        then: "persisted with both authors in order"
        bookRepository.count() == 2
        authorRepository.count() == 3

        def foundRefactoring = bookRepository.findByCode("ISBN-0134757599")
        foundRefactoring.isPresent()
        foundRefactoring.get().title() == Title.of("Refactoring")
        foundRefactoring.get().authors() == [Author.of("Martin Fowler"), Author.of("Kent Beck")]

        when: "finding all books"
        def allBooks = bookRepository.findAll()

        then:
        allBooks.size() == 2
        allBooks.collect { it.code().value() } == ["ISBN-0134757599", "ISBN-0321125215"]

        when: "finding books by author"
        def fowlerBooks = bookRepository.findByAuthor("Martin Fowler")
        def evansBooks = bookRepository.findByAuthor("Eric Evans")
        def nonExistentBooks = bookRepository.findByAuthor("Ghost Writer")

        then:
        fowlerBooks.size() == 1
        fowlerBooks[0].title() == Title.of("Refactoring")
        evansBooks.size() == 1
        evansBooks[0].title() == Title.of("Domain-Driven Design")
        nonExistentBooks.isEmpty()

        when: "updating an existing book's title and authors list"
        def updatedRefactoring = refactoring
                .withTitle("Refactoring: Improving the Design of Existing Code")
                .withAuthors("Martin Fowler")
        bookRepository.save(updatedRefactoring)

        then:
        bookRepository.count() == 2
        def reloaded = bookRepository.findByCode(refactoring.code()).get()
        reloaded.title() == Title.of("Refactoring: Improving the Design of Existing Code")
        reloaded.authors() == [Author.of("Martin Fowler")]

        when: "deleting a book by code"
        def deleted = bookRepository.deleteByCode(BookCode.of("ISBN-0321125215"))

        then:
        deleted
        bookRepository.count() == 1
        !bookRepository.existsByCode(BookCode.of("ISBN-0321125215"))
        bookRepository.findByCode("ISBN-0321125215").isEmpty()

        when: "deleting a non-existent book"
        def deleteNonExistent = bookRepository.deleteByCode("NON-EXISTENT")

        then:
        !deleteNonExistent
        bookRepository.count() == 1
    }
}
