<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:httpx="http://docs.openrepose.org/repose/httpx/v1.0"
    version="1.0">

    <xsl:output method="html"/>
    <xsl:param name="input-headers-uri" />
    <xsl:param name="input-query-uri" />
    <xsl:param name="input-request-uri" />
    <xsl:param name="output-headers-uri" />
    <xsl:param name="output-query-uri" />
    <xsl:param name="output-request-uri" />

    <xsl:variable name="headersDoc" select="doc($input-headers-uri)"/>
    <xsl:variable name="queryDoc" select="doc($input-query-uri)"/>
    <xsl:variable name="requestDoc" select="doc($input-request-uri)"/>

    <xsl:template match="/">
        <xsl:copy-of select="."/>
        <xsl:apply-templates select="$headersDoc/*"/>
        <xsl:apply-templates select="$queryDoc/*"/>
        <xsl:apply-templates select="$requestDoc/*" mode="request" />
    </xsl:template>

    <xsl:template match="httpx:request-information" mode="request">
        <!-- Overwrite the request URI and URL to the hard coded values below -->
        <xsl:result-document method="xml" include-content-type="no" href="{$output-request-uri}">
            <xsl:element name="httpx:request-information">
                <xsl:element name="httpx:uri">/test/request/</xsl:element>
                <xsl:element name="httpx:url">http://127.0.0.1:8088/test/request/</xsl:element>
            </xsl:element>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="httpx:headers">
        <!-- we could specify the href as $output-headers-uri as well.  Here we use repose:output:headers.xml -->
        <xsl:result-document method="xml" include-content-type="no" href="repose:output:headers.xml">
            <httpx:headers>
              <xsl:apply-templates/>
            </httpx:headers>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="httpx:request">
      <httpx:request>
        <httpx:header name="translation-header" value="test" quality="0.5"/>
        <xsl:apply-templates/>
      </httpx:request>
    </xsl:template>

    <xsl:template match="httpx:response">
      <httpx:response>
        <httpx:header name="translation-header" value="response" quality="0.5"/>
        <xsl:apply-templates/>
      </httpx:response>
    </xsl:template>

    <xsl:template match="httpx:header">
        <!-- If a header name begins with 'test', then ignore it.  Otherwise, write it to the output headers document -->
        <xsl:if test="not(starts-with(@name,'test'))">
          <xsl:if test="not(starts-with(@name,'x-'))">
            <xsl:element name="httpx:header">
	      <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
	      <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
	      <xsl:attribute name="quality"><xsl:value-of select="@quality"/></xsl:attribute>
            </xsl:element>
          </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template match="httpx:parameters">
        <!-- we could specify the href as $output-query-uri as well -->
        <xsl:result-document method="xml" include-content-type="no" href="repose:output:query.xml">
            <httpx:parameters>
                <xsl:apply-templates/>
            </httpx:parameters>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="httpx:parameter">
        <!-- If a query parameter name begins with 'test', then ignore it.  Otherwise, write it to the output query document -->
        <xsl:if test="not(starts-with(@name,'test'))">
            <xsl:element name="httpx:parameter">
                <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
            </xsl:element>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
