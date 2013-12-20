package com.rackspace.papi.components.versioning.util.http;

import com.rackspace.papi.commons.util.http.media.MediaType;

// NOTE: This does not belong in util - this is a domain object for versioning only
public interface RequestHeaderInfo {

    MediaType getPreferedMediaRange();

    boolean hasMediaRange(MediaType targetRange);
    
    String getHost();
}
