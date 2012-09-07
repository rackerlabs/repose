<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    version="1.0">
    <xsl:output method="html"/>
    <xsl:param name="query"/>
    <xsl:param name="headers" />
    <xsl:variable name="queryDoc" select="document($query)" />
    <xsl:variable name="headersDoc" select="document($headers)" />
    
    <xsl:template match="/">
        Main Content Will Be Here!!!
        <xsl:result-document method="html" include-content-type="no" href="reference:jio:query.html">
            Parameters:
            <ul>
                <xsl:apply-templates select="$queryDoc/params/param"/>
            </ul>
        </xsl:result-document>
        <xsl:result-document method="html" include-content-type="no" href="reference:jio:headers.html">
            Headers:
            <ul>
                <xsl:apply-templates select="$headersDoc/headers/header"/>
            </ul>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="param|header">
        <li><xsl:value-of select="@name" />=<xsl:value-of select="@value"/></li>
    </xsl:template>

</xsl:stylesheet>
