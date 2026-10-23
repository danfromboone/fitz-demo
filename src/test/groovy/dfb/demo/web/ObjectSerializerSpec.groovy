package dfb.demo.web

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import dfb.demo.dto.AuthorDto
import spock.lang.Specification

import java.lang.reflect.Modifier

class ObjectSerializerSpec extends Specification {

    def "creates instance with default no-arg constructor assigning new ObjectMapper"() {
        given:
        def serializer = new ObjectSerializer()
        def dto = new AuthorDto("Kent Beck")

        when:
        def json = serializer.serialize(dto)

        then:
        json == '{"name":"Kent Beck"}'
    }

    def "throws NullPointerException if ObjectMapper is null"() {
        when:
        new ObjectSerializer(null)

        then:
        def e = thrown(NullPointerException)
        e.message.contains("objectMapper")
    }

    def "exposes only one declared method: serialize"() {
        given:
        def declaredMethods = ObjectSerializer.declaredMethods.findAll {
            !it.synthetic && Modifier.isPublic(it.modifiers)
        }

        expect:
        declaredMethods.size() == 1
        declaredMethods[0].name == "serialize"
        declaredMethods[0].parameterTypes == [Object] as Class<?>[]
        declaredMethods[0].returnType == String
    }

    def "serializes object to JSON string"() {
        given:
        def serializer = new ObjectSerializer(new ObjectMapper())
        def dto = new AuthorDto("Martin Fowler")

        when:
        def json = serializer.serialize(dto)

        then:
        json == '{"name":"Martin Fowler"}'
    }

    def "serializes null to 'null'"() {
        given:
        def serializer = new ObjectSerializer(new ObjectMapper())

        when:
        def json = serializer.serialize(null)

        then:
        json == "null"
    }

    def "wraps ObjectMapper exception in SerializationException on serialization failure"() {
        given:
        def mapper = Mock(ObjectMapper)
        def cause = new MockJsonProcessingException("Boom")
        mapper.writeValueAsString(_) >> { throw cause }
        def serializer = new ObjectSerializer(mapper)

        when:
        serializer.serialize("test")

        then:
        def e = thrown(SerializationException)
        e.message.contains("Failed to serialize object to JSON")
        e.cause.is(cause)
    }

    def "SerialiazationException alias is an instance of SerializationException"() {
        expect:
        new SerialiazationException("test") instanceof SerializationException
    }

    private static class MockJsonProcessingException extends JsonProcessingException {
        MockJsonProcessingException(String msg) {
            super(msg)
        }
    }
}
