<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">
    <xsl:output method="xml"/>

    <xsl:template match="/">
        <add-me>
            <xsl:copy-of select="."/>
        </add-me>
    </xsl:template>
</xsl:stylesheet>
