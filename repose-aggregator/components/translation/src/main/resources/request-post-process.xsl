<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:json="http://www.ibm.com/xmlns/prod/2009/jsonx">
   
   <xsl:output method="xml" encoding="utf-8" indent="no" omit-xml-declaration="no" media-type="application/xml" />

   <xsl:template match="/|*">
      <xsl:choose>
         <xsl:when test="fn:string(node-name(.)) != 'body'">
            <xsl:copy>
               <xsl:copy-of select="@*"/>
               <xsl:apply-templates/>
            </xsl:copy>
         </xsl:when>
         <xsl:when test="fn:string(node-name(.)) = 'body'">
            <xsl:copy>
            <xsl:choose>
               <!--<xsl:when test="/httpx/request/head/headers/header[@name='Content-Type']/value = 'application/json'">-->
               <xsl:when test="fn:ends-with(/httpx/request/head/headers/header[@name='Content-Type']/value, '/json')">
                  <xsl:apply-templates mode="json"/>
               </xsl:when>
               <!--<xsl:when test="/httpx/request/head/headers/header[@name='Content-Type']/value = 'application/xml'">-->
               <xsl:when test="fn:ends-with(/httpx/request/head/headers/header[@name='Content-Type']/value, '/xml')">
                  <xsl:apply-templates mode="xml"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:apply-templates mode="other"/>
               </xsl:otherwise>
            </xsl:choose>
            </xsl:copy>
         </xsl:when>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template mode="other" match="*">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates mode="other"/>
      </xsl:copy>
   </xsl:template>

   <xsl:template mode="xml" match="*">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates mode="xml"/>
      </xsl:copy>
   </xsl:template>
   

    <xsl:template name="json:doNameAttr">
       <xsl:if test="local-name(..)!='array' and string-length(@name)>0">
          <xsl:value-of select="concat('&quot;', @name, '&quot;', ':')"/>
       </xsl:if>
    </xsl:template>

    <xsl:template mode="json" match="json:object">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:text>{ </xsl:text>
        <xsl:for-each select="*">
           <xsl:apply-templates mode="json" select="."/>
            <xsl:if test="position() != last()">
                <xsl:text>, </xsl:text>
            </xsl:if>
       </xsl:for-each>
       <xsl:text> }</xsl:text>
    </xsl:template>

    <xsl:template mode="json" match="json:array">
        <xsl:call-template name="json:doNameAttr" />
        <xsl:text>[ </xsl:text>
        <xsl:for-each select="*">
            <xsl:apply-templates mode="json" select="." />
            <xsl:if test="position() != last()">
                <xsl:text>, </xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:text> ]</xsl:text>
    </xsl:template>

    <xsl:template mode="json" match="json:string">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:text>"</xsl:text>
        <!-- XXX Need to replace " with &amp;quot; -->
        <xsl:value-of select="normalize-space()"/>
        <xsl:text>"</xsl:text>
    </xsl:template>

    <xsl:template mode="json" match="json:number">
       <xsl:call-template name="json:doNameAttr"/>
       <xsl:value-of select="normalize-space()"/>
    </xsl:template>

    <xsl:template mode="json" match="json:boolean">
       <xsl:call-template name="json:doNameAttr"/>
       <xsl:value-of select="normalize-space()"/>
    </xsl:template>

    <xsl:template mode="json" match="json:null">
        <xsl:call-template name="json:doNameAttr"/>
        <xsl:text>null</xsl:text>
    </xsl:template>

</xsl:stylesheet>        