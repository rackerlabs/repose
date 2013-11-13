package com.rackspace.repose.management.filters;

import com.sun.jersey.api.container.filter.UriConnegFilter;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 26, 2012
 * Time: 2:09:54 PM
 */
public class MediaTypeFilter extends UriConnegFilter {

    private static final Map<String, MediaType> MAPPED_MEDIA_TYPES = new HashMap<String, MediaType>(2);

    static {
        MAPPED_MEDIA_TYPES.put("json", MediaType.APPLICATION_JSON_TYPE);
        MAPPED_MEDIA_TYPES.put("xml", MediaType.APPLICATION_XML_TYPE);
    }

    public MediaTypeFilter() {
        super(MAPPED_MEDIA_TYPES);
    }
}
