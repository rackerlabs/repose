package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.StringUtilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MediaRange {
    
    private final MediaType mediaType;
    private final Map<String, String> parameters;
    private final String vendorSpecificMediaType;

    public MediaRange(MediaType mediaType) {
        this(mediaType, new HashMap<String, String>(), null);
    }

    public MediaRange(MediaType mediaType, String vendorSpecificMediaType) {
        this(mediaType, new HashMap<String, String>(), vendorSpecificMediaType);
    }

    public MediaRange(MediaType mediaType, Map<String, String> parameters, String vendorSpecificMediaType) {
        this.mediaType = mediaType;
        this.vendorSpecificMediaType = vendorSpecificMediaType;
        
        this.parameters = new HashMap<String, String>(parameters);
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getVendorSpecificMediaType() {
        return vendorSpecificMediaType;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MediaRange) {
            final MediaRange otherMediaRange = (MediaRange) o;

            if (mediaType.equals(otherMediaRange.mediaType)
                    && StringUtilities.nullSafeEqualsIgnoreCase(vendorSpecificMediaType, otherMediaRange.getVendorSpecificMediaType())
                    && mapsAreSame(parameters, otherMediaRange.getParameters())) {

                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.mediaType != null ? this.mediaType.hashCode() : 0);
        hash = 53 * hash + (this.parameters != null ? this.parameters.hashCode() : 0);
        hash = 53 * hash + (this.vendorSpecificMediaType != null ? this.vendorSpecificMediaType.hashCode() : 0);
        return hash;
    }

    public <T, R> boolean mapsAreSame(Map<T, R> map1, Map<T, R> map2) {
        if (map1 == map2) {
            return true;
        }
        
        if (map1.size() != map2.size()) {
            return false;
        }

        for (Map.Entry<T, R> firstMapEntry : map1.entrySet()) {
            final R secondMapValue = map2.get(firstMapEntry.getKey());

            if (!secondMapValue.equals(firstMapEntry.getValue())) {
                return false;
            }
        }

        return true;
    }
}
