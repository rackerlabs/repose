/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.rackspaceauthuser

import groovy.transform.Canonical

class RackspaceAuthPayloads {
    public static Map contentXml = ["content-type": "application/xml"]
    public static Map contentJson = ["content-type": "application/json"]
    public static String invalidData = "Invalid data"

    public static String userKeyXmlV11 =
            """<credentials xmlns="http://docs.rackspacecloud.com/auth/api/v1.1" username="test-user" key="testpwd" />"""

    public static String userKeyJsonV11 = """{
    "credentials":{
        "username": "test-user",
        "key": "testpwd"
    }
}"""

    public static String userKeyXmlEmptyV11 =
            """<credentials xmlns="http://docs.rackspacecloud.com/auth/api/v1.1" username="test-user" key="" />"""

    public static String userKeyJsonEmptyV11 = """{
    "credentials":{
        "username": "test-user",
        "key": ""
    }
}"""

    public static String userPasswordXmlV20 = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns="http://docs.openstack.org/identity/api/v2.0">
    <passwordCredentials username="demoAuthor" password="myPassword01" tenantId="1100111"/>
</auth>"""

    public static String userPasswordJsonV20 = """{
    "auth":{
        "passwordCredentials":{
            "username":"demoAuthor",
            "password":"myPassword01"
        },
        "tenantId": "1100111"
    }
}"""

    public static String userForgotXmlV20 = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<forgotPasswordCredentials xmlns="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" username="demoAuthor"/>"""

    public static String userForgotJsonV20 = """{
    "RAX-AUTH:forgotPasswordCredentials": {
        "username": "demoAuthor"
    }
}"""

    public static String userApiKeyXmlV20 = """<?xml version="1.0" encoding="UTF-8"?>
<auth>
    <apiKeyCredentials xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
                       username="demoAuthor"
                       apiKey="aaaaa-bbbbb-ccccc-12345678"/>
</auth>"""

    public static String userApiKeyJsonV20 = """{
    "auth":{
        "RAX-KSKEY:apiKeyCredentials": {
            "username": "demoAuthor",
            "apiKey": "aaaaa-bbbbb-ccccc-12345678"
        },
        "tenantId": "1100111"
    }
}"""

    // The example this was taken from is currently incorrect.
    // This is the best guess at what it should be until it is corrected.
    // The error will only effect the outcome if/when we start differentiating Scope'd items.
    // TODO: Have the API documentation updated and update this payload.
    public static String userMfaSetupXmlV20 = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
      xmlns="http://docs.openstack.org/identity/api/v2.0">
    <RAX-AUTH:scope>SETUP-MFA</RAX-AUTH:scope>
    <passwordCredentials username="demoAuthor" password="myPassword01"/>
</auth>"""

    public static String userMfaSetupJsonV20 = """{
    "auth":{
        "RAX-AUTH:scope": "SETUP-MFA",
        "passwordCredentials": {
            "username": "demoAuthor",
            "password": "myPassword01"
        }
    }
}"""

    public static String userPasswordXmlEmptyV20 = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns="http://docs.openstack.org/identity/api/v2.0">
    <passwordCredentials username="demoAuthor" password="" tenantId="1100111"/>
</auth>"""

    public static String userPasswordJsonEmptyV20 = """{
    "auth":{
        "passwordCredentials":{
            "username":"demoAuthor",
            "password":""
        },
        "tenantId": "1100111"
    }
}"""

    public static String userPasswordXmlOverV20 = """<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://docs.openstack.org/identity/api/v2.0">
    <credential xsi:type="PasswordCredentialsRequiredUsername"
                username="012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
                password="testpwd" />
</auth>"""

    public static String userPasswordJsonOverV20 = """{
    "auth":{
        "passwordCredentials":{
            "username":"012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
            "password":""
        },
        "tenantId": "1100111"
    }
}"""

