package com.rackspace.papi.components.versioning.util.http;

import com.rackspace.papi.commons.util.http.media.MediaRange;

// NOTE: This does not belong in util - this is a domain object for versioning only
public interface RequestHeaderInfo {

    MediaRange getPreferedMediaRange();

    boolean hasMediaRange(MediaRange targetRange);
    
    String getHost();
}
