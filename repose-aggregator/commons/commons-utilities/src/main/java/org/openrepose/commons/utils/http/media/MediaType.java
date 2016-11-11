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

import org.openrepose.commons.utils.http.header.HeaderValueImpl;

import java.util.Map;

public class MediaType extends HeaderValueImpl {

    private static final int HASH_CODE_NUM1 = 3;
    private static final int HASH_CODE_NUM2 = 79;
    private final MimeType mimeType;

    public MediaType(MimeType mimeType) {
        this(mimeType.getName(), mimeType, HeaderValueImpl.DEFAULT_QUALITY);
    }

    public MediaType(MimeType mimeType, double quality) {
        this(mimeType.getName(), mimeType, quality);
    }

    public MediaType(String value, MimeType mimeType) {
        this(value, mimeType, HeaderValueImpl.DEFAULT_QUALITY);
    }

    public MediaType(String value, MimeType mimeType, double quality) {
        super(value, quality);

        this.mimeType = mimeType;
    }

    public MediaType(String value, MimeType mediaType, Map<String, String> parameters) {
        super(value, parameters);

        this.mimeType = mediaType;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final MediaType other = (MediaType) obj;

        if (this.mimeType != other.mimeType) {
            return false;
        }

        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hash = HASH_CODE_NUM2 * HASH_CODE_NUM1 + (this.mimeType != null ? this.mimeType.hashCode() : 0);

        return hash + super.hashCode();
    }
}
