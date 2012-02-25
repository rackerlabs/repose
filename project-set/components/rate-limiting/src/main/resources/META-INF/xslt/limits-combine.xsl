<?xml version="1.0" encoding="UTF-8"?>

<!-- Limits Combine -->
<!--
   Combines rate limits, submited as input, with 
   absolute limits submited via the absoluteURL parameter.
-->
<transform xmlns="http://www.w3.org/1999/XSL/Transform"
           xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
           xmlns:lim="http://docs.openstack.org/common/api/v1.0"
           version="1.0">

    <!-- Reference to the absolute limits -->
    <param name="absoluteURL"  />

    <output method="xml" version="1.0"
            encoding="UTF-8"
            media-type="application/xml"
            omit-xml-declaration="no"/>

    <variable name="absoluteDoc" select="document($absoluteURL)" />

    <template match="lim:limits">
        <if test="not($absoluteURL)">
            <message terminate="yes">absoluteURL parameter must be set</message>
        </if>
        
        <if test="count($absoluteDoc)!=1">
            <message>Could not load <value-of select="$absoluteURL"/></message>
        </if>
        
        <limits xmlns="http://docs.openstack.org/common/api/v1.0">
            <xsl:apply-templates select="lim:rates"/>
            <xsl:apply-templates select="$absoluteDoc/lim:limits/lim:absolute"/>
        </limits>
    </template>

    <template match="node() | @*">
        <copy>
            <apply-templates select="@* | node()"/>
        </copy>
    </template>
</transform>
