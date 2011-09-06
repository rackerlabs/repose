package com.rackspace.papi.commons.util.http;

import com.rackspace.papi.commons.util.http.media.MediaRange;

public interface RequestHeaderInfo {

    MediaRange getPreferedMediaRange();

    boolean hasMediaRange(MediaRange targetRange);
}
