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
        <text> "rate" : [</text>
        <apply-templates select="./lim:rate"/>
        <text>]</text>
    </template>

    <template match="lim:absolute">
        <if test="/lim:limits/lim:rates">
            <text>,</text>
        </if>
        <text>"absolute" : {</text>
        <apply-templates select="./lim:limit" mode="absolute"/>
        <text>}</text>
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
        <text>"</text>
        <value-of select="@name"/>
        <text>" : </text>
        <value-of select="@value"/>
    </template>

    <template match="@value | @remaining">
        <if test="position() != 1">
            <text>,</text>
        </if>
        <text>"</text>
        <value-of select="name()"/>
        <text>" : </text>
        <value-of select="."/>
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
