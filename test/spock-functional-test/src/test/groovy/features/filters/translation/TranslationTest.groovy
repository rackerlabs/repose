package features.filters.translation

import framework.ReposeValveTest

class TranslationTest extends ReposeValveTest {

    //Start repose once for this particular translation test
    def setupSpec() {

        repose.applyConfigs(
        )
        repose.start()
    }

    def cleanupSpec() {
        repose.stop()
    }

    def "when translating application/xml response"() {

        given:
        //TODO: build handler to return  '<a><remove-me>test</remove-me>somebody</a>' in body


        when:
        //TODO: send request

        then:
        //TODO: validate response does not contain removed element
    }

    def "when translating application/xml response and expanding entities"() {

        given:
        //TODO: build handler to return  '<?xml version="1.0" standalone="no" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  "/etc/passwd"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>' in body

        when:

        //TODO: send request

        then:
        //TODO: validate response does not contain removed element and has expanded entities
    }

    def "when translating application/xhtml+xml response "() {


        given:
        //TODO: build handler to return  <a><remove-me>test</remove-me>somebody</a>

        when:
        //TODO: send request

        then:
        //TODO: validate response has element added

    }

    def "when translating application/json response to xml"() {

        given:
        //TODO: build handler to return  {"field1": "value1", "field2": "value2"}

        when:
        //TODO: send request


        then:
        //TODO: validate response has element added and jsonx elements
    }

    def "when translating application/other"(){

        given:
        //TODO: build handler to return  {"field1": "value1", "field2": "value2"}

        when:
        //TODO: send request


        then:
        //TODO: validate response does not have element added and contains the original json
    }


}