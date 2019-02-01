package it.manyos.roc


import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(AuthInterceptor)
class AuthInterceptorSpec extends Specification {

    def setup() {
    }

    def cleanup() {

    }

    void "Test demo interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"demo")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
