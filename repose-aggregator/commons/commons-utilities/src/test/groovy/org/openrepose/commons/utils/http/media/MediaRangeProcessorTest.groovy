/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.http.media

import org.openrepose.commons.utils.http.header.HeaderValue
import org.openrepose.commons.utils.http.header.HeaderValueImpl
import spock.lang.Specification
import spock.lang.Unroll


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
