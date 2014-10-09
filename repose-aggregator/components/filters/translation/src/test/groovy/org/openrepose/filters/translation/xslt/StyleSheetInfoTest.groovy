package org.openrepose.filters.translation.xslt
import org.junit.Test

import static org.hamcrest.CoreMatchers.allOf
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.nullValue
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty
import static org.junit.Assert.*

public class StyleSheetInfoTest {

    @Test
    public void testNodeProvidedConstructor() {
        String id = "id";
        String systemId = "sysID";
        StyleSheetInfo sheet = new StyleSheetInfo("id",null,"sysID");

        assertThat(sheet, allOf(hasProperty("id",equalTo(id)), hasProperty("uri",nullValue()), hasProperty("xsl",nullValue()),
                hasProperty("systemId",equalTo(systemId))))
    }
}

