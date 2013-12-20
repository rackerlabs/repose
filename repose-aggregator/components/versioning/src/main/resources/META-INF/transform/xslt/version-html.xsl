<?xml version="1.0" encoding="UTF-8"?>

<!-- XHTML5 Transform -->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:ver="http://docs.openstack.org/api/versioning/v1.0"
               xmlns:atom="http://www.w3.org/2005/Atom"
               xmlns="http://www.w3.org/1999/xhtml"
               exclude-result-prefixes="ver atom"
               version="1.0">
    <xsl:output method="xml" version="1.0"
                encoding="UTF-8"
                media-type="application/xhtml+xml"
                omit-xml-declaration="yes"/>

    <xsl:template match="/">
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html>&#xa;</xsl:text>
        <html lang="en">
            <xsl:apply-templates/>
        </html>
    </xsl:template>

    <xsl:template match="ver:versions | ver:choices">
        <xsl:variable name="title">
            <xsl:choose>
                <xsl:when test="local-name() = 'versions'">
                    <xsl:text>Available API Versions</xsl:text>
                </xsl:when>
                <xsl:when test="local-name() = 'choices'">
                    <xsl:text>Multiple Choices Available</xsl:text>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="httpHead">
            <xsl:with-param name="title" select="$title"/>
        </xsl:call-template>
        <body>
            <table>
                <thead>
                    <tr>
                        <th colspan="4" class="heading"><xsl:value-of select="$title"/></th>
                    </tr>
                    <tr>
                        <th>Link</th>
                        <th>Version</th>
                        <th>Status</th>
                        <th class="last">Media-Types</th>
                    </tr>
                </thead>
                <tbody>
                    <xsl:apply-templates mode="multiple"/>
                </tbody>
            </table>
        </body>
    </xsl:template>

    <xsl:template match="ver:version">
        <xsl:variable name="title" select="'About This Version'"/>
        <xsl:call-template name="httpHead">
            <xsl:with-param name="title" select="$title"/>
        </xsl:call-template>
        <body>
            <table>
                <thead>
                    <tr>
                        <th colspan="4" class="heading"><xsl:value-of select="$title"/></th>
                    </tr>
                    <tr>
                        <th>Version</th>
                        <th>Status</th>
                        <th>Documentation</th>
                        <th class="last">Media-Types</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td class='center'><xsl:call-template name="version"/></td>
                        <td class='center'><xsl:value-of select="@status"/></td>
                        <td>
                            <ul>
                                <xsl:apply-templates select="atom:link[@rel='describedby']" mode="single"/>
                            </ul>
                        </td>
                        <td>
                            <ul>
                                <xsl:apply-templates select="ver:media-types/ver:media-type"/>
                            </ul>
                        </td>
                    </tr>
                </tbody>
            </table>
        </body>
    </xsl:template>

    <xsl:template name="httpHead">
        <xsl:param name="title"/>
        <head>
            <title><xsl:value-of select="$title"/></title>
            <meta charset="UTF-8" />
            <style type="text/css">
                body { background-color: #D3D3D3; }
                table {text-align: center; margin-left:auto; margin-right:auto;
                border: 1px solid; border-collapse:collapse;}
                h1 {text-align: center}
                thead {background-color: black; color: white}
                table,td { border: 1px solid black; background-color: white; }
                th {border-right: 1px solid white; border-left: 1px solid black;
                padding: 5px;}
                td { padding: 15px; text-align: left;}
                td.center { text-align: center; }
                th.last {border-right: 1px solid black;}
                td.last {text-align:left;}
                th.heading {border-right: 1px solid black;
                            border-bottom: 1px solid white;
                            font-size: xx-large;}
                a:link, a:visited, a:active {color:black; background-color:white;
                                             text-decoration:none;}
                a:hover {color:black; background-color:white; text-decoration:underline;}
            </style>
        </head>
    </xsl:template>

    <xsl:template match="ver:version" mode="multiple">
        <xsl:variable name="endpoint" select="atom:link[@rel='self']"/>
        <tr>
            <td>
                <xsl:if test="$endpoint">
                    <xsl:call-template name="link">
                        <xsl:with-param name="in">
                            <xsl:value-of select="$endpoint/@href" />
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>
            </td>
            <td class="center">
                <xsl:call-template name="version"/>
            </td>
            <td class="center"><xsl:value-of select="@status"/></td>
            <td class="last">
                <ul>
                    <xsl:apply-templates/>
                </ul>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="atom:link[@rel='describedby']" mode="single">
        <xsl:variable name="href" select="@href"/>
        <xsl:variable name="type" select="@type"/>
        <li><a href="{$href}">
        <xsl:choose>
            <xsl:when test="$type = 'application/pdf'">
                <xsl:text>Developer Guide</xsl:text>
            </xsl:when>
            <xsl:when test="$type = 'application/vnd.sun.wadl+xml'">
                <xsl:text>WADL</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>Other (</xsl:text>
                <xsl:value-of select="$type"/>
                <xsl:text>)</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        </a></li>
    </xsl:template>

    <xsl:template name="version">
        <xsl:value-of select="@id"/>
        <xsl:if test="@updated">
            <br />
            <xsl:text> (</xsl:text>
            <xsl:choose>
                <xsl:when test="contains(string(@updated),'T')">
                    <xsl:value-of select="substring-before(string(@updated),'T')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@updated"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:text>)</xsl:text>
        </xsl:if>
    </xsl:template>

    <xsl:template name="link">
        <xsl:param name="in"/>
        <a href="{$in}"><xsl:value-of select="$in"/></a>
    </xsl:template>

    <xsl:template match="ver:media-type">
        <li><xsl:value-of select="@base"/>, <xsl:value-of select="@type"/></li>
    </xsl:template>

    <xsl:template match="text()" mode="multiple">
        <xsl:value-of select="normalize-space(.)"/>
    </xsl:template>
    <xsl:template match="text()">
        <xsl:value-of select="normalize-space(.)"/>
    </xsl:template>
</xsl:transform>
