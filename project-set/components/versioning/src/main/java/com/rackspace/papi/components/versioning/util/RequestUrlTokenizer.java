package com.rackspace.papi.components.versioning.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/14/11
 * Time: 3:01 PM
 */
public final class RequestUrlTokenizer {
    private static final String SERVICE_ROOT_HREF_REGEX = "http[s]?://[^/]+/";
    private static final String VERSION_ID_REGEX = "(?:http[s]?://[^/]+/)([^/]+)";
    private static final String RESOURCE_REGEX = "(?:http[s]?://[^/]+/)(?:[^/]+)(/.*$)";
    private static final String RESOURCE_WITHOUT_VERSION_REGEX = "(?:http[s]?://[^/]+)(.*$)";
    private static final Pattern SERVICE_ROOT_HREF_PATTERN = Pattern.compile(SERVICE_ROOT_HREF_REGEX);
    private static final Pattern VERSION_ID_PATTERN = Pattern.compile(VERSION_ID_REGEX);
    private static final Pattern RESOURCE_WITHOUT_VERSION_PATTERN = Pattern.compile(RESOURCE_WITHOUT_VERSION_REGEX);
    private static final Pattern RESOURCE_PATTERN = Pattern.compile(RESOURCE_REGEX);
    private String serviceRootHref = "";
    private String versionId = "";
    private String resource = "";

    private RequestUrlTokenizer(String serviceRootHref, String versionId, String resource) {
        this.serviceRootHref = serviceRootHref;
        this.versionId = versionId;
        this.resource = resource;
    }

    public static RequestUrlTokenizer tokenize(String requestUrl, Set<String> versionIds) {
        if (versionIds == null) {
            throw new IllegalArgumentException("versionIds must be provided!");
        }

        String serviceRootHref = "";
        String versionId = "";
        String resource = "";

        Matcher matcher;

        matcher = SERVICE_ROOT_HREF_PATTERN.matcher(requestUrl);
        if (matcher.find()) {
            serviceRootHref = matcher.group(0);
        }

        boolean hasVersionInfo = false;
        matcher = VERSION_ID_PATTERN.matcher(requestUrl);
        if (matcher.find() && hasVersionInfo(matcher.group(1), versionIds)) {
            versionId = matcher.group(1);
            hasVersionInfo = true;
        }

        matcher = hasVersionInfo
                ? RESOURCE_PATTERN.matcher(requestUrl)
                : RESOURCE_WITHOUT_VERSION_PATTERN.matcher(requestUrl);
        if (matcher.find() && !matcher.group(1).equals("/")) {
            resource = matcher.group(1);
        }

        return new RequestUrlTokenizer(serviceRootHref, versionId, resource);
    }

    public static boolean hasVersionInfo(String versionToken, Set<String> versionIds) {
        boolean found = false;

        for (String versionId: versionIds) {
            if (versionId.equals(versionToken)) {
                found = true;
                break;
            }
        }

        return found;
    }

    public String getServiceRootHref() {
        return serviceRootHref;
    }

    public String getVersionId() {
        return versionId;
    }

    public String getResource() {
        return resource;
    }
}
