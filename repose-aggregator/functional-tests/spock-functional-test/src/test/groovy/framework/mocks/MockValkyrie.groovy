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
package framework.mocks
import groovy.text.SimpleTemplateEngine
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
/**
 * Simulates responses from a Valkyrie Server
 */
class MockValkyrie {

    public MockValkyrie(int port) {

        resetHandlers()
        this.port = port
    }

    int port

    boolean missingRequestHeaders = false;

    protected AtomicInteger _authorizeCount = new AtomicInteger(0);


    void resetCounts() {
        _authorizeCount.set(0)
    }

    public int getAuthorizationCount() {
        return _authorizeCount.get()
    }

    void resetHandlers() {
        handler = this.&handleRequest
        authorizeHandler = this.&authorize
    }

    Closure<Response> authorizeHandler

    String device_id = ""
    String device_perm = ""

    //remove
    def client_token = 'this-is-the-token';
    def client_apikey = 'this-is-the-api-key';

    def sleeptime =0;

    def templateEngine = new SimpleTemplateEngine();

    def handler = { Request request -> return handleRequest(request) }

    Response handleRequest(Request request) {

        /*
         *
         * GET
         * account/ {TenantId}/permissions/contacts/devices/by_contact/{ContactId}/effective
         * TenantId : Required
         * ContactId : Required
         * REQUIRED HEADERS: X-Auth-User: username, X-Auth-Token: password (not a GA token)
         * Provides device permissions based on the input parameters
         *
         */

        String requestPath = request.getPath()
        String method = request.getMethod()

        // remove
        requestPath = "/account/12345/permissions/contacts/devices/by_contact/67891/effective"

        def username
        def password
        if (!request.headers.contains('X-Auth-User'))
            missingRequestHeaders = true
        else
            username = request.headers.getFirstValue('X-Auth-User')
        if (!request.headers.contains('X-Auth-Token'))
            missingRequestHeaders = true
        else
            password = request.headers.getFirstValue('X-Auth-Token')

        // remove
        missingRequestHeaders = false

        // remove
        if (true) {
        //if (method == "GET") {
            if (!missingRequestHeaders) {

                def match = (requestPath =~ permissionsRegex)
                def tenant = match[0][1]
                def contact = match[0][2]
                _authorizeCount.incrementAndGet()
                return authorizeHandler(tenant, contact, request)
            } else {
                // return default error response if required headers are present
                return new Response(403);
            }

        }

        return new Response(501);
    }

    static final String permissionsRegex = /^\/account\/([^\/]+)\/permissions\/contacts\/devices\/by_contact\/([^\/]+)\/effective/

    Response authorize(String tenant, String contact, Request request) {

        def params = [
                contact     : contact,
                tenant      : tenant,
                deviceID   : device_id,
                permission : device_perm
        ];

        def code;
        def template;
        def headers = [:];

        headers.put('Content-type', 'application/json')

        if (!missingRequestHeaders) {
            code = 200;
            template = validationSuccessTemplate
        } else {
            code = 403
            template = validationFailureTemplate
        }

        def body = templateEngine.createTemplate(template).make(params)
        if(sleeptime > 0) {
            sleep(sleeptime)
        }
        return new Response(code, null, headers, body.toString().getBytes(StandardCharsets.UTF_8))
    }



    def validationFailureTemplate =
            """{
                "itemNotFound" : {
                "message" : "Permission Error.",
                "code" : 403
                    }
                }
            """

    def validationSuccessTemplate =
            """{
                    "contact_permissions": [
                        {
                            "account_number": \${tenant},
                            "contact_id": \${contact},
                            "id": 0,
                            "item_id": \${deviceID},
                            "item_type_id": 1,
                            "item_type_name": "devices",
                            "permission_name": "\${permission}",
                            "permission_type_id": 12
                        },
                        {
                            "account_number": \${tenant},
                            "contact_id": \${contact},
                            "id": 0,
                            "item_id": 504358,
                            "item_type_id": 1,
                            "item_type_name": "devices",
                            "permission_name": "view_product",
                            "permission_type_id": 12
                        },
                        {
                            "account_number": \${tenant},
                            "contact_id": \${contact},
                            "id": 0,
                            "item_id": 504360,
                            "item_type_id": 1,
                            "item_type_name": "devices",
                            "permission_name": "admin_product",
                            "permission_type_id": 14
                        },
                        {
                            "account_number": \${tenant},
                            "contact_id": \${contact},
                            "id": 0,
                            "item_id": 504362,
                            "item_type_id": 1,
                            "item_type_name": "devices",
                            "permission_name": "edit_product",
                            "permission_type_id": 13
                        }
                    ]
                }"""


}
