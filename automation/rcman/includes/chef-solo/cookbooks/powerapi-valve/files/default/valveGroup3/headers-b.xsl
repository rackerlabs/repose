<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:httpx="http://openrepose.org/repose/httpx/v1.0"
    xmlns:ah="http://openrepose.org/repose/ah-fn"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    version="2.0">

    <xsl:output method="xml"/>
    <xsl:param name="input-headers-uri" />
    <xsl:param name="input-query-uri" />
    <xsl:param name="input-request-uri" />
    <xsl:param name="output-headers-uri" />
    <xsl:param name="output-query-uri" />
    <xsl:param name="output-request-uri" />
    
    <xsl:variable name="headersDoc" select="doc($input-headers-uri)"/>
    <xsl:variable name="queryDoc" select="doc($input-query-uri)"/>
    <xsl:variable name="requestDoc" select="doc($input-request-uri)"/>
    <xsl:variable name="uri" select="$requestDoc//httpx:uri" />
    <xsl:variable name="url" select="$requestDoc//httpx:url" />
    
    <xsl:template match="/">
        <xsl:copy-of select="."/>
	<!--
        <xsl:result-document method="xml" include-content-type="no" href="{$output-request-uri}">
            <xsl:element name="httpx:request-information">
                <xsl:element name="httpx:uri"><xsl:apply-templates select="$uri" mode="request"/></xsl:element>
                <xsl:element name="httpx:url"><xsl:apply-templates select="$url" mode="request"/></xsl:element>
            </xsl:element>
        </xsl:result-document>
        -->
        <xsl:apply-templates select="$headersDoc/*"/>
        <xsl:apply-templates select="$queryDoc/*"/>
    </xsl:template>
    
    <xsl:template match="httpx:headers">
        <xsl:result-document method="xml" include-content-type="no" href="repose:output:headers.xml">
            <httpx:headers>
                <httpx:request>
                  <xsl:element name="httpx:header">
                    <xsl:attribute name="name"><xsl:value-of select="'translation-b'"/></xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="'b'"/></xsl:attribute>
                  </xsl:element>
                  <xsl:apply-templates select="httpx:request//httpx:header" />
                </httpx:request>
                <httpx:response>
                  <xsl:element name="httpx:header">
                    <xsl:attribute name="name"><xsl:value-of select="'translation-response-b'"/></xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="'response-b'"/></xsl:attribute>
                  </xsl:element>
                  <xsl:apply-templates select="httpx:request//httpx:header" />
                </httpx:response>
            </httpx:headers>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="httpx:header">
        <xsl:element name="httpx:header">
            <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
            <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
            <xsl:attribute name="quality"><xsl:value-of select="@quality"/></xsl:attribute>
        </xsl:element>
    </xsl:template>

    <xsl:template match="httpx:parameters">
        <xsl:result-document method="xml" include-content-type="no" href="repose:output:query.xml">
            <httpx:parameters>
                <xsl:element name="httpx:parameter">
                  <xsl:attribute name="name"><xsl:value-of select="'translation-b'"/></xsl:attribute>
                  <xsl:attribute name="value"><xsl:value-of select="'b'"/></xsl:attribute>
                </xsl:element>
                <xsl:apply-templates/>
            </httpx:parameters>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="httpx:parameter">
        <xsl:element name="httpx:parameter">
            <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
            <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
