package dfb.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dfb.demo.web.RestResponses;
import dfb.fitz.Closeables;
import dfb.fitz.property.CompositePropertySource;
import dfb.fitz.property.DecryptingPropertySource;
import dfb.fitz.property.FilePropertySource;
import dfb.fitz.property.MutablePropertySource;
import dfb.fitz.property.PollingPropertySource;
import dfb.fitz.property.PropertySource;
import dfb.fitz.web.Controller;
import dfb.fitz.web.DispatcherServlet;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import lombok.Getter;

import java.time.Duration;
import java.util.List;

@Getter
@WebListener
public class ApplicationContext implements ServletContextListener {

    private final MutablePropertySource propertySource;

    private final ObjectMapper objectMapper;
    private final RestResponses restResponses;

    private final DataContext dataContext;
    private final FooRepository fooRepository;
    private final FooService fooService;

    private final GooRepository gooRepository;
    private final GooService gooService;

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    private final FooController fooController;

    private final GetBooksController getBooksController;
    private final GetBookByCodeController getBookByCodeController;
    private final CreateBookController createBookController;
    private final UpdateBookController updateBookController;
    private final DeleteBookController deleteBookController;

    private final GetAuthorsController getAuthorsController;
    private final GetAuthorByNameController getAuthorByNameController;
    private final CreateAuthorController createAuthorController;
    private final DeleteAuthorController deleteAuthorController;

    private final List<Controller> controllers;

    private final DispatcherServlet dispatcherServlet;

    public ApplicationContext() {
        this.objectMapper = new ObjectMapper();
        this.restResponses = new RestResponses(this.objectMapper);

        PropertySource defaultProperties = new FilePropertySource("default.properties");
        PropertySource localProperties = new PollingPropertySource(
                new FilePropertySource("local.properties"),
                Duration.ofSeconds(600)
        );
        CompositePropertySource compositePropertySource = new CompositePropertySource(
                defaultProperties,
                localProperties
        );
        this.propertySource = new DecryptingPropertySource(compositePropertySource);

        this.dataContext = new DataContext(this.propertySource);

        this.fooRepository = new FooRepository(dataContext.getDataSource());
        this.fooService = new FooService(fooRepository);
        this.gooRepository = new GooRepository(dataContext.getDataSource());
        this.gooService = new GooService(gooRepository);

        this.authorRepository = new AuthorRepository(dataContext.getDataSource());
        this.bookRepository = new BookRepository(dataContext.getDataSource(), this.authorRepository);

        this.fooController = new FooController(fooService);

        this.getBooksController = new GetBooksController(bookRepository, this.restResponses);
        this.getBookByCodeController = new GetBookByCodeController(bookRepository, this.restResponses);
        this.createBookController = new CreateBookController(bookRepository, this.restResponses);
        this.updateBookController = new UpdateBookController(bookRepository, this.restResponses);
        this.deleteBookController = new DeleteBookController(bookRepository, this.restResponses);

        this.getAuthorsController = new GetAuthorsController(authorRepository, this.restResponses);
        this.getAuthorByNameController = new GetAuthorByNameController(authorRepository, this.restResponses);
        this.createAuthorController = new CreateAuthorController(authorRepository, this.restResponses);
        this.deleteAuthorController = new DeleteAuthorController(authorRepository, this.restResponses);

        this.controllers = List.of(
                fooController,
                getBooksController,
                getBookByCodeController,
                createBookController,
                updateBookController,
                deleteBookController,
                getAuthorsController,
                getAuthorByNameController,
                createAuthorController,
                deleteAuthorController
        );
        this.dispatcherServlet = new DispatcherServlet(this.controllers);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletRegistration.Dynamic registration = sce.getServletContext()
                .addServlet("dispatcherServlet", dispatcherServlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Closeables.closeAllQuietly(dataContext, propertySource);
    }
}
