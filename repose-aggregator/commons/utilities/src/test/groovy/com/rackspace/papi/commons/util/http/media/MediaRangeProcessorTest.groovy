package com.rackspace.papi.commons.util.http.media

import com.rackspace.papi.commons.util.http.header.HeaderValue
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl
import spock.lang.Specification


class MediaRangeProcessorTest extends Specification {

    MediaRangeProcessor mediaRangeProcessor
    List<? extends HeaderValue> headerValues

    def "media type parameters should be stripped during processing"() {
        given:
        headerValues = new ArrayList<>()
        headerValues.add(new HeaderValueImpl("application/atom+xml; type=entry;charset=utf-8", 0.1))
        mediaRangeProcessor = new MediaRangeProcessor(headerValues);

        when:
        List<MediaType> mediaTypeList = mediaRangeProcessor.process()

        then:
        mediaTypeList.get(0).getMimeType() == MimeType.APPLICATION_ATOM_XML
    }

    def "media type parameters stripping should not change behavior when no parameters present"() {
        given:
        headerValues = new ArrayList<>()
        headerValues.add(new HeaderValueImpl("application/atom+xml", 0.1))
        mediaRangeProcessor = new MediaRangeProcessor(headerValues);

        when:
        List<MediaType> mediaTypeList = mediaRangeProcessor.process()

        then:
        mediaTypeList.get(0).getMimeType() == MimeType.APPLICATION_ATOM_XML
    }
}
