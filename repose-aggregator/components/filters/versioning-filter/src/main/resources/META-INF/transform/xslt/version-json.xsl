<?xml version="1.0" encoding="UTF-8"?>
<!--
  _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
  Repose
  _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
  Copyright (C) 2010 - 2015 Rackspace US, Inc.
  _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
  -->

<transform xmlns:ver="http://docs.openstack.org/common/api/v1.0"
           xmlns:atom="http://www.w3.org/2005/Atom"
           xmlns="http://www.w3.org/1999/XSL/Transform"
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
        <variable name="links" select="descendant::atom:link"/>
        <variable name="types" select="descendant::ver:media-type"/>

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
        <param name="name"/>
        <param name="nodes"/>
        <call-template name="json-string">
            <with-param name="in" select="$name"/>
        </call-template>
        <text> :[</text>
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
        <call-template name="json-string">
            <with-param name="in" select="name()"/>
        </call-template>
        <text> : </text>
        <call-template name="json-string">
            <with-param name="in" select="."/>
        </call-template>
    </template>

    <template name="json-string">
        <param name="in"/>
        <variable name="no-backslash">
            <call-template name="escape-out">
                <with-param name="in" select="$in"/>
                <with-param name="char" select="'\'"/>
            </call-template>
        </variable>
        <variable name="no-quote">
            <call-template name="escape-out">
                <with-param name="in" select="$no-backslash"/>
                <with-param name="char" select="'&quot;'"/>
            </call-template>
        </variable>
        <value-of select="concat('&quot;',$no-quote,'&quot;')"/>
    </template>

    <template name="escape-out">
        <param name="in"/>
        <param name="char"/>
        <variable name="before" select="substring-before($in, $char)"/>
        <variable name="after" select="substring-after($in, $char)"/>
        <choose>
            <when test="string-length($before) &gt; 0 or string-length($after) &gt; 0">
                <value-of select="concat($before,'\',$char)"/>
                <call-template name="escape-out">
                    <with-param name="in" select="$after"/>
                    <with-param name="char" select="$char"/>
                </call-template>
            </when>
            <otherwise>
                <value-of select="$in"/>
            </otherwise>
        </choose>
    </template>
</transform>
