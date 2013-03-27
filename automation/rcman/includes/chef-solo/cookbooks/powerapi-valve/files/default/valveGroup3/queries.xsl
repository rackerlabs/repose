<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:httpx="http://openrepose.org/repose/httpx/v1.0"
    xmlns:ah="http://openrepose.org/repose/ah-fn"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    version="2.0">

    <xsl:output method="xml"/>
    <xsl:param name="input-query-uri" />
    <xsl:param name="output-query-uri" />
    
    <xsl:variable name="queryDoc" select="doc($input-query-uri)"/>
    
    <xsl:template match="/">
        <xsl:copy-of select="."/>
        <xsl:apply-templates select="$queryDoc/*"/>
    </xsl:template>
    
    <xsl:template match="httpx:parameters">
        <xsl:result-document method="xml" include-content-type="no" href="repose:output:query.xml">
            <httpx:parameters>
                <xsl:element name="httpx:parameter">
                  <xsl:attribute name="name"><xsl:value-of select="'extra-query'"/></xsl:attribute>
                  <xsl:attribute name="value"><xsl:value-of select="'result'"/></xsl:attribute>
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
