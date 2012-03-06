<?xml version="1.0" encoding="UTF-8"?>

<transform xmlns="http://www.w3.org/1999/XSL/Transform"
           xmlns:tst="test://test.me"
           version="1.0">
    <output method="text"/>

    <template match="tst:fail">
        <message terminate="yes">Throwing Error!</message>
    </template>

    <template match="tst:warn">
        <message>This is simply a warning</message>
    </template>
</transform>
