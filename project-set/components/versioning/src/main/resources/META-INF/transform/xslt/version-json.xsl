<?xml version="1.0" encoding="UTF-8"?>

<transform xmlns="http://www.w3.org/1999/XSL/Transform"
           xmlns:ver="http://docs.openstack.org/common/api/v1.0"
           xmlns:atom="http://www.w3.org/2005/Atom"
           version="1.0">
    <output method="text" encoding="UTF-8"/>

    <template match="ver:version">
        <text>{ "version" :</text>
        <call-template name="doVersion"/>
        <text>}</text>
    </template>

    <template match="ver:versions | ver:choices">
        <text>{ "</text>
        <value-of select="local-name()"/>
        <text>" : [ </text>
        <for-each select="descendant::ver:version">
            <if test="position() != 1">
                <text>,</text>
            </if>
            <call-template name="doVersion"/>
        </for-each>
        <text>]}</text>
    </template>

    <template name="doVersion">
        <variable name="attribs" select="@*"/>
        <variable name="links"   select="descendant::atom:link"/>
        <variable name="types"   select="descendant::ver:media-type"/>

        <text>{</text>
        <apply-templates select="$attribs"/>
        <if test="$links">
            <if test="$attribs">
                <text>,</text>
            </if>
            <call-template name="doArray">
                <with-param name="name">links</with-param>
                <with-param name="nodes" select="$links"/>
            </call-template>
        </if>
        <if test="$types">
            <if test="$links | $attribs">
                <text>,</text>
            </if>
            <call-template name="doArray">
                <with-param name="name">media-types</with-param>
                <with-param name="nodes" select="$types"/>
            </call-template>
        </if>
        <text>}</text>
    </template>

    <template name="doArray">
        <param name="name" />
        <param name="nodes" />
        <text>"</text>
        <value-of select="$name"/>
        <text>":[</text>
        <for-each select="$nodes">
            <if test="position() != 1">
                <text>,</text>
            </if>
            <text>{</text>
            <apply-templates select="./@*"/>
            <text>}</text>
        </for-each>
        <text>]</text>
    </template>

    <template match="@*">
        <if test="position() != 1">
            <text>,</text>
        </if>
        <text>"</text>
        <value-of select="name()"/>
        <text>" : "</text>
        <value-of select="."/>
        <text>"</text>
    </template>
</transform>
