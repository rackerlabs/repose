package com.rackspace.papi.components.cnorm.normalizer;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.components.normalization.config.MediaType;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

public class MediaTypeNormalizer {

    private static final Pattern VARIANT_EXTRACTOR_REGEX = Pattern.compile("/([^/.]+)?(\\.[^/?#]+)");
    private static final int VARIANT_EXTENSION_GROUP = 2;
    
    private final List<MediaType> knownMediaTypes;
    private final MediaType preferredMediaType;

    public MediaTypeNormalizer(List<MediaType> knownMediaTypes) {
        this.knownMediaTypes = knownMediaTypes;

        preferredMediaType = getPreferredMediaType(knownMediaTypes);
    }

    private MediaType getPreferredMediaType(List<MediaType> mediaTypes) {
        for (MediaType mediaType : knownMediaTypes) {
            if (mediaType.isPreferred()) {
                return mediaType;
            }
        }

        //TODO:Log let the user know that the preferred media type will be the first element in the media type list        
        return mediaTypes.size() > 0 ? mediaTypes.get(0) : null;
    }

    public void normalizeContentMediaType(HttpServletRequest request, FilterDirector director) {
        // The preferred media type should only be null if there were no media types specified
        if (preferredMediaType != null) {
            final boolean requestHasAcceptHeader = request.getHeader(CommonHttpHeader.ACCEPT.headerKey()) != null;
            final MediaType requestedVariantMediaType = getMediaTypeForVariant(request);
            
            if (requestedVariantMediaType != null) {
                director.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.headerKey(), requestedVariantMediaType.getName());
            } else if (!requestHasAcceptHeader) {
                director.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.headerKey(), preferredMediaType.getName());
            }
        }
    }

    public MediaType getMediaTypeForVariant(HttpServletRequest request) {
        final Matcher variantMatcher = VARIANT_EXTRACTOR_REGEX.matcher(request.getRequestURI());

        if (variantMatcher.find()) {
            final String requestedVariant = variantMatcher.group(VARIANT_EXTENSION_GROUP);
            
            for (MediaType mediaType : knownMediaTypes) {
                final String variantExtension = formatVariant(mediaType.getVariantExtension());

                if (StringUtilities.isNotBlank(variantExtension) && requestedVariant.equalsIgnoreCase(variantExtension)) {
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
