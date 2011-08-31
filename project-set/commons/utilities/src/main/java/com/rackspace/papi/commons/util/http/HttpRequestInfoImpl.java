package com.rackspace.papi.commons.util.http;

import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.servlet.RequestMediaRangeInterrogator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class HttpRequestInfoImpl implements HttpRequestInfo {
    private final List<MediaRange> acceptMediaRange;
    private final MediaRange preferedMediaRange;
    private final String uri;
    private final String url;

    public HttpRequestInfoImpl(HttpServletRequest request) {
        this.uri = request.getRequestURI();
        this.url = request.getRequestURL().toString();
        
        this.acceptMediaRange = RequestMediaRangeInterrogator.interrogate(uri, request.getHeader(CommonHttpHeader.ACCEPT.headerKey()));
        this.preferedMediaRange = MediaRangeParser.getPerferedMediaRange(acceptMediaRange);
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
