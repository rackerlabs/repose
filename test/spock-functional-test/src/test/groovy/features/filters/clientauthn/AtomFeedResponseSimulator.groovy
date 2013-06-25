package features.filters.clientauthn

import groovy.text.SimpleTemplateEngine
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Response

/**
 * Simulates responses from an Identity Atom Feed
 */
class AtomFeedResponseSimulator {
    final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    def templateEngine = new SimpleTemplateEngine()
    boolean hasEntry = false
    int atomPort

    def client_token = 'this-is-the-token'
    def client_tenant = 'this-is-the-tenant'

    AtomFeedResponseSimulator(int atomPort) {
        this.atomPort = atomPort
    }

    def handler = { request ->

        def template

        if (hasEntry)
            template = atomWithEntryXml
        else
            template = atomEmptyXml

        def now = new DateTime()

        def params = [
                'atom_port': atomPort,
                'time': now.toString(DATE_FORMAT),
                'token': client_token,
                'tenant': client_tenant,
        ]

        def headers = [
                'Connection': 'close',
                'Content-type': 'application/xml',
        ]
        hasEntry = false
        def body = templateEngine.createTemplate(template).make(params)

        return new Response(200, 'OK', headers, body)
    }


    def String atomEmptyXml =
"""<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
    <link href="http://localhost:\${atom_port}/feed/"
        rel="current"/>
    <link href="http://localhost:\${atom_port}/feed/"
        rel="self"/>
    <id>urn:uuid:12345678-9abc-def0-1234-56789abcdef0</id>
    <title type="text">feed</title>
    <link href="http://localhost:\${atom_port}/feed/?marker=last&amp;limit=25&amp;search=&amp;direction=backward"
          rel="last"/>
    <updated>\${time}</updated>
</feed>
"""

    def String atomWithEntryXml =
"""<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
    <link href="http://localhost:\${atom_port}/feed/"
        rel="current"/>
    <link href="http://localhost:\${atom_port}/feed/"
        rel="self"/>
    <id>urn:uuid:12345678-9abc-def0-1234-56789abcdef0</id>
    <title type="text">feed</title>
    <link href="http://localhost:\${atom_port}/feed/?marker=urn:uuid:1&amp;limit=25&amp;search=&amp;direction=forward"
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

}
