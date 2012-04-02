<?xml version="1.0" encoding="UTF-8"?>

<transform xmlns="http://www.w3.org/1999/XSL/Transform"
           xmlns:lim="http://docs.openstack.org/common/api/v1.0"
           version="1.0">
    <output method="text" encoding="UTF-8"/>

    <template match="lim:limits">
        <text>{ "limits" : {</text>
        <apply-templates />
        <text>}}</text>
    </template>

    <template match="lim:rates">
        <text> "rate" : { "values" : [</text>
        <apply-templates select="./lim:rate"/>
        <text>]}</text>
    </template>

    <template match="lim:absolute">
        <if test="/lim:limits/lim:rates">
            <text>,</text>
        </if>
        <text>"absolute" : { "values" : {</text>
        <apply-templates select="./lim:limit" mode="absolute"/>
        <text>}}</text>
    </template>

    <template match="lim:rate">
        <variable name="attribs" select="./@*"/>
        <if test="position() != 1">
            <text>,</text>
        </if>
        <text>{</text>
        <apply-templates select="$attribs"/>
        <if test="$attribs">
            <text>,</text>
        </if>
        <text> "limit" : [</text>
        <apply-templates select="./lim:limit" mode="rate"/>
        <text>]</text>
        <text>}</text>
    </template>

    <template match="lim:limit" mode="rate">
        <if test="position() != 1">
            <text>,</text>
        </if>
        <text>{</text>
        <apply-templates select="./@*"/>
        <text>}</text>
    </template>

    <template match="lim:limit" mode="absolute">
        <if test="position() != 1">
            <text>,</text>
        </if>
        <call-template name="json-string">
            <with-param name="in" select="@name"/>
        </call-template>
        <text> : </text>
        <value-of select="@value"/>
    </template>

    <template match="@value | @remaining">
        <if test="position() != 1">
            <text>,</text>
        </if>
        <call-template name="json-string">
            <with-param name="in" select="name()"/>
        </call-template>
        <text> : </text>
        <value-of select="."/>
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
        <param name="in" />
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
