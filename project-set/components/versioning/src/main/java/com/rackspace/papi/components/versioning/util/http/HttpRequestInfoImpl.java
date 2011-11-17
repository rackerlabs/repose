package com.rackspace.papi.components.versioning.util.http;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.servlet.RequestMediaRangeInterrogator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

// NOTE: This does not belong in util - this is a domain object for versioning only
public class HttpRequestInfoImpl implements HttpRequestInfo {

    private static List<MediaRange> getMediaRanges(HttpServletRequest request) {
        return RequestMediaRangeInterrogator.interrogate(request.getRequestURI(), request.getHeader(CommonHttpHeader.ACCEPT.getHeaderKey()));
    }
    private final List<MediaRange> acceptMediaRange;
    private final MediaRange preferedMediaRange;
    private final String uri;
    private final String url;
    private final String host;

    public HttpRequestInfoImpl(HttpServletRequest request) {
        this(getMediaRanges(request), request.getRequestURI(), request.getRequestURL().toString(), request.getHeader(CommonHttpHeader.HOST.getHeaderKey()));
    }

    public HttpRequestInfoImpl(List<MediaRange> acceptMediaRange, String uri, String url, String host) {
        this.preferedMediaRange = MediaRangeParser.getPerferedMediaRange(acceptMediaRange);
        this.acceptMediaRange = acceptMediaRange;
        this.uri = uri;
        this.url = url;
        this.host = host;
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

    @Override
    public String getHost() {
        return host;
    }
}
