package dfb.demo;

import dfb.fitz.Closeables;
import dfb.fitz.web.Controller;
import dfb.fitz.web.DispatcherServlet;
import dfb.fitz.property.CompositePropertySource;
import dfb.fitz.property.DecryptingPropertySource;
import dfb.fitz.property.FilePropertySource;
import dfb.fitz.property.MutablePropertySource;
import dfb.fitz.property.PollingPropertySource;
import dfb.fitz.property.PropertySource;
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

    private final DataContext dataContext;
    private final FooRepository fooRepository;
    private final FooService fooService;

    private final GooRepository gooRepository;
    private final GooService gooService;

    private final FooController fooController;

    private final List<Controller> controllers;

    private final DispatcherServlet dispatcherServlet;

    public ApplicationContext() {
        PropertySource defaultProperties = new FilePropertySource("default.properties");
        PropertySource localProperties = new PollingPropertySource(
                new FilePropertySource("local.properties"),
                Duration.ofSeconds(5)
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

        this.fooController = new FooController(fooService);
        this.controllers = List.of(fooController);
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
