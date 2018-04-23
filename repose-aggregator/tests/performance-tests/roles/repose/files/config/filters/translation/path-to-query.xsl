<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:httpx="http://openrepose.org/repose/httpx/v1.0">
    <!-- Declare the format of the output document -->
    <xsl:output method="xml"/>

    <!-- The params that get populated by the translation filter -->
    <xsl:param name="input-request-uri"/>
    <xsl:param name="input-query-uri"/>
    <xsl:param name="output-request-uri"/>
    <xsl:param name="output-query-uri"/>

    <!-- Assign variables to the request XML documents -->
    <xsl:variable name="requestDoc" select="doc($input-request-uri)"/>
    <xsl:variable name="queryDoc" select="doc($input-query-uri)"/>

    <!-- We parse the penultimate and ultimate path segments here, but don't cover the corner cases (e.g., if the path ends with a '/') -->
    <xsl:variable name="penultimate" select="tokenize($requestDoc//httpx:uri, '/')[position()=last()-1]"/>
    <xsl:variable name="ultimate" select="tokenize($requestDoc//httpx:uri, '/')[position()=last()]"/>

    <!-- Match on the body, copy it, then process the rest of the request by applying templates -->
    <xsl:template match="/">
        <xsl:copy-of select="."/>
        <xsl:apply-templates select="$queryDoc/*"/>
        <xsl:apply-templates select="$requestDoc/*"/>
    </xsl:template>

    <!-- Write the query parameters by copying the existing parameters and adding the two we want -->
    <xsl:template match="httpx:parameters">
        <xsl:result-document method="xml" include-content-type="no" href="{$output-query-uri}">
            <parameters xmlns="http://openrepose.org/repose/httpx/v1.0">
                <xsl:apply-templates/>
                <parameter name="penultimate" value="{$penultimate}"/>
                <parameter name="ultimate" value="{$ultimate}"/>
            </parameters>
        </xsl:result-document>
    </xsl:template>

    <!-- Copy the existing parameters -->
    <xsl:template match="httpx:parameter">
        <xsl:copy/>
    </xsl:template>

    <!-- Remove the path segments from the uri and url that are now represented as query parameters -->
    <xsl:template match="httpx:request-information">
        <xsl:result-document method="xml" include-content-type="no" href="{$output-request-uri}">
            <request-information xmlns="http://openrepose.org/repose/httpx/v1.0">
                <!-- This will remove the penultimate and ultimate path segments that we parsed above from the end of the uri and url -->
                <uri><xsl:value-of select="substring(httpx:uri, 1, string-length(httpx:uri) - string-length(concat('/', $penultimate, '/', $ultimate)))"/>
                </uri>
                <url><xsl:value-of select="substring(httpx:url, 1, string-length(httpx:url) - string-length(concat('/', $penultimate, '/', $ultimate)))"/>
                </url>
            </request-information>
        </xsl:result-document>
    </xsl:template>
</xsl:stylesheet>
