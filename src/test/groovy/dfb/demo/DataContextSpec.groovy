package dfb.demo

import dfb.fitz.jdbc.DatabaseConfig
import dfb.fitz.jdbc.MutableDataSource
import dfb.fitz.property.CompositePropertySource
import dfb.fitz.property.DecryptingPropertySource
import dfb.fitz.property.FilePropertySource
import dfb.fitz.property.PollingPropertySource
import dfb.fitz.property.PropertyEncryptor
import dfb.fitz.property.PropertySource
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class DataContextSpec extends Specification {

    @TempDir
    Path tempDir

    def "initializes MutableDataSource with DatabaseConfig from local.properties overriding default.properties"() {
        given:
        def localPolling = new PollingPropertySource(new FilePropertySource("local.properties"), Duration.ofMinutes(1))
        def composite = new CompositePropertySource(
                new FilePropertySource("default.properties"),
                localPolling
        )
        def decrypting = new DecryptingPropertySource(composite)

        when:
        def dataContext = new DataContext(decrypting)
        MutableDataSource dataSource = dataContext.dataSource as MutableDataSource

        then:
        dataSource.databaseConfig.url == "jdbc://goo/bar"
        dataSource.databaseConfig.userName == "goo"
        dataSource.databaseConfig.password == "goopassword"

        cleanup:
        decrypting.close()
    }

    def "atomically swaps DatabaseConfig with decrypted password when independently polled source updates ENC(...) value"() {
        given:
        Path keyFile = tempDir.resolve("secret.key")
        Files.writeString(keyFile, PropertyEncryptor.generateKeyString())

        def dynamicOverrides = [
                (DecryptingPropertySource.DEFAULT_KEY_PATH_PROPERTY): keyFile.toString(),
                "database.url"                                      : "jdbc://goo/bar",
                "database.username"                                 : "goo",
                "database.password"                                 : PropertyEncryptor.encrypt("initial_secret", keyFile)
        ]
        def localPollingSource = new PollingPropertySource(
                { -> new LinkedHashMap<>(dynamicOverrides) } as PropertySource,
                Duration.ofMinutes(1)
        )
        def composite = new CompositePropertySource(
                new FilePropertySource("default.properties"),
                localPollingSource
        )
        def propertySource = new DecryptingPropertySource(composite)
        def dataContext = new DataContext(propertySource)
        MutableDataSource dataSource = dataContext.dataSource as MutableDataSource
        DatabaseConfig initialConfig = dataSource.databaseConfig

        expect: "initial encrypted password was decrypted into DatabaseConfig"
        initialConfig.password == "initial_secret"

        when: "database properties (including encrypted password) are updated and local source is polled"
        dynamicOverrides["database.url"] = "jdbc://prod/bar"
        dynamicOverrides["database.username"] = "prod_user"
        dynamicOverrides["database.password"] = PropertyEncryptor.encrypt("rotated_prod_secret", keyFile)
        localPollingSource.poll()

        then: "the same DataSource instance has a new immutable DatabaseConfig with decrypted values swapped in"
        dataContext.dataSource.is(dataSource)
        !dataSource.databaseConfig.is(initialConfig)
        dataSource.databaseConfig.url == "jdbc://prod/bar"
        dataSource.databaseConfig.userName == "prod_user"
        dataSource.databaseConfig.password == "rotated_prod_secret"

        cleanup:
        propertySource.close()
    }

    def "does not trigger database reconfiguration when non-database properties change"() {
        given:
        def props = [
                "database.url"     : "jdbc://goo/bar",
                "database.username": "goo",
                "database.password": "goopassword",
                "app.theme"        : "light"
        ]
        PropertySource mapSource = { -> new LinkedHashMap<>(props) } as PropertySource
        def pollingSource = new PollingPropertySource(mapSource, Duration.ofMinutes(1))

        def dataContext = new DataContext(pollingSource)
        MutableDataSource dataSource = dataContext.dataSource as MutableDataSource
        DatabaseConfig initialConfig = dataSource.databaseConfig

        when: "an unrelated property changes"
        props["app.theme"] = "dark"
        pollingSource.poll()

        then: "the DatabaseConfig instance is untouched"
        dataSource.databaseConfig.is(initialConfig)

        when: "a database property changes"
        props["database.password"] = "rotated_password"
        pollingSource.poll()

        then: "a new DatabaseConfig instance is swapped in"
        !dataSource.databaseConfig.is(initialConfig)
        dataSource.databaseConfig.password == "rotated_password"

        cleanup:
        pollingSource.close()
    }

    def "DatabaseConfig trims arguments and rejects null or blank values"() {
        when:
        def config = new DatabaseConfig("  jdbc://goo/bar  ", "  goo  ", "  goopassword  ")

        then:
        config.url == "jdbc://goo/bar"
        config.userName == "goo"
        config.password == "goopassword"

        when:
        new DatabaseConfig(url, userName, password)

        then:
        thrown(expectedException)

        where:
        url              | userName | password | expectedException
        null             | "user"   | "pass"   | NullPointerException
        "jdbc://goo/bar" | null     | "pass"   | NullPointerException
        "jdbc://goo/bar" | "user"   | null     | NullPointerException
        ""               | "user"   | "pass"   | IllegalArgumentException
        "   "            | "user"   | "pass"   | IllegalArgumentException
        "jdbc://goo/bar" | "   "    | "pass"   | IllegalArgumentException
        "jdbc://goo/bar" | "user"   | "   "    | IllegalArgumentException
    }
}
