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
package features.filters.keystonev2

import groovy.text.SimpleTemplateEngine
import org.joda.time.DateTime
import org.rackspace.deproxy.Response

/**
 * Simulates responses from an Identity Atom Feed
 */
class AtomFeedResponseSimulator {
    final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    def templateEngine = new SimpleTemplateEngine()
    volatile boolean hasEntry = false
    int atomPort

    def client_token = 'this-is-the-token'
    def client_tenant = 'this-is-the-tenant'

    def headers = [
            'Connection'  : 'close',
            'Content-type': 'application/xml',
    ]

    AtomFeedResponseSimulator(int atomPort) {
        this.atomPort = atomPort
    }

    /**
     * This is a method that takes params, and returns a closure
     * Creates an appropriate closure for the TRR user token revocation event
     * @param userId
     * @return
     */
    def trrEventHandler(String userId) {
        { request ->
            if (hasEntry) {
                def params = [
                        'atomPort': atomPort,
                        'time'    : new DateTime().toString(DATE_FORMAT),
                        'userId'  : userId
                ]
                hasEntry = false //Only respond with it once once it's been got

                new Response(200, 'OK', headers, templateEngine.createTemplate(tokenRevocationRecordAtomEntryXml).make(params)
                )
            }
        }
    }

    def userUpdateHandler(String userId) {
        { request ->
            if (hasEntry) {
                def params = [
                        'atomPort': atomPort,
                        'time'    : new DateTime().toString(DATE_FORMAT),
                        'userId'  : userId
                ]
                hasEntry = false
                new Response(200, 'OK', headers, templateEngine.createTemplate(userUpdateEvent).make(params))
            }
        }
    }


    def handler = { request ->

        def template

        if (hasEntry) {
            template = atomWithEntryXml
        } else {
            template = atomEmptyXml
        }

        def params = [
                'atomPort': atomPort,
                'time'    : new DateTime().toString(DATE_FORMAT),
                'token'   : client_token,
                'tenant'  : client_tenant,
        ]
        hasEntry = false
        return new Response(200, 'OK', headers, templateEngine.createTemplate(template).make(params))
    }


    def String atomEmptyXml =
            """<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
    <link href="http://localhost:\${atomPort}/feed/"
        rel="current"/>
    <link href="http://localhost:\${atomPort}/feed/"
        rel="self"/>
    <id>urn:uuid:12345678-9abc-def0-1234-56789abcdef0</id>
    <title type="text">feed</title>
    <link href="http://localhost:\${atomPort}/feed/?marker=last&amp;limit=25&amp;search=&amp;direction=backward"
          rel="last"/>
    <updated>\${time}</updated>
</feed>
"""

    def String atomWithEntryXml =
            """<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
    <link href="http://localhost:\${atomPort}/feed/"
        rel="current"/>
    <link href="http://localhost:\${atomPort}/feed/"
        rel="self"/>
    <id>urn:uuid:12345678-9abc-def0-1234-56789abcdef0</id>
    <title type="text">feed</title>
    <link href="http://localhost:\${atomPort}/feed/?marker=urn:uuid:1&amp;limit=25&amp;search=&amp;direction=forward"
          rel="previous"/>
    <updated>\${time}</updated>
    <atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns="http://docs.rackspace.com/core/event"
                xmlns:id="http://docs.rackspace.com/event/identity/token">
        <atom:id>urn:uuid:1</atom:id>
        <atom:category term="rgn:IDK"/>
        <atom:category term="dc:IDK1"/>
        <atom:category term="rid:\${token}"/>
        <atom:category term="cloudidentity.token.token.delete"/>
        <atom:title type="text">Identity Token Event</atom:title>
        <atom:author>
            <atom:name>Repose Team</atom:name>
        </atom:author>
        <atom:content type="application/xml">
            <event dataCenter="IDK"
                   environment="JUNIT"
                   eventTime="\${time}"
                   id="12345678-9abc-def0-1234-56789abcdef0"
                   region="IDK"
                   resourceId="\${token}"
                   type="DELETE"
                   version="1">
                <id:product resourceType="TOKEN"
                            serviceCode="CloudIdentity"
                            tenants="\${tenant}"
                            version="1"/>
            </event>
        </atom:content>
        <atom:link href="http://test.feed.atomhopper.rackspace.com/some/identity/feed/entries/urn:uuid:4fa194dc-5148-a465-254d-b8ccab3766bc"
                   rel="self"/>
        <atom:updated>\${time}</atom:updated>
        <atom:published>\${time}</atom:published>
    </atom:entry>
</feed>
"""

