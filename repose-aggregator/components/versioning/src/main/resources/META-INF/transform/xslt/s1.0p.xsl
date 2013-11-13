<?xml version="1.0" encoding="UTF-8"?>

<transform xmlns="http://www.w3.org/1999/XSL/Transform"
           xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
           xmlns:xsd="http://www.w3.org/2001/XMLSchema"
           version="1.0"
           >
    <template match="node() | @*">
        <copy>
            <apply-templates select="node() | @*"/>
        </copy>
    </template>
    <template match="xsd:*[@vc:minVersion &gt; 1.0]"/>
</transform>
