<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                xmlns:httpx="http://docs.openrepose.org/repose/httpx/v1.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:repose="http://wwww.openrepose.org/2013/XMLSchema"
                version="2.0">

    <xsl:output method="xml"/>
    <xsl:param name="input-headers-uri" />
    <xsl:param name="output-headers-uri" />

    <xsl:variable name="headersDoc" select="doc($input-headers-uri)"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="count(//httpx:unknown-content) > 0">
                <xsl:value-of select="//httpx:unknown-content"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="."/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates select="$headersDoc/*"/>
    </xsl:template>

    <xsl:template match="httpx:headers">
        <xsl:result-document method="xml" include-content-type="no" href="repose:output:headers.xml">
            <httpx:headers>
                <httpx:request>
                    <xsl:apply-templates  />
                </httpx:request>
            </httpx:headers>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="httpx:header">
        <xsl:choose>

            <xsl:when test="@name = 'x-onetomany-a'" >
                <xsl:element name="httpx:header">
                    <xsl:attribute name="name"><xsl:value-of select="'x-onetomany-c'"/></xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
                    <xsl:attribute name="quality"><xsl:value-of select="@quality"/></xsl:attribute>
                </xsl:element>
                <xsl:element name="httpx:header">
                    <xsl:attribute name="name"><xsl:value-of select="'x-onetomany-d'"/></xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
                    <xsl:attribute name="quality"><xsl:value-of select="@quality"/></xsl:attribute>
                </xsl:element>
            </xsl:when>
        </xsl:choose>
        <xsl:element name="httpx:header">
            <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
            <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
            <xsl:attribute name="quality"><xsl:value-of select="@quality"/></xsl:attribute>
        </xsl:element>


    </xsl:template>

</xsl:stylesheet>
