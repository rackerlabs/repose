<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:httpx="http://docs.openrepose.org/repose/httpx/v1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:repose="http://wwww.openrepose.org/2013/XMLSchema" version="2.0">
    <xsl:output method="xml"/>
    <xsl:param name="input-request-uri"/>
    <xsl:param name="output-request-uri"/>
    <xsl:variable name="requestDoc" select="doc($input-request-uri)"/>

    <xsl:template match="httpx:request-information">
        <xsl:result-document method="xml" include-content-type="no" href="{$output-request-uri}">
            <request-information xmlns="http://docs.openrepose.org/repose/httpx/v1.0">
                <xsl:apply-templates />
            </request-information>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="/">
        <xsl:variable name="requestURL" select="doc($input-request-uri)"/>
        <xsl:variable name="uri" select="$requestURL//httpx:uri"/>
        <xsl:variable name="url" select="$requestURL//httpx:url"/>
        <xsl:analyze-string select="$url" regex="^(http://|https://)([^/]+)/([^/]+)/([^/]+)(/?)(.*)">
            <xsl:matching-substring>
                <xsl:result-document method="xml" include-content-type="no" href="{$output-request-uri}">
                    <httpx:request-information>
                        <httpx:uri>
                            <xsl:value-of select="concat(regex-group(3),'/',regex-group(6))"/>
                        </httpx:uri>
                        <httpx:url>
                            <xsl:value-of select="concat(regex-group(1),regex-group(2),'/',regex-group(3),'/',regex-group(6))"/>
                        </httpx:url>
                    </httpx:request-information>
                </xsl:result-document>
            </xsl:matching-substring>
            <xsl:non-matching-substring/>
        </xsl:analyze-string>
        <xsl:apply-templates/>
    </xsl:template>

</xsl:stylesheet>
