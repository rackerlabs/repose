package com.rackspace.papi.components.translation.xslt

import spock.lang.Specification

/**
 * Created by eric7500 on 6/2/14.
 */
class StyleSheetInfoTest extends Specification {

    def "Test node provided constructor"() {
        given:
        String id = "id"
        String systemId = "sysID"

        when:
        StyleSheetInfo sheet = new StyleSheetInfo("id",null,"sysID")


        then:
        sheet.id.equals("id")
        sheet.xsl.equals(null)
        sheet.uri.equals(null)
        sheet.systemId.equals("sysID")
        sheet.getSystemId().equals("sysID")
    }

}
