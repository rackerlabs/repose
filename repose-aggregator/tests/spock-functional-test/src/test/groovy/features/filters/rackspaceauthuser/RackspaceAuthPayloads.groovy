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


class RackspaceAuthPayloads {
    public static Map contentXml = ["content-type": "application/xml"]
    public static Map contentJSON = ["content-type": "application/json"]

    public static String xmlPasswordCred = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xmlns="http://docs.openstack.org/identity/api/v2.0">
  <passwordCredentials username="demoauthor" password="theUsersPassword" tenantId="1100111"/>
</auth>"""


    public static String xmlPasswordCredEmptyKey = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xmlns="http://docs.openstack.org/identity/api/v2.0">
  <passwordCredentials username="demoauthor" password="" tenantId="1100111"/>
</auth>"""

    public static String invalidData = "Invalid data"
    public
    static String xmlOverLimit = "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><credential xsi:type=\"PasswordCredentialsRequiredUsername\" username=\"012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\" password=\"testpwd\" /></auth>"

    public static String jsonPasswordCred = """
{
    "auth":{
        "passwordCredentials":{
            "username":"demoauthor",
            "password":"theUsersPassword"
        },
        "tenantId": "12345678"
    }
}
"""

    public static String jsonPasswordCredAuthr2 = """
{
    "auth":{
        "passwordCredentials":{
            "username":"demoauthr2",
            "password":"theUsersPassword"
        },
        "tenantId": "12345678"
    }
}
"""

    public def static String jsonApiKeyCred = """{
    "auth": {
        "RAX-KSKEY:apiKeyCredentials": {
            "username": "demoauthor",
            "apiKey": "aaaaa-bbbbb-ccccc-12345678"
        },
        "tenantId": "1100111"
    }
}
"""
    //v1.1
    public static String jsonKeyCred11 = "{ \"credentials\": { \"username\": \"test-user\", \"key\": \"testpwd\"}}"
    public static String jsonKeyCredEmptyKey11 = "{ \"credentials\": { \"username\": \"test-user\", \"key\": \"\"}}"
    public
    static String xmlKeyCred11 = "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"test-user\" key=\"testpwd\" />"
    public
    static String xmlKeyCredEmptyKey11 = "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"test-user\" key=\"\" />"

}
