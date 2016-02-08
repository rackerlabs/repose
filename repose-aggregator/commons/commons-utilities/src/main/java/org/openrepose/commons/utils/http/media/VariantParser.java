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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public abstract class VariantParser {

    static final Pattern VARIANT_REGEX = Pattern.compile("[^\\?]*\\.([^\\?]+).*");

    private VariantParser() {

    }

    public static MimeType getMediaTypeFromVariant(String variant) {
        final Matcher matcher = VARIANT_REGEX.matcher(variant);
        MimeType mediaType = null;

        if (matcher.matches()) {
            if (variant.toLowerCase().contains("xml")) {
                mediaType = MimeType.APPLICATION_XML;
            } else if (variant.toLowerCase().contains("json")) {
                mediaType = MimeType.APPLICATION_JSON;
            }
        }

        return mediaType;
    }
}
