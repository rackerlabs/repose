<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:event="http://docs.rackspace.com/core/event"
                xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns:httpx="http://docs.openrepose.org/repose/httpx/v1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://wadl.dev.java.net/2009/02"
                version="2.0">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    <xsl:param name="input-headers-uri"/>
    <xsl:variable name="headerDoc" select="doc($input-headers-uri)"/>
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!--For product: CloudServers -->
    <xsl:template xmlns:pf="http://docs.rackspace.com/event/servers/slice"
                  match="pf:product[@version='1']/@*[some $x in ('rootPassword','huddleId') satisfies $x eq local-name(.)]">
    </xsl:template>

</xsl:stylesheet>