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
package org.openrepose.framework.test.mocks

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
        resetParameters()
        this.port = port
    }

    int port

    boolean missingRequestHeaders = false

    protected AtomicInteger _authorizeCount = new AtomicInteger(0)


    void resetCounts() {
        _authorizeCount.set(0)
    }

    public int getAuthorizationCount() {
        return _authorizeCount.get()
    }

    void resetHandlers() {
        handler = this.&handleRequest
        authorizeHandler = this.&authorize
        validationSuccess = this.&validationSuccessTemplate
    }

    void resetParameters() {
        device_id = "234567"
        device_id2 = "123456"
        device_perm = "butts"
        account_perm = "also_butts"
        contact_id = ""
        tenant_id = ""
        sleeptime = 0
        valid_auth = "valid-auth"
    }

    Closure<Response> authorizeHandler
    Closure<String> validationSuccess

    String device_id = "234567"
    String device_id2 = "123456"
    String device_perm = "butts"
    String account_perm = "also_butts"
    String contact_id = ""
    String tenant_id = ""
    String valid_auth = "valid-auth"
    Integer device_multiplier = 10
    Integer inventory_multiplier = 1

    def sleeptime = 0

    def templateEngine = new SimpleTemplateEngine()

    def handler = { Request request -> return handleRequest(request) }

    Response handleRequest(Request request) {

        /*
         *
         * GET
         * account device permission: account/ {TenantId}/permissions/contacts/devices/by_contact/{ContactId}/effective or
         * account level permission: account/ {TenantId}/permissions/contacts/account/by_contact/{ContactId}/effective
         * TenantId : Required
         * ContactId : Required
         * REQUIRED HEADERS: X-Auth-User: username, X-Auth-Token: password (not a GA token)
         * Provides device permissions based on the input parameters
         *
         */

        String requestPath = request.getPath()
        String method = request.getMethod()

        def username
        def password
        if (!request.headers.contains('X-Auth-User')) {
            missingRequestHeaders = !(request.headers.getFirstValue('X-Auth-Token') == valid_auth)
        } else {
            username = request.headers.getFirstValue('X-Auth-User')
        }
        if (!request.headers.contains('X-Auth-Token'))
            missingRequestHeaders = true
        else
            password = request.headers.getFirstValue('X-Auth-Token')

        if (method == "GET") {
            if (!missingRequestHeaders) {
                if (request.getPath().contains("inventory")) {
                    def match = (requestPath =~ inventoryRegex)
                    tenant_id = match[0][1]
                    contact_id = null
                    _authorizeCount.incrementAndGet()
                } else /*if (request.getPath().contains("permissions"))*/ {
                    def match = (requestPath =~ permissionsRegex)
                    tenant_id = match[0][1]
                    contact_id = match[0][2]
                    _authorizeCount.incrementAndGet()
                }
                return authorizeHandler(tenant_id, contact_id, request)
            } else {
                // return default error response if required headers are present
                return new Response(403)
            }

        }

        return new Response(501)
    }

    static
    final String inventoryRegex = /^\/account\/([^\/]+)\/inventory/

    static
    final String permissionsRegex = /^\/account\/([^\/]+)\/permissions\/contacts\/any\/by_contact\/([^\/]+)\/effective/

    Response authorize(String tenant, String contact, Request request) {

        def params = [
                contact     : contact,
                tenant      : tenant,
                deviceID    : device_id,
                deviceID2   : device_id2,
                permission  : device_perm,
                account_perm: account_perm,
        ]

        def code
        def headers = [:]

        headers.put('Content-type', 'application/json')

        def body
        if (!missingRequestHeaders) {
            code = 200
            if (request.getPath().contains("inventory")) {
                body = inventorySuccessTemplate(params)
            } else /*if (request.getPath().contains("permissions"))*/ {
                body = validationSuccess(params)
            }
        } else {
            code = 403
            body = validationFailureTemplate(params)
        }

        if (sleeptime > 0) {
            sleep(sleeptime)
        }
        return new Response(code, null, headers, body.toString().getBytes(StandardCharsets.UTF_8))
    }


    String validationFailureTemplate(def params) {
        """{
                "itemNotFound" : {
                "message" : "Permission Error.",
                "code" : 403
                    }
                }
            """
    }

    String validationSuccessTemplate(Map<String, String> params) {

        //Create the original template stuff and get a string of it
        String originalTemplate = templateEngine.createTemplate("""{
                    "contact_permissions": [
                        {
                            "item_type_id": 2,
                            "permission_type_id": 15,
                            "item_type_name": "devices",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_name": "\${permission}",
                            "item_id": \${deviceID},
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "permission_type_id": 12,
                            "item_type_name": "devices",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_name": "\${permission}",
                            "item_id": \${deviceID2},
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "permission_type_id": 9,
                            "item_type_name": "devices",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_name": "admin_product",
                            "item_id": 862323,
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "permission_type_id": 2,
                            "item_type_name": "devices",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_name": "edit_product",
                            "item_id": 862323,
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "permission_type_id": 9,
                            "item_type_name": "accounts",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_name": "upgrade_account",
                            "item_id": 862323,
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "permission_type_id": 2,
                            "item_type_name": "accounts",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_name": "edit_ticket",
                            "item_id": 862323,
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "permission_type_id": 6,
                            "item_type_name": "accounts",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_name": "view_domain",
                            "item_id": 862323,
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "item_type_name": "accounts",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_type_id": 17,
                            "permission_name": "view_reports",
                            "item_id": 862323,
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "item_type_name": "accounts",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_type_id": 10,
                            "permission_name": "manage_users",
                            "item_id": 862323,
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "item_type_name": "accounts",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_type_id": 7,
                            "permission_name": "edit_domain",
                            "item_id": 862323,
                            "id": 0
                        },
                        {
                            "item_type_id": 2,
                            "item_type_name": "accounts",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_type_id": 15,
                            "permission_name": "\${account_perm}",
                            "item_id": 862323,
                            "id": 0
                        },
                """).make(params).toString()

        //Build up a pile of hyoog json results
        StringBuilder lotsOJson = new StringBuilder()
        device_multiplier.times { x ->
            lotsOJson.append(templateEngine.createTemplate("""{
                            "item_type_id": 2,
                            "item_type_name": "devices",
                            "contact_id": \${contact},
                            "account_number": \${tenant},
                            "permission_type_id": 7,
                            "permission_name": "hyoog_json",
                            "item_id": ${x + 1000},
                            "id": 0
                        },""").make(params))
        }
        lotsOJson.deleteCharAt(lotsOJson.size() - 1) //Delete the pesky trailing comma

        //Now glue all the things together
        originalTemplate + lotsOJson + "]}"
    }

    String inventorySuccessTemplate(Map<String, String> params) {

        //Create the original template stuff and get a string of it
        String originalTemplate = templateEngine.createTemplate("""{
                "inventory": [
                    {
                        "status": "Online",
                        "datacenter": "Datacenter (ABC1)",
                        "name": "\${deviceID}-hyp1.abc.rvi.local",
                        "ipv6_network": "",
                        "type": "Server",
                        "primary_ipv4": "",
                        "primary_ipv6": "",
                        "primary_ipv4_gateway": "",
                        "datacenter_id": 1,
                        "platform": "Super Server",
                        "nickname": null,
                        "os": "Penguin Power",
                        "account_number": 11,
                        "primary_ipv4_netmask": "",
                        "id": \${deviceID},
                        "ipv6_server_allocation_block": "",
                        "permissions": [
                            "racker"
                        ]
                    },
                    {
                        "status": "Online",
                        "datacenter": "Datacenter (ABC1)",
                        "name": "\${deviceID2}-hyp1.abc.rvi.local",
                        "ipv6_network": "",
                        "type": "Server",
                        "primary_ipv4": "",
                        "primary_ipv6": "",
                        "primary_ipv4_gateway": "",
                        "datacenter_id": 1,
                        "platform": "Super Server",
                        "nickname": null,
                        "os": "Penguin Power",
                        "account_number": 11,
                        "primary_ipv4_netmask": "",
                        "id": \${deviceID2},
                        "ipv6_server_allocation_block": "",
                        "permissions": [
                            "racker"
                        ]
                    },
                """).make(params).toString()

        //Build up a pile of hyoog json results
        StringBuilder lotsOJson = new StringBuilder()
        inventory_multiplier.times { x ->
            lotsOJson.append(templateEngine.createTemplate("""{
                        "status": "Online",
                        "datacenter": "Datacenter (ABC1)",
                        "name": "${x + 5000}-hyp1.abc.rvi.local",
                        "ipv6_network": "",
                        "type": "Server",
                        "primary_ipv4": "",
                        "primary_ipv6": "",
                        "primary_ipv4_gateway": "",
                        "datacenter_id": 1,
                        "platform": "Super Server",
                        "nickname": null,
                        "os": "Penguin Power",
                        "account_number": 11,
                        "primary_ipv4_netmask": "",
                        "id": ${x + 5000},
                        "ipv6_server_allocation_block": "",
                        "permissions": [
                            "hyoog_json"
                        ]
                    },""").make(params))
        }
        lotsOJson.deleteCharAt(lotsOJson.size() - 1) //Delete the pesky trailing comma

        //Now glue all the things together
        originalTemplate + lotsOJson + "]}"
    }
}
