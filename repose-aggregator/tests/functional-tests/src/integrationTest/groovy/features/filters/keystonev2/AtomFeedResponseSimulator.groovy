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

import groovy.xml.MarkupBuilder
import org.joda.time.DateTime
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * Simulates responses from an Identity Atom Feed
 */
class AtomFeedResponseSimulator {
    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    static final Map DEFAULT_FEED_PARAMS = [id: "urn:uuid:12345678-9abc-def0-1234-56789abcdef0", isLastPage: false, pageSize: 25]

    volatile boolean hasEntry = false
    int atomPort

    def client_token = 'this-is-the-token'
    def client_tenant = 'this-is-the-tenant'

    def headers = ['Content-type': 'application/xml']

    AtomFeedResponseSimulator(int atomPort) {
        this.atomPort = atomPort
    }

    def getDefaultParams() {
        [atomPort: atomPort,
         time    : new DateTime().toString(DATE_FORMAT),
         token   : client_token,
         tenant  : client_tenant]
    }

    /**
     * This is a method that takes params, and returns a closure
     * Creates an appropriate closure for the TRR user token revocation event
     */
    def trrEventHandler(String userId) {
        { request ->
            if (hasEntry) {
                // reset for next time
                hasEntry = false

                new Response(SC_OK, null, headers, atomFeed(atomEntryForTokenRevocationRecord(userId: userId)))
            }
        }
    }

    def userUpdateHandler(String userId) {
        { request ->
            if (hasEntry) {
                // reset for next time
                hasEntry = false

                new Response(SC_OK, null, headers, atomFeed(atomEntryForUserUpdate(userId: userId)))
            }
        }
    }

    def handler = { request ->
        def body = hasEntry ? atomFeed(atomEntryForTokenInvalidation()) : atomFeedWithNoEntries()

        // reset for next time
        hasEntry = false

        new Response(SC_OK, null, headers, body)
    }

    def handlerWithEntry(Closure<MarkupBuilder> entry) {
        { Request request -> new Response(SC_OK, null, headers, atomFeed(entry)) }
    }

    def handlerWithEntries(List<Closure<MarkupBuilder>> entries) {
        // use closure composition and inject (fold) to turn the list of closures into one closure
        handlerWithEntry(entries.inject({ MarkupBuilder builder -> builder }) { composedEntries, entry ->
            composedEntries >> entry
        })
    }

    String atomFeed(Map values = [:], Closure<MarkupBuilder> entries) {
        Map params = getDefaultParams() + DEFAULT_FEED_PARAMS + values

        return buildXmlToString { MarkupBuilder xmlBuilder ->
            xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")

            xmlBuilder.'feed'(xmlns: "http://www.w3.org/2005/Atom") {
                'link'(href: "http://localhost:${params.atomPort}/feed/", rel: "current")
                'link'(href: "http://localhost:${params.atomPort}/feed/", rel: "self")
                'id'(params.id)
                'title'(type: "text", "feed")
                if (params.isLastPage) {
                    'link'(href: "http://localhost:${params.atomPort}/feed/?marker=last&amp;limit=25&amp;search=&amp;direction=backward", rel: "last")
                } else {
                    'link'(href: "http://localhost:${params.atomPort}/feed/?marker=urn:uuid:1&amp;limit=25&amp;search=&amp;direction=forward", rel: "previous")
                }
                'updated'(params.time)
                entries(xmlBuilder)
            }

            xmlBuilder
        }
    }

