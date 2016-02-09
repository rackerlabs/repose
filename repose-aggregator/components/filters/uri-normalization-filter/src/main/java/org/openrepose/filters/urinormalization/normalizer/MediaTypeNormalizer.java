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
package org.openrepose.filters.urinormalization.normalizer;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.filters.urinormalization.config.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaTypeNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(MediaTypeNormalizer.class);
    private static final Pattern VARIANT_EXTRACTOR_REGEX = Pattern.compile("((\\.)[^\\d][\\w]*)");
    private static final int VARIANT_EXTENSION_GROUP = 1;
    private final List<MediaType> configuredMediaTypes;
    private final MediaType configuredPreferredMediaType;

    public MediaTypeNormalizer(List<MediaType> configuredMediaTypes) {
        this.configuredMediaTypes = configuredMediaTypes;

        configuredPreferredMediaType = getPreferredMediaType(configuredMediaTypes);
    }

    public static String formatVariant(String variant) {
        return variant == null ? null : variant.startsWith(".") ? variant : "." + variant;
    }

    private MediaType getPreferredMediaType(List<MediaType> mediaTypes) {
        MediaType preferredMediaType = mediaTypes.size() > 0 ? mediaTypes.get(0) : null;

        for (MediaType mediaType : configuredMediaTypes) {
            if (mediaType.isPreferred()) {
                preferredMediaType = mediaType;
                break;
            }
        }

        if (preferredMediaType != null && !preferredMediaType.isPreferred()) {
            LOG.info("No preferred media type specified in the content normalization configuration.  Using the first in the list.");
        }

        return preferredMediaType;
    }

    public void normalizeContentMediaType(HttpServletRequest request, FilterDirector director) {
        // The preferred media type should only be null if there were no media types specified
        if (configuredPreferredMediaType != null) {
            final String acceptHeader = request.getHeader(CommonHttpHeader.ACCEPT.toString());
            final boolean requestHasAcceptHeader = acceptHeader != null;
            final MediaType requestedVariantMediaType = getMediaTypeForVariant(request, director);

            if (requestedVariantMediaType != null) {
                director.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.toString(), requestedVariantMediaType.getName());
            } else if (!requestHasAcceptHeader || MimeType.getMatchingMimeType(acceptHeader).equals(MimeType.WILDCARD)) {
                director.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.toString(), configuredPreferredMediaType.getName());
            }
        }
    }

    public MediaType getMediaTypeForVariant(HttpServletRequest request, FilterDirector director) {
        final Matcher variantMatcher = VARIANT_EXTRACTOR_REGEX.matcher(request.getRequestURI());

        if (variantMatcher.find()) {
            final String requestedVariant = variantMatcher.group(VARIANT_EXTENSION_GROUP);

            for (MediaType mediaType : configuredMediaTypes) {
                final String variantExtension = formatVariant(mediaType.getVariantExtension());

                if (StringUtilities.isNotBlank(variantExtension) && requestedVariant.equalsIgnoreCase(variantExtension)) {
                    final int uriExtensionIndex = request.getRequestURI().lastIndexOf(requestedVariant);
                    final int urlExtensionIndex = request.getRequestURL().lastIndexOf(requestedVariant);

                    if (uriExtensionIndex > 0 && urlExtensionIndex > 0) {
                        final StringBuilder uriBuilder = new StringBuilder(request.getRequestURI());

                        director.setRequestUri(uriBuilder.delete(uriExtensionIndex, uriExtensionIndex + requestedVariant.length()).toString());
                        director.setRequestUrl(request.getRequestURL().delete(urlExtensionIndex, urlExtensionIndex + requestedVariant.length()));
                    }

                    return mediaType;
                }
            }
        }

        return null;
    }
}
