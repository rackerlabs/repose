package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.SetUtilities;
import com.rackspace.papi.commons.util.StringUtilities;

import java.util.Set;

/**
 *
 * 
 */
public class MediaRange {

    private final MediaType mediaType;
    private final Set<String> parameters;
    private final String vendorSpecificMediaType;

    public MediaRange(MediaType mediaType, Set<String> parameters, String vendorSpecificMediaType) {
        this.mediaType = mediaType;
        this.parameters = parameters;
        this.vendorSpecificMediaType = vendorSpecificMediaType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public Set<String> getParameters() {
        return parameters;
    }

    public String getVendorSpecificMediaType() {
        return vendorSpecificMediaType;
    }

    @Override
    public boolean equals(Object o) {
        boolean equals = false;

        if (o instanceof MediaRange) {
            MediaRange otherMediaRange = (MediaRange) o;

            if ( (SetUtilities.nullSafeEquals(parameters, otherMediaRange.getParameters())) &&
                 (StringUtilities.nullSafeEqualsIgnoreCase(vendorSpecificMediaType, otherMediaRange.getVendorSpecificMediaType())) &&
                 (mediaType.equals(otherMediaRange.mediaType)) ) {
                equals = true;
            }
        }

        return equals;
    }
}
