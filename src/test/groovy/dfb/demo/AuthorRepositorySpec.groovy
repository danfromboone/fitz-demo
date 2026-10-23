package dfb.demo

import dfb.demo.domain.Author
import dfb.fitz.property.PropertySource
import dfb.fitz.property.PollingPropertySource
import spock.lang.Specification

import java.time.Duration

class AuthorRepositorySpec extends Specification {

    DataContext dataContext
    AuthorRepository repository

    def setup() {
        def h2Url = "jdbc:h2:mem:author_repo_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1"
        def props = [
                "database.url"     : h2Url,
                "database.username": "sa",
                "database.password": "sa"
        ]
        PropertySource mapSource = { -> new LinkedHashMap<>(props) } as PropertySource
        def pollingSource = new PollingPropertySource(mapSource, Duration.ofMinutes(1))

        dataContext = new DataContext(pollingSource)
        repository = new AuthorRepository(dataContext.dataSource)
    }

    def cleanup() {
        dataContext?.close()
    }

    def "supports full CRUD operations for authors"() {
        expect: "starts empty"
        repository.count() == 0
        repository.findAll().isEmpty()

        when: "saving new authors"
        def author1 = repository.save(Author.of("Kent Beck"))
        def author2 = repository.save(Author.of("Martin Fowler"))

        then: "authors are stored"
        repository.count() == 2
        repository.existsByName("Kent Beck")
        repository.existsByName("Martin Fowler")
        !repository.existsByName("Unknown Author")

        when: "finding by name"
        def found = repository.findByName("Kent Beck")

        then:
        found.isPresent()
        found.get().name() == "Kent Beck"

        when: "querying all authors"
        def all = repository.findAll()

        then: "returned ordered alphabetically"
        all.size() == 2
        all[0].name() == "Kent Beck"
        all[1].name() == "Martin Fowler"

        when: "saving existing author is idempotent"
        repository.save(Author.of("Kent Beck"))

        then:
        repository.count() == 2

        when: "deleting an author"
        def deleted = repository.deleteByName("Kent Beck")

        then:
        deleted
        repository.count() == 1
        !repository.existsByName("Kent Beck")
        repository.findByName("Kent Beck").isEmpty()

        when: "deleting a non-existent author"
        def deleteNonExistent = repository.deleteByName("Non Existent")

        then:
        !deleteNonExistent
        repository.count() == 1
    }
}
