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
package org.openrepose.core.services.rms;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.core.services.rms.config.Message;

import java.util.List;

/**
 * @author fran
 */
public final class MessageFilter {

    private MessageFilter() {
    }

    public static Message filterByMediaType(List<Message> messages, MediaType mediaType) {
        Message wildcard = null;

        if (messages != null && mediaType != null) {
            for (Message message : messages) {
                final String messageMediaType = message.getMediaType();

                if (StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, mediaType.getValue())) {
                    return message;
                }

                // A configured wildcard (*/*) will be returned if an exact match is not found
                if (StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, MimeType.WILDCARD.getName())) {
                    wildcard = message;
                }
            }
        }

        return wildcard;
    }

    public static Message filterByMediaType(List<Message> messages, List<MediaType> mediaTypes) {
        Message wildcard = null;

        if (messages != null && mediaTypes != null) {

            for (MediaType mediaType : mediaTypes) {

                for (Message message : messages) {
                    final String messageMediaType = message.getMediaType();


                    if (StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, mediaType.getValue())) {
                        return message;
                    }

                    // A configured wildcard (*/*) will be returned if an exact match is not found
                    if (wildcard == null && StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, MimeType.WILDCARD.getName())) {
                        wildcard = message;
                    }

                }
            }
        }

        return wildcard;

    }
}
