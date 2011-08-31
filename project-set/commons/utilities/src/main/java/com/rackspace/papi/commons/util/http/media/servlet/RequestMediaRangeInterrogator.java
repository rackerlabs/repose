package com.rackspace.papi.commons.util.http.media.servlet;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.VariantParser;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;

import java.util.LinkedList;
import java.util.List;

public abstract class RequestMediaRangeInterrogator {

    public static List<MediaRange> interrogate(String requestUri, String acceptHeader) {
        final List<MediaRange> ranges = new LinkedList<MediaRange>();
        
        final MediaType mediaType = VariantParser.getMediaTypeFromVariant(requestUri);

        if (mediaType == null) {
            ranges.addAll(MediaRangeParser.getMediaRangesFromAcceptHeader(acceptHeader));

        } else {
            ranges.add(new MediaRange(mediaType));
        }
        
        if (ranges.isEmpty()) {
            ranges.add(new MediaRange(MediaType.UNSPECIFIED));
        }

        return ranges;
    }
}
