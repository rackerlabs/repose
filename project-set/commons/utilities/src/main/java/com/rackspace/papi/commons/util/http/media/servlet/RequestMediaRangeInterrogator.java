package com.rackspace.papi.commons.util.http.media.servlet;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.VariantParser;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;

import java.util.List;

/**
 *
 * 
 */
public abstract class RequestMediaRangeInterrogator {

    public static MediaRange interrogate(String requestUri, String acceptHeader) {
        MediaRange mediaRange = new MediaRange(MediaType.UNKNOWN, null, null);
        
        final MediaType mediaType = VariantParser.getMediaTypeFromVariant(requestUri);

        if (mediaType == null) {
            List<MediaRange> mediaRanges = MediaRangeParser.getMediaRangesFromAcceptHeader(acceptHeader);

            if (!mediaRanges.isEmpty()) {
                mediaRange = mediaRanges.get(0);
            }
        } else {
            mediaRange = new MediaRange(mediaType, null, null);
        }

        return mediaRange;
    }
}