    /**
     * This is to revoke a specific set of tokens for a specific user
     * The resourceId is a user id, not a token ID.
     */
    def tokenRevocationRecordAtomEntryXml =
            """<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
    <link href="http://localhost:\${atomPort}/feed/"
        rel="current"/>
    <link href="http://localhost:\${atomPort}/feed/"
        rel="self"/>
    <id>urn:uuid:12345678-9abc-def0-1234-56789abcdef0</id>
    <title type="text">feed</title>
    <link href="http://localhost:\${atomPort}/feed/?marker=urn:uuid:1&amp;limit=25&amp;search=&amp;direction=forward"
          rel="previous"/>
    <updated>\${time}</updated>
    <atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns="http://www.w3.org/2001/XMLSchema">
        <atom:id>urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814</atom:id>
        <atom:category term="rgn:DFW"/>
        <atom:category term="dc:DFW1"/>
        <atom:category term="rid:\${userId}"/>
        <atom:category term="cloudidentity.user.trr_user.delete"/>
        <atom:category term="type:cloudidentity.user.trr_user.delete"/>
        <atom:title>CloudIdentity</atom:title>
        <atom:content type="application/xml">
            <event xmlns="http://docs.rackspace.com/core/event"
                xmlns:sample="http://docs.rackspace.com/event/identity/trr/user"
                id="e53d007a-fc23-11e1-975c-cfa6b29bb814"
                version="2"
                resourceId="\${userId}"
                eventTime="\${time}"
                type="DELETE"
                dataCenter="DFW1"
                region="DFW">
                <sample:product serviceCode="CloudIdentity"
                    version="1"
                    resourceType="TRR_USER"
                    tokenCreationDate="2013-09-26T15:32:00Z">
                    <sample:tokenAuthenticatedBy values="PASSWORD APIKEY"/>
                </sample:product>
            </event>
        </atom:content>
        <atom:link href="https://ord.feeds.api.rackspacecloud.com/identity/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814"
        rel="self"/>
        <atom:updated>\${time}</atom:updated>
        <atom:published>\${time}</atom:published>
    </atom:entry>
</feed>
"""

    def userUpdateEvent =
            """<?xml version="1.0" encoding="UTF-8"?>
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>urn:uuid:e29ac1ca-fd06-11e1-a80c-bb58fc4a6929</atom:id>
    <atom:category term="rgn:DFW"/>
    <atom:category term="dc:DFW1"/>
    <atom:category term="rid:10031728"/>
    <atom:category term="tid:123456"/>
    <atom:category term="cloudidentity.user.user.update"/>
    <atom:category term="type:cloudidentity.user.user.update"/>
    <atom:category term="updatedAttributes:GROUPS"/>
    <atom:title type="text">Identity Event</atom:title>
    <atom:content type="application/xml">
        <event xmlns="http://docs.rackspace.com/core/event"
           xmlns:id="http://docs.rackspace.com/event/identity/user"
           dataCenter="DFW1"
           environment="PROD"
           eventTime="\${time}"
           tenantId="123456"
           id="e29ac1ca-fd06-11e1-a80c-bb58fc4a6929"
           region="DFW"
           resourceId="\${userId}"
           resourceName="testuser"
           type="UPDATE"
           version="1">
            <id:product displayName="testUser"
                  groups="group1 group2 group3"
                  migrated="false"
                  multiFactorEnabled="false"
                  resourceType="USER"
                  roles="admin RAX:admin role3"
                  serviceCode="CloudIdentity"
                  updatedAttributes="GROUPS"
                  version="2"/>
        </event>
    </atom:content>
    <atom:link href="http://localhost:\${atomPort}/feed/"
             rel="self"/>
    <atom:updated>\${time}</atom:updated>
    <atom:published>\${time}</atom:published>
</atom:entry>
"""
}
