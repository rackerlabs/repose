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

var express = require('express');
require('date-utils');

var app = express();
app.disable('etag');

app.post('/v2.0/tokens', function(req, res) {
    var body = req.body;
    res.set('Content-Type', 'application/json');
    res.status(200).json({"access": {"serviceCatalog": [{"endpoints": [{"internalURL": "https://snet-storage.stg.swift.racklabs.com/v1/US_1234567","publicURL": "https://storage.stg.swift.racklabs.com/v1/US_1234567","region": "ORD","tenantId": "US_1234567"},{"internalURL": "https://snet-storage.stg.swift.racklabs.com/v1/US_1234567","publicURL": "https://storage.stg.swift.racklabs.com/v1/US_1234567","region": "DFW","tenantId": "US_1234567"}],"name": "cloudFiles","type": "object-store"},{"endpoints": [{"internalURL": "https://service-internal.com/v1/1234567","publicURL": "https://service-public.com/v1/1234567","region": "DFW","tenantId": "1234567"}],"name": "testEndpoint123456","type": "MOSSO"}],"token": {"RAX-AUTH:authenticatedBy": ["PASSWORD"],"expires": "2016-01-27T03:58:00.670Z","id": "legit-token","tenant": {"id": "1234567","name": "1234567"}},"user": {"RAX-AUTH:defaultRegion": "ORD","id": "24b5848caea44c31890d5a2018247342","name": "complex:admin","roles": [{"description": "A Role that allows a user access to keystone Service methods","id": "5","name": "object-store:default","tenantId": "US_1234567"},{"description": "A Role that allows a user access to keystone Service methods","id": "684","name": "compute:default","tenantId": "1234567"},{"description": "User Admin Role.","id": "3","name": "identity:user-admin" }]}}});
});

app.get('/v2.0/tokens/:token_id', function(req, res) {
    var token = req.params.token_id;
    var tempDate = new Date();

    var date = tempDate.addDays(1).toFormat('YYYY-MM-DDTHH24:MI:SSZ');
    var user_id = token.substr(0, 10);
    res.set('Content-Type', 'application/json');
    res.status(200).json({"access": {"serviceCatalog": [{"endpoints": [{"internalURL": "https://snet-storage.stg.swift.racklabs.com/v1/US_1234567","publicURL": "https://storage.stg.swift.racklabs.com/v1/US_1234567","region": "ORD","tenantId": "US_1234567"},{"internalURL": "https://snet-storage.stg.swift.racklabs.com/v1/US_1234567","publicURL": "https://storage.stg.swift.racklabs.com/v1/US_1234567","region": "DFW","tenantId": "US_1234567"}],"name": "cloudFiles","type": "object-store"},{"endpoints": [{"internalURL": "https://service-internal.com/v1/1234567","publicURL": "https://service-public.com/v1/1234567","region": "DFW","tenantId": "1234567"}],"name": "testEndpoint123456","type": "MOSSO"}],"token": {"RAX-AUTH:authenticatedBy": ["PASSWORD"],"expires": date,"id": token,"tenant": {"id": user_id,"name": user_id}},"user": {"RAX-AUTH:defaultRegion": "ORD","id": user_id,"name": "username","roles": [{"description": "A Role that allows a user access to keystone Service methods","id": "5","name": "object-store:default","tenantId": user_id},{"description": "A Role that allows a user access to keystone Service methods","id": "684","name": "compute:default","tenantId": user_id},{"description": "Bogus User Role 1.","id": "101","name": "bogus:user-101","tenantId": "BOGUS_101"},{"description": "Bogus User Role 2.","id": "102","name": "bogus:user-102","tenantId": "BOGUS_102"},{"description": "Bogus User Role 3.","id": "102","name": "bogus:user-103","tenantId": "BOGUS_103"},{"description": "User Admin Role.","id": "3","name": "identity:user-admin" }]}}});
});

app.get('/v2.0/users/:user_id/RAX-KSGRP', function(req, res) {
    res.set('Content-Type','application/json');
    var user = req.params.user_id;
    res.status(200).json({"RAX-KSGRP:groups":[{"id":"0","name":"Default","description":"Default Limits"}],"RAX-KSGRP:groups_links":[]});
});

app.get('/', function(req, res) {
    res.send('hello world');
});

app.listen(9090);
