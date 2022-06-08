<?xml version="1.0" encoding="UTF-8"?>

<transform xmlns:tst="test://test.me"
           xmlns="http://www.w3.org/1999/XSL/Transform"
           version="1.0">
    <output method="text"/>

    <template match="tst:fail">
        <message terminate="yes">Throwing Error!</message>
    </template>

    <template match="tst:warn">
        <message>This is simply a warning</message>
    </template>
</transform>
