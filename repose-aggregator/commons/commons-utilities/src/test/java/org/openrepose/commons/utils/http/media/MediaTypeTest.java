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
package org.openrepose.commons.utils.http.media;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author fran
 */
public class MediaTypeTest {

    @Test
    public void shouldReturnTrueIfComparingEqualTypes() {
        MimeType mediaType = MimeType.getMatchingMimeType("application/vnd.rackspace.services.a-v1.0+xml");
        MediaType oneMediaRange = new MediaType("application/vnd.rackspace.services.a-v1.0+xml", mediaType, -1);
        MediaType twoMediaRange = new MediaType("application/vnd.rackspace.services.a-v1.0+xml", mediaType, -1);

        assertTrue(oneMediaRange.equals(twoMediaRange));
    }

    @Test
    public void shouldReturnFalseIfComparingADifferentType() {
        MimeType mediaType = MimeType.getMatchingMimeType("application/vnd.rackspace.services.a-v1.0+xml");
        MediaType oneMediaRange = new MediaType("application/vnd.rackspace.services.a-v1.0+xml", mediaType, -1);

        assertFalse(oneMediaRange.equals("another object"));
    }
}