    /**
     * The page size will be determined for each page based on the number of entries.
     * Currently only supports pages of the same size.
     * It is recommended to collate entry parameters to be passed to this method.
     *
     * @param values    parameters to be used when constructing the feed
     * @param pages     the content of the feed
     * @return          a collection of string representations of pages of an Atom feed
     */
    List<String> pagedTokenInvalidationAtomFeed(Map values = [:], List<List<Map>> pages) {
        Map params = getDefaultParams() + DEFAULT_FEED_PARAMS + values

        return pages.withIndex().collect { indexedPage ->
            def page = indexedPage.first
            def index = indexedPage.second;

            { MarkupBuilder xmlBuilder ->
                xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")

                xmlBuilder.'feed'(xmlns: "http://www.w3.org/2005/Atom") {
                    'link'(href: "http://localhost:${params.atomPort}/feed/", rel: "current")
                    'link'(href: "http://localhost:${params.atomPort}/feed/?limit=${params.pageSize}&amp;direction=backward", rel: "self")
                    'id'(params.id)
                    'title'(type: "text", "feed")
                    if (index - 1 >= 0) {
                        'link'(href: "http://localhost:${params.atomPort}/feed/?marker=${pages[index - 1].last().id}&amp;limit=${params.pageSize}&amp;search=&amp;direction=forward", rel: "previous")
                    }
                    if (index + 1 < pages.size()) {
                        'link'(href: "http://localhost:${params.atomPort}/feed/?marker=${pages[index + 1].first().id}&amp;limit=${params.pageSize}&amp;search=&amp;direction=forward", rel: "next")
                    }
                    'link'(href: "http://localhost:${params.atomPort}/feed/?marker=last&amp;limit=${params.pageSize}&amp;search=&amp;direction=backward", rel: "last")
                    'updated'(params.time)
                    page.collect { atomEntryForTokenInvalidation(it) }.each { it(xmlBuilder) }
                }

                xmlBuilder
            }
        }.collect { buildXmlToString(it) }
    }

    String atomFeedWithNoEntries() {
        atomFeed(isLastPage: true) { MarkupBuilder builder ->
            builder
        }
    }

    static Closure<MarkupBuilder> atomEntry(Map params = [:], Closure<MarkupBuilder> event) {
        return { MarkupBuilder builder ->
            builder.'atom:entry'(
                    'xmlns:atom': "http://www.w3.org/2005/Atom",
                    'xmlns': "http://docs.rackspace.com/core/event",
                    'xmlns:id': "http://docs.rackspace.com/event/identity/token") {
                'atom:id'(params.id)
                params.categories.each {
                    'atom:category'(term: it)
                }
                'atom:title'(type: "text", params.title)
                'atom:author' {
                    'atom:name'("Repose Team")
                }
                'atom:content'(type: "application/xml") {
                    event(builder)
                }
                'atom:link'(href: params.selfLink, rel: "self")
                'atom:updated'(params.time)
                'atom:published'(params.time)
            }

            builder
        }
    }

    Closure<MarkupBuilder> atomEntryForTokenInvalidation(Map values = [:]) {
        Map params = getDefaultParams()
        params += [
                title: "Identity Token Event",
                id: "urn:uuid:1",
                categories: ["rgn:IDK", "dc:IDK1", "rid:${values.token ?: params.token}",
                             "cloudidentity.token.token.delete"],
                selfLink: "http://test.feed.atomhopper.rackspace.com/some/identity/feed/entries/urn:uuid:4fa194dc-5148-a465-254d-b8ccab3766bc"]
        params += values

        return atomEntry(params) { MarkupBuilder builder ->
            builder.'event'(
                    dataCenter: "IDK",
                    environment: "JUNIT",
                    eventTime: params.time,
                    id: "12345678-9abc-def0-1234-56789abcdef0",
                    region: "IDK",
                    resourceId: params.token,
                    type: "DELETE",
                    version: "1") {
                'id:product'(resourceType: "TOKEN", serviceCode: "CloudIdentity", tenants: params.tenant, version: "1")
            }

            builder
        }
    }

