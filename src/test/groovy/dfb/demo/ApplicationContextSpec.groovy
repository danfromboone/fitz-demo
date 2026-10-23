package dfb.demo

import dfb.fitz.jdbc.MutableDataSource
import jakarta.servlet.ServletContext
import jakarta.servlet.ServletContextEvent
import jakarta.servlet.ServletRegistration
import spock.lang.Specification

class ApplicationContextSpec extends Specification {

    def "wires application components, property sources, and controllers in ApplicationContext"() {
        given:
        def context = new ApplicationContext()

        expect:
        context.objectMapper != null
        context.restResponses != null
        context.propertySource != null
        context.dataContext != null
        (context.dataContext.dataSource as MutableDataSource).databaseConfig.url == "jdbc://goo/bar"
        (context.dataContext.dataSource as MutableDataSource).databaseConfig.userName == "goo"
        (context.dataContext.dataSource as MutableDataSource).databaseConfig.password == "goopassword"
        context.fooRepository != null
        context.fooService != null
        context.gooRepository != null
        context.gooService != null
        context.authorRepository != null
        context.bookRepository != null
        context.fooController != null
        context.getBooksController != null
        context.getBookByCodeController != null
        context.createBookController != null
        context.updateBookController != null
        context.deleteBookController != null
        context.getAuthorsController != null
        context.getAuthorByNameController != null
        context.createAuthorController != null
        context.deleteAuthorController != null
        context.controllers.size() == 10
        context.controllers == [
                context.fooController,
                context.getBooksController,
                context.getBookByCodeController,
                context.createBookController,
                context.updateBookController,
                context.deleteBookController,
                context.getAuthorsController,
                context.getAuthorByNameController,
                context.createAuthorController,
                context.deleteAuthorController
        ]

        cleanup:
        context.contextDestroyed(null)
    }

    def "initializes DispatcherServlet with controllers from context"() {
        given:
        def context = new ApplicationContext()

        when:
        def servlet = context.dispatcherServlet

        then:
        servlet != null
        servlet.controllers == context.controllers
        servlet.is(context.dispatcherServlet)
    }

    def "registers DispatcherServlet with ServletContext on contextInitialized"() {
        given:
        def context = new ApplicationContext()
        def registration = Mock(ServletRegistration.Dynamic)
        def servletContext = Mock(ServletContext)
        def event = new ServletContextEvent(servletContext)

        when:
        context.contextInitialized(event)

        then:
        1 * servletContext.addServlet("dispatcherServlet", { it.is(context.dispatcherServlet) }) >> registration
        1 * registration.setLoadOnStartup(1)
        1 * registration.addMapping("/")
    }

    def "has only one constructor which is no-arg"() {
        expect:
        ApplicationContext.constructors.length == 1
        ApplicationContext.constructors[0].parameterCount == 0
    }
}
