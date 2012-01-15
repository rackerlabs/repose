package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.*;
import com.rackspace.papi.commons.util.http.header.QualityFactorUtility;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.limits.schema.Limits;
import com.rackspace.papi.components.limits.schema.RateLimitList;
import com.rackspace.papi.components.ratelimit.cache.CachedRateLimit;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.components.ratelimit.util.LimitsEntityStreamTransformer;
import com.rackspace.papi.components.ratelimit.util.combine.LimitsTransformPair;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class RateLimiterResponse extends RateLimitingOperation {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimiterResponse.class);
    private static final LimitsEntityStreamTransformer RESPONSE_TRANSFORMER = new LimitsEntityStreamTransformer();
    private final RateLimitCache cache;

    public RateLimiterResponse(RateLimitCache cache, RateLimitingConfiguration cfg) {
        super(cfg);

        this.cache = cache;
    }

    public void writeActiveLimits(RateLimitingRequestInfo requestInfo, FilterDirector filterDirector) {
        final RateLimitList liveRateLimits = new RateLimitListBuilder(
                getCachedRateLimitsByUser(requestInfo.getUserName()),
                getRatelimitsForRole(requestInfo.getFirstUserGroup())).toRateLimitList();

        try {
            final Limits limits = new Limits();
            limits.setRates(liveRateLimits);

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            RESPONSE_TRANSFORMER.entityAsXml(limits, bos);

            writeLimitsResponse(bos.toByteArray(), requestInfo.getRequest(), filterDirector);
        } catch (Exception ex) {
            consumeException(ex, filterDirector);
        }
    }

    public void writeCombinedLimits(RateLimitingRequestInfo requestInfo, ReadableHttpServletResponse response, FilterDirector filterDirector) {
        final RateLimitList liveRateLimits = new RateLimitListBuilder(
                getCachedRateLimitsByUser(requestInfo.getUserName()),
                getRatelimitsForRole(requestInfo.getFirstUserGroup())).toRateLimitList();

        try {
            final LimitsTransformPair transformPair = new LimitsTransformPair(response.getBufferedOutputAsInputStream(), liveRateLimits);

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            RESPONSE_TRANSFORMER.combine(transformPair, bos);

            writeLimitsResponse(bos.toByteArray(), requestInfo.getRequest(), filterDirector);
        } catch (Exception ex) {
            consumeException(ex, filterDirector);
        }
    }

    private void consumeException(Exception ex, FilterDirector filterDirector) {
        LOG.error("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);

        filterDirector.setFilterAction(FilterAction.RETURN);
        filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
    }

    private Map<String, CachedRateLimit> getCachedRateLimitsByUser(String powerProxyUserId) {
        return cache.getUserRateLimits(powerProxyUserId);
    }

    private void writeLimitsResponse(byte[] readableContents, HttpServletRequest request, FilterDirector filterDirector) throws IOException {
        filterDirector.setResponseStatus(HttpStatusCode.OK);
        
        final List<MediaType> mediaRanges = new MediaRangeParser(request.getHeaders(CommonHttpHeader.ACCEPT.toString())).parse();
        final MediaType preferredMediaRange = QualityFactorUtility.choosePreferedHeaderValue(mediaRanges);
        
        // TODO:Review - Possible null guard required for preferredMediaRange
        
        switch (preferredMediaRange.getMimeType()) {
            case APPLICATION_XML:
                filterDirector.getResponseOutputStream().write(readableContents);
                filterDirector.responseHeaderManager().putHeader(CommonHttpHeader.CONTENT_TYPE.toString(), MimeType.APPLICATION_XML.toString());
                break;

            case APPLICATION_JSON:
            default:
                RESPONSE_TRANSFORMER.streamAsJson(new ByteArrayInputStream(readableContents), filterDirector.getResponseOutputStream());
                filterDirector.responseHeaderManager().putHeader(CommonHttpHeader.CONTENT_TYPE.toString(), MimeType.APPLICATION_JSON.toString());
        }
    }
}
