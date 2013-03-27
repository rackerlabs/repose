<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:httpx="http://openrepose.org/repose/httpx/v1.0"
    xmlns:ah="http://openrepose.org/repose/ah-fn"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    version="2.0">

    <xsl:output method="xml"/>
    <xsl:param name="input-headers-uri" />
    <xsl:param name="output-headers-uri" />
    
    <xsl:variable name="headersDoc" select="doc($input-headers-uri)"/>
    
    <xsl:template match="/">
        <xsl:copy-of select="."/>
        <xsl:apply-templates select="$headersDoc/*"/>
    </xsl:template>
    
    <xsl:template match="httpx:headers">
        <xsl:result-document method="xml" include-content-type="no" href="repose:output:headers.xml">
            <httpx:headers>
                <httpx:request>
                  <xsl:element name="httpx:header">
                    <xsl:attribute name="name"><xsl:value-of select="'extra-header'"/></xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="'result'"/></xsl:attribute>
                  </xsl:element>
                  <xsl:apply-templates select="httpx:request//httpx:header" />
                </httpx:request>
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
</xsl:stylesheet>