    Closure<MarkupBuilder> atomEntryForTokenRevocationRecord(Map values = [:]) {
        Map params = getDefaultParams()
        params += [
                title: "CloudIdentity",
                id: "urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814",
                categories: ["rgn:DFW", "dc:DFW1", "rid:${values.userId ?: params.userId}",
                             "cloudidentity.user.trr_user.delete", "type:cloudidentity.user.trr_user.delete"],
                selfLink: "https://ord.feeds.api.rackspacecloud.com/identity/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814"]
        params += values

        return atomEntry(params) { MarkupBuilder builder ->
            builder.'event'(
                    'xmlns:sample': "http://docs.rackspace.com/event/identity/trr/user",
                    dataCenter: "DFW1",
                    eventTime: params.time,
                    id: "e53d007a-fc23-11e1-975c-cfa6b29bb814",
                    region: "DFW",
                    resourceId: params.userId,
                    type: "DELETE",
                    version: "2") {
                'sample:product'(resourceType: "TRR_USER", serviceCode: "CloudIdentity", version: "1", tokenCreationDate: "2013-09-26T15:32:00Z") {
                    'sample:tokenAuthenticatedBy'(values: "PASSWORD APIKEY")
                }
            }

            builder
        }
    }

    Closure<MarkupBuilder> atomEntryForUserUpdate(Map values = [:]) {
        Map params = getDefaultParams()
        params += [
                title: "Identity Event",
                id: "urn:uuid:e29ac1ca-fd06-11e1-a80c-bb58fc4a6929",
                categories: ["rgn:DFW", "dc:DFW1", "rid:${values.userId ?: params.userId}", "tid:123456",
                             "cloudidentity.user.user.update", "type:cloudidentity.user.user.update",
                             "updatedAttributes:GROUPS ROLES PASSWORD"],
                selfLink: "http://localhost:${params.atomPort}/feed/"]
        params += values

        return atomEntry(params) { MarkupBuilder builder ->
            builder.'event'(
                    'xmlns:id': "http://docs.rackspace.com/event/identity/user",
                    dataCenter: "DFW1",
                    environment: "PROD",
                    eventTime: params.time,
                    id: "e29ac1ca-fd06-11e1-a80c-bb58fc4a6929",
                    region: "DFW",
                    resourceId: params.userId,
                    tenantId: "123456",
                    resourceName: "testuser",
                    type: "UPDATE",
                    version: "1") {
                'id:product'(resourceType: "USER", serviceCode: "CloudIdentity", version: "2", displayName: "testUser",
                        groups: "group1 group2 group3", migrated: "false", multiFactorEnabled: "false",
                        roles: "admin RAX:admin role3", updatedAttributes: "GROUPS ROLES PASSWORD")
            }

            builder
        }
    }

    Closure<MarkupBuilder> atomEntryForIdpUpdate(Map values = [:]) {
        Map params = getDefaultParams()
        params += [
                title: "Identity Provider Event",
                id: "urn:uuid:${UUID.randomUUID().toString()}",
                eventId: UUID.randomUUID().toString(),
                idpId: "_${UUID.randomUUID().toString()}",
                issuer: "http://idp.external.com",
                eventType: "UPDATE",
                resourceType: "IDP",
                serviceCode: "CloudIdentity",
                categories: [],
                selfLink: "http://localhost:${params.atomPort}/feed/"]
        params += values

        return atomEntry(params) { MarkupBuilder builder ->
            builder.'event'(
                    'xmlns:idfed': "http://docs.rackspace.com/event/identity/idp",
                    dataCenter: "DFW1",
                    eventTime: params.time,
                    id: params.eventId,
                    region: "DFW",
                    resourceId: params.idpId,
                    type: params.eventType,
                    version: "2") {
                'idfed:product'(resourceType: params.resourceType, serviceCode: params.serviceCode, version: "1", issuer: params.issuer)
            }

            builder
        }
    }

    static String buildXmlToString(Closure<MarkupBuilder> buildFunction) {
        def writer = new StringWriter()
        def xmlBuilder = new MarkupBuilder(writer)
        xmlBuilder.doubleQuotes = true

        buildFunction(xmlBuilder)

        writer.toString()
    }
}
