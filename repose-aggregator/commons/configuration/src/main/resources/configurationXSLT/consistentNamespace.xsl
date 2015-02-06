<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:ns="http://docs.openrepose.org/ds/namespace-fixing"
                version="2.0">

    <xsl:variable name="namespaceMap">
        <ns:entries>
            <!-- exhaustive list of all repose xsd URIs translated to their new, more consistent ones -->
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/add-header/v1.0">http://docs.openrepose.org/repose/add-header/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/client-auth/http-basic/v1.0">http://docs.openrepose.org/repose/client-auth/http-basic/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/client-auth/v1.0">http://docs.openrepose.org/repose/client-auth/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/container/v2.0">http://docs.openrepose.org/repose/container/v2.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/content-compression/v1.0">http://docs.openrepose.org/repose/content-compression/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/content-normalization/v1.0">http://docs.openrepose.org/repose/content-normalization/v1.0</ns:entry>
            <ns:entry key="http://openrepose.org/repose/destination-router/v1.0">http://docs.openrepose.org/repose/destination-router/v1.0</ns:entry>
            <ns:entry key="http://openrepose.org/repose/distributed-datastore/v1.0">http://docs.openrepose.org/repose/distributed-datastore/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/header-identity/v1.0">http://docs.openrepose.org/repose/header-identity/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/header-id-mapping/v1.0">http://docs.openrepose.org/repose/header-id-mapping/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/header-normalization/v1.0">http://docs.openrepose.org/repose/header-normalization/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/header-translation/v1.0">http://docs.openrepose.org/repose/header-translation/v1.0</ns:entry>
            <ns:entry key="http://docs.openrepose.org/highly-efficient-record-processor/v1.0">http://docs.openrepose.org/repose/highly-efficient-record-processor/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/http-connection-pool/v1.0">http://docs.openrepose.org/repose/http-connection-pool/v1.0</ns:entry>
            <ns:entry key="http://openrepose.org/repose/httpx/v1.0">http://docs.openrepose.org/repose/httpx/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/ip-identity/v1.0">http://docs.openrepose.org/repose/ip-identity/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/metrics/v1.0">http://docs.openrepose.org/repose/metrics/v1.0</ns:entry>
            <ns:entry key="http://openrepose.org/components/openstack-identity/auth-z/v1.0">http://docs.openrepose.org/repose/openstack-identity/auth-z/v1.0</ns:entry>
            <ns:entry key="http://docs.openrepose.org/openstack-identity-v3/v1.0">http://docs.openrepose.org/repose/openstack-identity-v3/v1.0</ns:entry>
            <ns:entry key="http://docs.openrepose.org/rackspace-auth-user/v1.0">http://docs.openrepose.org/repose/rackspace-auth-user/v1.0</ns:entry>
            <ns:entry key="http://docs.openrepose.org/rackspace-identity-basic-auth/v1.0">http://docs.openrepose.org/repose/rackspace-identity-basic-auth/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/rate-limiting/v1.0">http://docs.openrepose.org/repose/rate-limiting/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/response-messaging/v1.0">http://docs.openrepose.org/repose/response-messaging/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/slf4j-http-logging/v1.0">http://docs.openrepose.org/repose/slf4j-http-logging/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/system-model/v2.0">http://docs.openrepose.org/repose/system-model/v2.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/translation/v1.0">http://docs.openrepose.org/repose/translation/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/uri-identity/v1.0">http://docs.openrepose.org/repose/uri-identity/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/uri-normalization/v1.0">http://docs.openrepose.org/repose/uri-normalization/v1.0</ns:entry>
            <ns:entry key="http://docs.api.rackspacecloud.com/repose/uri-stripper/v1.0">http://docs.openrepose.org/repose/uri-stripper/v1.0</ns:entry>
            <ns:entry key="http://openrepose.org/repose/validator/v1.0">http://docs.openrepose.org/repose/validator/v1.0</ns:entry>
            <ns:entry key="http://docs.rackspacecloud.com/repose/versioning/v2.0">http://docs.openrepose.org/repose/versioning/v2.0</ns:entry>
        </ns:entries>
    </xsl:variable>

    <xsl:key name="nsentries" match="ns:entry" use="@key"/>

    <xsl:template match='@*|node()'>
        <xsl:copy>
            <xsl:apply-templates select='@*|node()'/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@xsi:schemaLocation"/>

    <xsl:template match='element()' priority="100">
        <xsl:variable name="namespaceValue" as='xs:string' select="string(namespace-uri())" />
        <xsl:variable name="newNamespace" select="key('nsentries', $namespaceValue, $namespaceMap)/text()"/>

        <xsl:choose>
            <xsl:when test="$newNamespace">
                <xsl:element name='{local-name()}' namespace="{$newNamespace}">
                    <xsl:copy-of select='namespace::*[not(. = $namespaceValue)]' />
                    <xsl:apply-templates select='@* | node()'/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select='@*|node()'/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>