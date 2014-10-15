package org.openrepose.commons.utils.http.media

import org.openrepose.commons.utils.http.header.HeaderValue
import org.openrepose.commons.utils.http.header.HeaderValueImpl
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

    def "media type parameter stripping should not change behavior when no parameters present"() {
        given:
        headerValues = new ArrayList<>()
        headerValues.add(new HeaderValueImpl("application/atom+xml", 0.1))
        mediaRangeProcessor = new MediaRangeProcessor(headerValues);

        when:
        List<MediaType> mediaTypeList = mediaRangeProcessor.process()

        then:
        mediaTypeList.get(0).getMimeType() == MimeType.APPLICATION_ATOM_XML
    }

    def "unknown media type parameters should still be a valid mime type"() {
        given:
        headerValues = new ArrayList<>()
        headerValues.add(new HeaderValueImpl("thing/what", 0.1))
        mediaRangeProcessor = new MediaRangeProcessor(headerValues);

        when:
        List<MediaType> mediaTypeList = mediaRangeProcessor.process()

        then:
        mediaTypeList.get(0).getMimeType() == MimeType.UNKNOWN
    }
}
