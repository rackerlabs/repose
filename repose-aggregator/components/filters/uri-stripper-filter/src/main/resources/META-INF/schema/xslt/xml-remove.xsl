<xsl:transform version="2.0"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:xslout="http://www.rackspace.com/xslout"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:param="http://www.rackspace.com/repose/params"
               exclude-result-prefixes="param">

    <xsl:param name="xpath" as="xs:string"/>

    <xsl:template match="/">
        <xslout:transform version="2.0">
           <xslout:template match="@*|node()">
               <xslout:copy>
                   <xslout:apply-templates select="@*|node()"/>
               </xslout:copy>
           </xslout:template>

            <xslout:template match="{$xpath}"/>
        </xslout:transform>
    </xsl:template>
</xsl:transform>
