<?xml version="1.0" encoding="UTF-8"?>

<!-- Atom transfrom -->
<!--
    This transform converts the versions or version document.
    It doesn't make sense to convert a choices document.
-->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:ver="http://docs.openstack.org/common/api/v1.0"
               xmlns:atom="http://www.w3.org/2005/Atom"
               xmlns="http://www.w3.org/2005/Atom"
               exclude-result-prefixes="ver atom"
               version="1.0">

    <xsl:param name="authorName" select="'Rackspace'"/>
    <xsl:param name="authorURI" select="'http://www.rackspace.com/'"/>

    <xsl:output method="xml" version="1.0"
                indent="yes"
                encoding="UTF-8"
                media-type="application/atom+xml"
                omit-xml-declaration="no"/>

    <xsl:variable name="versions" select="//ver:version"/>

    <xsl:template match="/">
         <feed>
             <xsl:apply-templates/>
         </feed>
    </xsl:template>

    <xsl:template match="ver:versions | ver:version">
        <xsl:variable name="id">
            <xsl:call-template name="id">
                <xsl:with-param name="version" select="$versions[1]"/>
                <xsl:with-param name="base" select="local-name() != 'version'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="title">
            <xsl:choose>
                <xsl:when test="local-name() = 'versions'">
                    <xsl:text>Available API Versions</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>About This Version</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <title type="text"><xsl:value-of select="$title"/></title>
        <updated><xsl:call-template name="latest-time"/></updated>
        <id><xsl:value-of select="$id"/></id>
        <author>
            <name><xsl:value-of select="$authorName"/></name>
            <uri><xsl:value-of select="$authorURI"/></uri>
        </author>
        <link rel="self" href="{$id}"/>
        <xsl:apply-templates select="$versions" mode="multiple">
            <xsl:sort select="@updated" order="descending"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="ver:version" mode="multiple">
        <entry>
            <id><xsl:call-template name="id"/></id>
            <title type="text">Version <xsl:value-of select="@id"/></title>
            <updated><xsl:value-of select="@updated"/></updated>
            <xsl:apply-templates />
            <content type="text">
                <xsl:text>Version </xsl:text>
                <xsl:value-of select="@id"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="@status"/>
                <xsl:text> (</xsl:text>
                <xsl:value-of select="@updated"/>
                <xsl:text>)</xsl:text>
            </content>
        </entry>
    </xsl:template>

    <xsl:template match="atom:link">
        <xsl:element name="link">
            <xsl:copy-of select="@*"/>
        </xsl:element>
    </xsl:template>

    <xsl:template name="latest-time">
       <xsl:for-each select="$versions">
           <xsl:sort select="@updated" order="descending"/>
           <xsl:if test="position() = 1">
               <xsl:value-of select="@updated"/>
           </xsl:if>
       </xsl:for-each>
    </xsl:template>

    <xsl:template name="id">
        <xsl:param name="version" select="."/>
        <xsl:param name="base" select="false()"/>
        <xsl:variable name="href">
            <xsl:choose>
                <xsl:when test="$version/atom:link[@rel='alternate']">
                    <xsl:value-of select="$version/atom:link[@rel='alternate']/@href"/>
                </xsl:when>
                <xsl:when test="$version/atom:link[@rel='self']">
                    <xsl:value-of select="$version/atom:link[@rel='self']/@href"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:message>version-atom.xsl:  Expected link with rel=alternate or rel=self!</xsl:message>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$base">
                <xsl:value-of select="substring-before ($href, string($version/@id))"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$href"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:value-of select="normalize-space(.)"/>
    </xsl:template>
</xsl:transform>
