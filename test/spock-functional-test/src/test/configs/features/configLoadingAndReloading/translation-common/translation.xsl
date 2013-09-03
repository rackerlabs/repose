<xsl:stylesheet
    version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:old="http://docs.rackspacecloud.com/servers/api/v1.0"
    xmlns="http://docs.rackspacecloud.com/servers/api/v1.1">

    <xsl:output method="xml" encoding="UTF-8" />

    <xsl:template match="old:server">
        <xsl:element name="server">
            <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
            <xsl:element name="imageId"><xsl:value-of select="@imageId"/></xsl:element>
            <xsl:element name="flavorId"><xsl:value-of select="@flavorId"/></xsl:element>

            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="old:metadata">
        <xsl:element name="metadata">
            <xsl:apply-templates select="old:meta"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="old:meta">
        <xsl:element name="meta">
            <xsl:attribute name="key"><xsl:value-of select="@key"/></xsl:attribute>
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="old:personality">
        <xsl:element name="personality">
            <xsl:apply-templates select="old:file"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="old:file">
        <xsl:element name="file">
            <xsl:attribute name="path"><xsl:value-of select="@path"/></xsl:attribute>
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>