package com.rackspace.papi.commons.util.http.media;

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