    public static String rackerPasswordXmlV20 = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns="http://docs.openstack.org/identity/api/v2.0"
      xmlns:OS-KSADM="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
      xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
      xmlns:atom="http://www.w3.org/2005/Atom">
    <passwordCredentials password="mypassword" username="jqsmith"/>
    <RAX-AUTH:domain name="Rackspace"/>
</auth>"""

    public static String rackerPasswordJsonV20 = """{
    "auth": {
        "RAX-AUTH:domain": {
            "name": "Rackspace"
        },
        "passwordCredentials": {
            "username": "jqsmith",
            "password": "mypassword"
        }
    }
}"""

    public static String rackerTokenKeyXmlV20 = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns="http://docs.openstack.org/identity/api/v2.0"
      xmlns:OS-KSADM="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
      xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
      xmlns:atom="http://www.w3.org/2005/Atom">
    <RAX-AUTH:rsaCredentials tokenKey="8723984574" username="jqsmith"/>
    <RAX-AUTH:domain name="Rackspace"/>
</auth>"""

    public static String rackerTokenKeyJsonV20 = """{
    "auth": {
        "RAX-AUTH:domain": {
            "name": "Rackspace"
        },
        "RAX-AUTH:rsaCredentials": {
            "tokenKey": "8723984574",
            "username": "jqsmith"
        }
    }
}"""

    public static String federatedPasswordXmlV20 = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns="http://docs.openstack.org/identity/api/v2.0"
      xmlns:OS-KSADM="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
      xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
      xmlns:atom="http://www.w3.org/2005/Atom">
    <passwordCredentials password="mypassword" username="jqsmith"/>
    <RAX-AUTH:domain name="Federated"/>
</auth>"""

    public static String federatedPasswordJsonV20 = """{
    "auth": {
        "RAX-AUTH:domain": {
            "name": "Federated"
        },
        "passwordCredentials": {
            "username": "jqsmith",
            "password": "mypassword"
        }
    }
}"""

    def static payloadTests = [
            new PayloadTest("userPasswordXmlV20", userPasswordXmlV20, contentXml, "demoAuthor"),
            new PayloadTest("userPasswordJsonV20", userPasswordJsonV20, contentJson, "demoAuthor"),
            new PayloadTest("userApiKeyXmlV20", userApiKeyXmlV20, contentXml, "demoAuthor"),
            new PayloadTest("userApiKeyJsonV20", userApiKeyJsonV20, contentJson, "demoAuthor"),
            new PayloadTest("userPasswordXmlEmptyV20", userPasswordXmlEmptyV20, contentXml, "demoAuthor"),
            new PayloadTest("userPasswordJsonEmptyV20", userPasswordJsonEmptyV20, contentJson, "demoAuthor"),
            new PayloadTest("userMfaSetupXmlV20", userMfaSetupXmlV20, contentXml, "demoAuthor"),
            new PayloadTest("userMfaSetupJsonV20", userMfaSetupJsonV20, contentJson, "demoAuthor"),
            new PayloadTest("rackerPasswordXmlV20", rackerPasswordXmlV20, contentXml, "Racker:jqsmith"),
            new PayloadTest("rackerPasswordJsonV20", rackerPasswordJsonV20, contentJson, "Racker:jqsmith"),
            new PayloadTest("rackerTokenKeyXmlV20", rackerTokenKeyXmlV20, contentXml, "Racker:jqsmith"),
            new PayloadTest("rackerTokenKeyJsonV20", rackerTokenKeyJsonV20, contentJson, "Racker:jqsmith"),
            new PayloadTest("federatedTokenKeyXmlV20", federatedPasswordXmlV20, contentXml, "jqsmith"),
            new PayloadTest("federatedTokenKeyJsonV20", federatedPasswordJsonV20, contentJson, "jqsmith"),
            new PayloadTest("userKeyXmlV11", userKeyXmlV11, contentXml, "test-user"),
            new PayloadTest("userKeyJsonV11", userKeyJsonV11, contentJson, "test-user"),
            new PayloadTest("userKeyXmlEmptyV11", userKeyXmlEmptyV11, contentXml, "test-user"),
            new PayloadTest("userKeyJsonEmptyV11", userKeyJsonEmptyV11, contentJson, "test-user")]

    @Canonical
    static class PayloadTest {
        String testName
        String requestBody
        Map contentType
        String expectedUser
    }
}
