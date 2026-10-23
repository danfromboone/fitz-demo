package dfb.demo;

import dfb.fitz.Closeables;
import dfb.fitz.jdbc.DatabaseConfig;
import dfb.fitz.jdbc.MutableDataSource;
import dfb.fitz.property.MutablePropertySource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;

@Getter
public class DataContext implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DataContext.class);

    private final MutableDataSource dataSource;
    private final MutablePropertySource propertySource;

    public DataContext(MutablePropertySource propertySource) {
        this.propertySource = propertySource;

        DatabaseConfig databaseConfig = getDatabaseConfig();
        this.dataSource = new MutableDataSource(databaseConfig);

        propertySource.registerListener("(database|datasource)\\..*", () -> {
            log.info("code=DATA_CONFIG_PROPERTY_CHANGE");
            DatabaseConfig updatedDatabaseConfig = getDatabaseConfig();

            dataSource.setDatabaseConfig(updatedDatabaseConfig);
        });

        runLiquibase();
    }

    public void runLiquibase() {
        String enabled = getProperty("liquibase.enabled");
        if ("false".equalsIgnoreCase(enabled)) {
            log.info("code=LIQUIBASE_DISABLED");
            return;
        }

        DatabaseConfig config = dataSource.getDatabaseConfig();
        if (config == null || config.getUrl() == null || !config.getUrl().matches("^jdbc:[a-zA-Z0-9_]+:.*")) {
            log.info("code=LIQUIBASE_SKIPPED reason=INVALID_OR_DUMMY_URL url={}", (Object) (config != null ? config.getUrl() : null));
            return;
        }

        String changelog = getProperty("liquibase.change-log", "liquibase.changelog");
        if (changelog == null || changelog.isBlank()) {
            changelog = "db/changelog/db.changelog-master.xml";
        }

        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(getClass().getClassLoader()), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
                log.info("code=LIQUIBASE_MIGRATION_SUCCESS changelog={}", changelog);
            }
        } catch (Exception e) {
            log.error("code=LIQUIBASE_MIGRATION_ERROR changelog={}", changelog, e);
            throw new RuntimeException("Failed to run Liquibase migrations", e);
        }
    }

    public DatabaseConfig getDatabaseConfig() {
        String url = getProperty("database.url", "datasource.url");
        String user = getProperty("database.username", "datasource.username", "database.user", "datasource.user");
        String pass = getProperty("database.password", "datasource.password");
        String driver = getProperty(
                "database.driver-class-name", "datasource.driver-class-name",
                "database.driverClassName", "datasource.driverClassName"
        );

        if (url != null && !url.isBlank()) {
            return DatabaseConfig.builder()
                    .url(url)
                    .userName(user != null ? user : "sa")
                    .password(pass != null ? pass : "sa")
                    .driverClassName(driver)
                    .build();
        }
        return DatabaseConfig.builder()
                .url("jdbc:h2:mem:fitzdemo;DB_CLOSE_DELAY=-1")
                .userName("sa")
                .password("sa")
                .driverClassName(driver)
                .build();
    }

    private String getProperty(String... keys) {
        if (propertySource == null) {
            return null;
        }
        for (String key : keys) {
            String val = propertySource.getProperty(key);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public void close() {
        Closeables.closeQuietly(dataSource);
    }
}
