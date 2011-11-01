package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.AcceptHeader;
import com.rackspace.httpx.RequestHeaders;
import com.rackspace.httpx.SimpleParameter;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.httpx.ObjectFactoryUser;

import java.util.List;
import java.util.Map;

/**
 * @author fran
 */
public class AcceptHeaderNode extends ObjectFactoryUser implements Node {
    private final String requestAcceptHeader;
    private final RequestHeaders headers;

    public AcceptHeaderNode(String requestAcceptHeader, RequestHeaders headers) {
        this.requestAcceptHeader = requestAcceptHeader;
        this.headers = headers;
    }

    @Override
    public void build() {
        AcceptHeader acceptHeader = objectFactory.createAcceptHeader();

        if (StringUtilities.isNotBlank((requestAcceptHeader))) {
            final List<MediaRange> mediaRanges = MediaRangeParser.getMediaRangesFromAcceptHeader(requestAcceptHeader);

            for (com.rackspace.papi.commons.util.http.media.MediaRange range : mediaRanges) {
                com.rackspace.httpx.MediaRange mediaRange = objectFactory.createMediaRange();

                mediaRange.setType(range.getMediaType().getType());
                mediaRange.setSubtype(range.getMediaType().getSubtype());

                for (Map.Entry<String, String> entry : range.getParameters().entrySet()) {
                    SimpleParameter simpleParameter = objectFactory.createSimpleParameter();

                    simpleParameter.setName(entry.getKey());
                    simpleParameter.setValue(entry.getValue());

                    mediaRange.getParameter().add(simpleParameter);
                }

                acceptHeader.getMediaRange().add(mediaRange);
            }
        }

        headers.setAccept(acceptHeader);
    }
}
