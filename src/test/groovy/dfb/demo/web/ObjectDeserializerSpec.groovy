package dfb.demo.web

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import dfb.demo.dto.AuthorDto
import spock.lang.Specification

import java.lang.reflect.Modifier

class ObjectDeserializerSpec extends Specification {

    def "creates instance with default no-arg constructor assigning new ObjectMapper"() {
        given:
        def deserializer = new ObjectDeserializer()
        def json = '{"name":"Kent Beck"}'

        when:
        AuthorDto dto = deserializer.deserialize(json, AuthorDto)

        then:
        dto.name() == "Kent Beck"
    }

    def "throws NullPointerException if ObjectMapper is null"() {
        when:
        new ObjectDeserializer(null)

        then:
        def e = thrown(NullPointerException)
        e.message.contains("objectMapper")
    }

    def "exposes only one declared method: deserialize"() {
        given:
        def declaredMethods = ObjectDeserializer.declaredMethods.findAll {
            !it.synthetic && Modifier.isPublic(it.modifiers)
        }

        expect:
        declaredMethods.size() == 1
        declaredMethods[0].name == "deserialize"
        declaredMethods[0].parameterTypes == [String, Class] as Class<?>[]
        declaredMethods[0].returnType == Object
    }

    def "deserializes JSON string to target class"() {
        given:
        def deserializer = new ObjectDeserializer(new ObjectMapper())
        def json = '{"name":"Martin Fowler"}'

        when:
        AuthorDto dto = deserializer.deserialize(json, AuthorDto)

        then:
        dto.name() == "Martin Fowler"
    }

    def "deserializes 'null' JSON to null object"() {
        given:
        def deserializer = new ObjectDeserializer(new ObjectMapper())

        when:
        def result = deserializer.deserialize("null", AuthorDto)

        then:
        result == null
    }

    def "wraps ObjectMapper exception in DeserializationException on invalid JSON"() {
        given:
        def deserializer = new ObjectDeserializer(new ObjectMapper())

        when:
        deserializer.deserialize("{malformed json", AuthorDto)

        then:
        def e = thrown(DeserializationException)
        e.message.contains("Failed to deserialize JSON")
        e.cause instanceof JsonProcessingException
    }
}
