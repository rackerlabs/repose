<?xml version="1.0" encoding="UTF-8"?>
<!--
  _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
  Repose
  _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
  Copyright (C) 2010 - 2015 Rackspace US, Inc.
  _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                version="1.0">

    <xsl:variable name="namespaceMap">
        <!-- exhaustive list of all repose xsd URIs translated to their new, more consistent ones -->
        <entry key="http://docs.api.rackspacecloud.com/repose/add-header/v1.0">http://docs.openrepose.org/repose/add-header/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/client-auth/http-basic/v1.0">http://docs.openrepose.org/repose/client-auth/http-basic/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/client-auth/v1.0">http://docs.openrepose.org/repose/client-auth/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/container/v2.0">http://docs.openrepose.org/repose/container/v2.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/content-compression/v1.0">http://docs.openrepose.org/repose/content-compression/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/content-normalization/v1.0">http://docs.openrepose.org/repose/content-normalization/v1.0</entry>
        <entry key="http://openrepose.org/repose/destination-router/v1.0">http://docs.openrepose.org/repose/destination-router/v1.0</entry>
        <entry key="http://openrepose.org/repose/distributed-datastore/v1.0">http://docs.openrepose.org/repose/distributed-datastore/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/header-identity/v1.0">http://docs.openrepose.org/repose/header-identity/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/header-id-mapping/v1.0">http://docs.openrepose.org/repose/header-id-mapping/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/header-normalization/v1.0">http://docs.openrepose.org/repose/header-normalization/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/header-translation/v1.0">http://docs.openrepose.org/repose/header-translation/v1.0</entry>
        <entry key="http://docs.openrepose.org/highly-efficient-record-processor/v1.0">http://docs.openrepose.org/repose/highly-efficient-record-processor/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/http-connection-pool/v1.0">http://docs.openrepose.org/repose/http-connection-pool/v1.0</entry>
        <entry key="http://openrepose.org/repose/httpx/v1.0">http://docs.openrepose.org/repose/httpx/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/ip-identity/v1.0">http://docs.openrepose.org/repose/ip-identity/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/metrics/v1.0">http://docs.openrepose.org/repose/metrics/v1.0</entry>
        <entry key="http://openrepose.org/components/openstack-identity/auth-z/v1.0">http://docs.openrepose.org/repose/openstack-identity/auth-z/v1.0</entry>
        <entry key="http://docs.openrepose.org/openstack-identity-v3/v1.0">http://docs.openrepose.org/repose/openstack-identity-v3/v1.0</entry>
        <entry key="http://docs.openrepose.org/rackspace-auth-user/v1.0">http://docs.openrepose.org/repose/rackspace-auth-user/v1.0</entry>
        <entry key="http://docs.openrepose.org/rackspace-identity-basic-auth/v1.0">http://docs.openrepose.org/repose/rackspace-identity-basic-auth/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/rate-limiting/v1.0">http://docs.openrepose.org/repose/rate-limiting/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/response-messaging/v1.0">http://docs.openrepose.org/repose/response-messaging/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/slf4j-http-logging/v1.0">http://docs.openrepose.org/repose/slf4j-http-logging/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/system-model/v2.0">http://docs.openrepose.org/repose/system-model/v2.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/translation/v1.0">http://docs.openrepose.org/repose/translation/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/uri-identity/v1.0">http://docs.openrepose.org/repose/uri-identity/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/uri-normalization/v1.0">http://docs.openrepose.org/repose/uri-normalization/v1.0</entry>
        <entry key="http://docs.api.rackspacecloud.com/repose/uri-stripper/v1.0">http://docs.openrepose.org/repose/uri-stripper/v1.0</entry>
        <entry key="http://openrepose.org/repose/validator/v1.0">http://docs.openrepose.org/repose/validator/v1.0</entry>
        <entry key="http://docs.rackspacecloud.com/repose/versioning/v2.0">http://docs.openrepose.org/repose/versioning/v2.0</entry>

    </xsl:variable>

    <xsl:template match='@*|node()'>
        <xsl:copy>
            <xsl:apply-templates select='@*|node()'/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@xsi:schemaLocation"/>

    <xsl:template match='node()'>
        <xsl:variable name="namespaceValue" select="string(namespace-uri())"/>
        <xsl:variable name="newNamespace" select="xalan:nodeset($namespaceMap)/entry[@key=$namespaceValue]"/>

        <xsl:choose>
            <xsl:when test="$newNamespace">
                <xsl:element name='{local-name()}' namespace="{$newNamespace}">
                    <xsl:copy-of select='namespace::*[not(. = $newNamespace/@key)]'/>
                    <xsl:apply-templates select='@*'/>
                    <xsl:apply-templates select='node()'/>
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