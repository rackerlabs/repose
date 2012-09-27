package com.rackspace.papi.components.unorm.normalizer;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.components.uri.normalization.config.MediaType;
import com.rackspace.papi.filter.logic.FilterDirector;
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

    public static String formatVariant(String variant) {
        return variant == null ? null : variant.startsWith(".") ? variant : "." + variant;
    }
}
