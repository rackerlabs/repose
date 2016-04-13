<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:json="http://www.ibm.com/xmlns/prod/2009/jsonx"
                version="1.0">

    <xsl:output method="xml" encoding="utf-8" indent="no" omit-xml-declaration="no" media-type="application/xml"/>

    <xsl:template name="json:doNameAttr">
        <xsl:if test="local-name(..)!='array' and string-length(@name)>0">
            <xsl:value-of select="concat('&quot;', @name, '&quot;', ':')"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="json:object">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:text>{ </xsl:text>
        <xsl:for-each select="*">
            <xsl:apply-templates select="."/>
            <xsl:if test="position() != last()">
                <xsl:text>, </xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:text> }</xsl:text>
    </xsl:template>

    <xsl:template match="json:array">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:text>[ </xsl:text>
        <xsl:for-each select="*">
            <xsl:apply-templates select="."/>
            <xsl:if test="position() != last()">
                <xsl:text>, </xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:text> ]</xsl:text>
    </xsl:template>

    <xsl:template match="json:string">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:text>"</xsl:text>
        <!-- XXX Need to replace " with &amp;quot; -->
        <xsl:value-of select="normalize-space()"/>
        <xsl:text>"</xsl:text>
    </xsl:template>

    <xsl:template match="json:number">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:value-of select="normalize-space()"/>
    </xsl:template>

    <xsl:template match="json:boolean">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:value-of select="normalize-space()"/>
    </xsl:template>

    <xsl:template match="json:null">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:text>null</xsl:text>
    </xsl:template>

</xsl:stylesheet>