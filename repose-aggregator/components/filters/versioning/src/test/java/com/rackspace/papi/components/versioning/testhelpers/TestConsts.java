package com.rackspace.papi.components.versioning.testhelpers;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/10/11
 * Time: 2:14 PM
 */
public interface TestConsts {
    static final String REQUEST_URI = "/the/target/resource/";
    static final String SERVICE_ROOT_HREF = "http://rackspacecloud.com/";
    static final String REQUEST_URL_WITHOUT_VERSION = SERVICE_ROOT_HREF + REQUEST_URI;
    static final String VERSION_ID = "v1.1";
    static final String REQUEST_URL_WITH_VERSION = SERVICE_ROOT_HREF + VERSION_ID + REQUEST_URI;
}
