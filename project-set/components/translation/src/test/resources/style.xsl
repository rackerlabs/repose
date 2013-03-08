<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version="1.0">
    <xsl:output method="html"/>
    <xsl:param name="query"/>
    <xsl:param name="headers" />
    <xsl:variable name="queryDoc" select="document($query)" />
    <xsl:variable name="headersDoc" select="document($headers)" />
    
    <xsl:template match="/">
        Main Content Will Be Here!!!
    </xsl:template>

    <xsl:template match="param|header">
        <li><xsl:value-of select="@name" />=<xsl:value-of select="@value"/></li>
    </xsl:template>

</xsl:stylesheet>
