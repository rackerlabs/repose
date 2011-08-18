package com.rackspace.papi.commons.util.http.media;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public abstract class VariantParser {

    static final Pattern VARIANT_REGEX = Pattern.compile("[^\\?]*\\.([^\\?]+).*");

    public static MediaType getMediaTypeFromVariant(String variant) {
        final Matcher matcher = VARIANT_REGEX.matcher(variant);
        MediaType mediaType = null;
        
        if (matcher.matches()) {
            if (variant.toLowerCase().contains("xml")) {
                mediaType = MediaType.APPLICATION_XML;
            } else if (variant.toLowerCase().contains("json")) {
                mediaType = MediaType.APPLICATION_JSON;
            }
        }

        return mediaType;
    }
}
