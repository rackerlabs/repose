package org.openrepose.filters.cnorm.normalizer;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.HeaderManager;
import org.openrepose.filters.cnorm.config.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrepose.commons.utils.http.CommonHttpHeader.ACCEPT;

public class MediaTypeNormalizer {
    private static final Logger LOG = LoggerFactory.getLogger(MediaTypeNormalizer.class);
    private static final Pattern VARIANT_EXTRACTOR_REGEX = Pattern.compile("((\\.)[^\\d][\\w]*)");
    private static final int VARIANT_EXTENSION_GROUP = 1;

    private final List<MediaType> configuredMediaTypes;
    private final MediaType preferredMediaType;

    public MediaTypeNormalizer(List<MediaType> configuredMediaTypes) {
        this.configuredMediaTypes = configuredMediaTypes;

        preferredMediaType = getPreferredMediaType(configuredMediaTypes);
    }

    private MediaType getPreferredMediaType(List<MediaType> mediaTypes) {
        MediaType prefMediaType = !mediaTypes.isEmpty() ? mediaTypes.get(0) : null;

        for (MediaType mediaType : configuredMediaTypes) {
            if (mediaType.isPreferred()) {
                prefMediaType = mediaType;
                break;
            }
        }

        if (prefMediaType != null && !prefMediaType.isPreferred()) {
            LOG.info("No preferred media type specified in the content normalization configuration.  Using the first in the list.");
        }

        return prefMediaType;
    }

    public void normalizeContentMediaType(HttpServletRequest request, FilterDirector director) {
        // The preferred media type should only be null if there were no media types specified
        if (preferredMediaType != null) {
            //Using request.getHeader(name) seems to return slightly different things.
            //Using the getHeaders, gives me all of the values for that header, and so if I have more elements before
            // Asking for the first one, I certainly have at least one. I thought it better to be consistent
            final boolean requestHasAcceptHeader = request.getHeaders(ACCEPT.toString()).hasMoreElements();
            final MediaType requestedVariantMediaType = getMediaTypeForVariant(request, director);
            HeaderManager headerManager = director.requestHeaderManager();

            if (requestedVariantMediaType != null) {
                headerManager.putHeader(ACCEPT.toString(), requestedVariantMediaType.getName());
            } else if (!requestHasAcceptHeader) {
                headerManager.putHeader(ACCEPT.toString(), preferredMediaType.getName());
            } else {
                //We have an Accept header, lets see if it contains something we're looking for
                Enumeration<String> headerEnumeration = request.getHeaders(ACCEPT.toString());
                List<String> sanitizedAcceptHeaders = new LinkedList<String>();

                //doing a map on this, because we don't actually have map :(
                //Making sure that for each item in the list, we sanitize the accept header.
                // Eventually we'll pay attention to the ;q=.1 or whatev
                //Also build up a collection of the original ones, so we can compare things
                while (headerEnumeration.hasMoreElements()) {
                    String replaced = headerEnumeration.nextElement().replaceAll(";.*", ""); //Stripping off any ;stuff
                    sanitizedAcceptHeaders.add(replaced);
                }

                //If we have an acceptable media type that's in this list, use it, based on exact match
                //If none match, use preferred
                String toUse = null;
                for (MediaType mt : configuredMediaTypes) {
                    if (sanitizedAcceptHeaders.contains(mt.getName())) {
                        //use the one it contains, and we're done
                        toUse = mt.getName();
                        break;
                    }
                }
                if (toUse == null) {
                    //this means we didn't find one
                    //Use the preferred one
                    toUse = preferredMediaType.getName();
                }

                //Only actually change it, if we're needing to change it.
                //If the request header is different than what we're going to use, change it
                //This is non trivial thanks to a crap collections framework
                //It's more complicated than its worth. The Enumeration that it returns is a pain in the butt to use.
                //The code to rebuild the "is this different from the original accepts header?" is not worth it
                headerManager.putHeader(ACCEPT.toString(), toUse);
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
