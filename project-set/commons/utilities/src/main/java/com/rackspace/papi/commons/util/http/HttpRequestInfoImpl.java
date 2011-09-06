package com.rackspace.papi.commons.util.http;

import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.servlet.RequestMediaRangeInterrogator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class HttpRequestInfoImpl implements HttpRequestInfo {

    private static List<MediaRange> getMediaRanges(HttpServletRequest request) {
        return RequestMediaRangeInterrogator.interrogate(request.getRequestURI(), request.getHeader(CommonHttpHeader.ACCEPT.headerKey()));
    }
    
    private final List<MediaRange> acceptMediaRange;
    private final MediaRange preferedMediaRange;
    private final String uri;
    private final String url;

    public HttpRequestInfoImpl(HttpServletRequest request) {
        this(getMediaRanges(request), request.getRequestURI(), request.getRequestURL().toString());
    }

    public HttpRequestInfoImpl(List<MediaRange> acceptMediaRange, String uri, String url) {
        this.preferedMediaRange = MediaRangeParser.getPerferedMediaRange(acceptMediaRange);
        this.acceptMediaRange = acceptMediaRange;
        this.uri = uri;
        this.url = url;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public MediaRange getPreferedMediaRange() {
        return preferedMediaRange;
    }

    @Override
    public boolean hasMediaRange(MediaRange targetRange) {
        for (MediaRange requestedRange : acceptMediaRange) {
            if (requestedRange.equals(targetRange)) {
                return true;
            }
        }

        return false;
    }
}
