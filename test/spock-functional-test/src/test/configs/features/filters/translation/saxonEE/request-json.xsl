<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:map="http://www.w3.org/2005/xpath-functions/map"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns="http://www.w3.org/2005/Atom"
    exclude-result-prefixes="map atom"
    version="3.0">

    <xsl:output method="xml" encoding="UTF-8"/>

    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="atom:entry[atom:content/@type = 'application/json']">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:variable name="json" select="parse-json(atom:content)"/>
            <xsl:variable name="id" select="map:get(map:get($json,'payload'),'id')"/>
            <xsl:choose>
                <xsl:when test="starts-with($id,'9')">
                    <!--
                        We got a match on the ID so create DATACENTER and REGION.
                    -->
                    <category term="DATACENTER=req1"/>
                    <category term="REGION=req"/>
                </xsl:when>
                <xsl:otherwise>
                    <!--
                        No match so copy region and datacenter.
                    -->
                    <xsl:copy-of select="atom:category[starts-with(@term,'DATACENTER')]"/>
                    <xsl:copy-of select="atom:category[starts-with(@term,'REGION')]"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!--
        Don't automatically copy DATA and REGION.
    -->

    <xsl:template match="atom:category[starts-with(@term,'DATACENTER')]"/>
    <xsl:template match="atom:category[starts-with(@term,'REGION')]"/>
</xsl:stylesheet>
