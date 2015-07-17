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

import org.apache.commons.lang3.StringUtils;
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

    public static Message filter(List<Message> messages, String mediaType, List<MediaType> contentTypes) {
        // matchMatch is RETURNED INSTANTLY
        Message matchWild = null;
        Message matchBlank = null;
        Message wildMatch = null;
        Message wildWild = null;
        Message wildBlank = null;
        Message blankMatch = null;
        Message blankWild = null;
        Message blankBlank = null;

        if (messages != null && mediaType != null && contentTypes != null) {
            for (MediaType contentType : contentTypes) {
                for (Message message : messages) {
                    final String messageMediaType = message.getMediaType();
                    final String messageContentType = message.getContentType();
                    if (StringUtils.equalsIgnoreCase(messageMediaType, mediaType)) {
                        if (StringUtils.equalsIgnoreCase(messageContentType, contentType.getValue())) {
                            return message;
                        } else if (matchWild == null
                                && StringUtils.equalsIgnoreCase(messageContentType, MimeType.WILDCARD.getMimeType())) {
                            matchWild = message;
                        } else if (matchBlank == null && (
                                StringUtils.isBlank(messageContentType) ||
                                        StringUtils.equalsIgnoreCase(messageContentType, MimeType.TEXT_PLAIN.getMimeType())
                        )) {
                            matchBlank = message;
                        }
                    } else if (StringUtils.equalsIgnoreCase(messageMediaType, MimeType.WILDCARD.getMimeType())) {
                        if (wildMatch == null
                                && StringUtils.equalsIgnoreCase(messageContentType, contentType.getValue())) {
                            wildMatch = message;
                        } else if (wildWild == null
                                && StringUtils.equalsIgnoreCase(messageContentType, MimeType.WILDCARD.getMimeType())) {
                            wildWild = message;
                        } else if (wildBlank == null && (
                                StringUtils.isBlank(messageContentType) ||
                                        StringUtils.equalsIgnoreCase(messageContentType, MimeType.TEXT_PLAIN.getMimeType())
                        )) {
                            wildBlank = message;
                        }
                    } else if (StringUtils.isBlank(messageMediaType)) {
                        if (blankMatch == null
                                && StringUtils.equalsIgnoreCase(messageContentType, contentType.getValue())) {
                            blankMatch = message;
                        } else if (blankWild == null
                                && StringUtils.equalsIgnoreCase(messageContentType, MimeType.WILDCARD.getMimeType())) {
                            blankWild = message;
                        } else if (blankBlank == null && (
                                StringUtils.isBlank(messageContentType) ||
                                        StringUtils.equalsIgnoreCase(messageContentType, MimeType.TEXT_PLAIN.getMimeType())
                        )) {
                            blankBlank = message;
                        }
                    }
                }
            }
        }
        return matchWild != null ? matchWild :
                wildMatch != null ? wildMatch :
                        wildWild != null ? wildWild :
                                matchBlank != null ? matchBlank :
                                        blankMatch != null ? blankMatch :
                                                wildBlank != null ? wildBlank :
                                                        blankWild != null ? blankWild :
                                                                blankBlank;
    }
}
