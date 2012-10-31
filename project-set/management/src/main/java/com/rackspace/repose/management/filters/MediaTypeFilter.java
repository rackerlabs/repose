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

    private static final Map<String, MediaType> mappedMediaTypes = new HashMap<String, MediaType>(2);

    static {
        mappedMediaTypes.put("json", MediaType.APPLICATION_JSON_TYPE);
        mappedMediaTypes.put("xml", MediaType.APPLICATION_XML_TYPE);
    }

    public MediaTypeFilter() {
        super(mappedMediaTypes);
    }
}
