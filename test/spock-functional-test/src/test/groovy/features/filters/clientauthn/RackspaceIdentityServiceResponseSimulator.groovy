package features.filters.clientauthn

import groovy.text.SimpleTemplateEngine
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

/**
 * Simulates responses from an Identity Service
 */
class RackspaceIdentityServiceResponseSimulator {

    final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    boolean ok = true;
    def tokenExpiresAt = null;

    int errorCode;
    boolean isAbleToGetGroups = true;
    boolean isTokenAuthenticated = true;

    def port = 12200
    def origin_service_port = 10001

    def client_token = 'token';
    def client_username = 'username';
    def belongs_to_user_id

    def templateEngine = new SimpleTemplateEngine();


    def handler = { Request request ->
        def xml = false

        request.headers.findAll('Accept').each { values ->
            if (values.contains('application/xml')) {
                xml = true
            }
        }

        def params = [:]

        // default response code and message
        def template
        def headers = ['Connection': 'close']
        def code = 200
        def message = 'OK'

        if (request.method == "GET" && request.path.contains("belongsTo")) {
            return handleBelongsToCall(request);
        } else if (request.method == "GET" && request.path.endsWith("groups")) {
            return handleGroupsCall(request);
        } else {
            throw new UnsupportedOperationException('Unknown request: %r' % request)
        }
    }

    Response handleBelongsToCall(Request request) {
        // check if token is authenticated and throw back a 404 if it isn't
        // e.g. GET, /token/[token]?belongsTo=random&type=CLOUD, HTTP/1.1

        def path = request.getPath()
        belongs_to_user_id = path.substring(path.indexOf("belongsTo=") + 10, path.indexOf("&type"))

        def params = [
                expires: getExpires(),
                userid: belongs_to_user_id,
                username: client_username,
                type: "user",
                success: isTokenAuthenticated ,
                token: client_token
        ];

        def xml = false

        request.headers.findAll('Accept').each { values ->
            if (values.contains('application/xml')) {
                xml = true
            }
        }

        def code = 200;
        def template;
        def headers = ['Connection': 'close'];

        if (xml) {
            headers.put('Content-type', 'application/xml')
        } else {
            headers.put('Content-type', 'application/json')
        }

        if(isTokenAuthenticated){
            //authenticated
            if(belongs_to_user_id != null && !belongs_to_user_id.isEmpty() &&  !belongs_to_user_id.equals("null") &&
                    (client_token != null && !client_token.isEmpty())){
                if(xml){
                    template = identitySuccessXmlTemplate
                } else{
                    template = identitySuccessJsonTemplate
                }
            }else{
                code = 404
                if(xml){
                    template = identityNotFoundXmlTemplate
                } else {
                    template = identityNotFoundJsonTemplate
                }
            }
        } else{
            //failed auth
            code = 401
            if(xml){
                template = identityNotAuthorizedXmlTemplate
            } else {
                template = identityNotAuthorizedJsonTemplate
            }
        }


        def body = templateEngine.createTemplate(template).make(params)

        return new Response(code, null, headers, body)
    }



Response handleGroupsCall(Request request) {
    if(!isAbleToGetGroups)
        return new Response(500)

    def path = request.getPath()

    def params = [
            expires: getExpires(),
            userid: belongs_to_user_id,
            username: client_username,
            type: "user",
            success: isTokenAuthenticated,
            token: client_token
    ];

    def xml = false

    request.headers.findAll('Accept').each { values ->
        if (values.contains('application/xml')) {
            xml = true
        }
    }

    def code = 200;
    def template;
    def headers = ['Connection': 'close'];

    if (xml) {
        headers.put('Content-type', 'application/xml')
    } else {
        headers.put('Content-type', 'application/json')
    }

    if(xml){
        template = groupsXmlTemplate
    } else{
        template = groupsJsonTemplate
    }


    def body = templateEngine.createTemplate(template).make(params)

    return new Response(code, null, headers, body)

}




def groupsJsonTemplate =
    """{
"RAX-KSGRP:groups": [
{
    "id": "0",
    "description": "Default Limits",
    "name": "Default"
}
]
}
"""

def groupsXmlTemplate =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<groups xmlns="http://docs.rackspacecloud.com/auth/api/v1.1">
<group id="Admin"><description>Unknown</description></group>
<group id="Technical"><description>Unknown</description></group></groups>
"""

def identityNotFoundJsonTemplate =
    """{
"itemNotFound" : {
  "message" : "Invalid Token, not found.",
  "code" : 404
}
}
"""

    def identityNotAuthorizedJsonTemplate =
        """{
    "itemNotFound" : {
      "message" : "Username or api key is invalid",
      "code" : 401
    }
    }
    """

def identityNotFoundXmlTemplate =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<itemNotFound xmlns="http://docs.rackspacecloud.com/auth/api/v1.1" code="404">
<message>Token not found.</message><details></details></itemNotFound>
"""
    def identityNotAuthorizedXmlTemplate =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <unauthorized xmlns="http://docs.rackspacecloud.com/auth/api/v1.1" code="401">
            <message>Username or api key is invalid</message><details></details></unauthorized>"""


def identitySuccessJsonTemplate =
    """{
"token" : {
  "created" : "2013-09-03T18:48:39.638Z",
  "userId" : "\${def belongs_to_user_id}",
  "userURL" : "/user/url",
  "region" : "DFW",
  "id" : "\${token}",
  "expires" : "\${expires}"
}
}
"""

def identitySuccessXmlTemplate =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<token xmlns="http://docs.rackspacecloud.com/auth/api/v1.1" created="2013-09-03T18:48:39.638Z"
userId="\${def belongs_to_user_id}" userURL="/user/url" id="\${token}" expires="\${expires}"/>"""

    String getExpires() {


        if (this.tokenExpiresAt != null && this.tokenExpiresAt instanceof String) {

            return this.tokenExpiresAt;

        } else if (this.tokenExpiresAt instanceof DateTime) {

            DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_FORMAT).withLocale(Locale.US).withZone(DateTimeZone.UTC);
            return fmt.print(tokenExpiresAt)

        } else if (this.tokenExpiresAt) {

            return this.tokenExpiresAt.toString();

        } else {

            def now = new DateTime()
            def nowPlusOneDay = now.plusDays(1)
            return nowPlusOneDay;
        }
    }

}
