package dfb.demo

import dfb.demo.domain.Author
import dfb.demo.domain.Book
import dfb.demo.domain.BookCode
import dfb.demo.domain.Title
import spock.lang.Specification

class BookSpec extends Specification {

    def "BookCode trims input and rejects null or blank values"() {
        when:
        def code = BookCode.of("  ISBN-12345  ")

        then:
        code.value() == "ISBN-12345"
        code.toString() == "ISBN-12345"

        when:
        BookCode.of(invalid)

        then:
        thrown(expectedException)

        where:
        invalid | expectedException
        null    | NullPointerException
        ""      | IllegalArgumentException
        "   "   | IllegalArgumentException
    }

    def "Title trims input and rejects null or blank values"() {
        when:
        def title = Title.of("  Clean Architecture  ")

        then:
        title.value() == "Clean Architecture"
        title.toString() == "Clean Architecture"

        when:
        Title.of(invalid)

        then:
        thrown(expectedException)

        where:
        invalid | expectedException
        null    | NullPointerException
        ""      | IllegalArgumentException
        "   "   | IllegalArgumentException
    }

    def "Author trims input and rejects null or blank values"() {
        when:
        def author = Author.of("  Martin Fowler  ")

        then:
        author.name() == "Martin Fowler"
        author.toString() == "Martin Fowler"

        when:
        Author.of(invalid)

        then:
        thrown(expectedException)

        where:
        invalid | expectedException
        null    | NullPointerException
        ""      | IllegalArgumentException
        "   "   | IllegalArgumentException
    }

    def "Book enforces 1 or more authors and immutable state"() {
        when: "creating a book with valid details"
        def book = Book.of("978-0132350884", "Clean Code", "Robert C. Martin")

        then:
        book.code() == BookCode.of("978-0132350884")
        book.title() == Title.of("Clean Code")
        book.authors() == [Author.of("Robert C. Martin")]

        when: "creating a book with multiple authors"
        def multiAuthorBook = Book.of("978-0201616224", "The Pragmatic Programmer", "Andy Hunt", "Dave Thomas")

        then:
        multiAuthorBook.authors().size() == 2
        multiAuthorBook.authors() == [Author.of("Andy Hunt"), Author.of("Dave Thomas")]

        when: "attempting to create a book without authors"
        new Book(BookCode.of("123"), Title.of("No Author"), [])

        then:
        thrown(IllegalArgumentException)

        when: "attempting to mutate authors list"
        multiAuthorBook.authors().add(Author.of("Another Author"))

        then:
        thrown(UnsupportedOperationException)
    }

    def "Book supports updating title or authors producing new immutable instances"() {
        given:
        def original = Book.of("B-001", "Domain-Driven Design", "Eric Evans")

        when:
        def withNewTitle = original.withTitle("Domain-Driven Design: Tackling Complexity in the Heart of Software")

        then:
        withNewTitle.code() == original.code()
        withNewTitle.title() == Title.of("Domain-Driven Design: Tackling Complexity in the Heart of Software")
        withNewTitle.authors() == original.authors()
        !withNewTitle.is(original)

        when:
        def withNewAuthors = original.withAuthors("Eric Evans", "Martin Fowler")

        then:
        withNewAuthors.authors().size() == 2
        withNewAuthors.authors() == [Author.of("Eric Evans"), Author.of("Martin Fowler")]
    }
}
