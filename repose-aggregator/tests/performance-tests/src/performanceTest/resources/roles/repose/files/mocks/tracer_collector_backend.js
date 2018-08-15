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
var app = express();

app.use (function(req, res, next) {
    var data = '';
    req.setEncoding('utf8');
    req.on('data', function(chunk) {
        data += chunk;
    });

    req.on('end', function() {
        req.body = data;
        next();
    });
});

app.disable('etag');

app.get('/*', function(req, res) {
    copyReqHeadersToRes(req, res)
    res.send(getResStatus(req, 200), '{"server":"obtained successfully"}');
});

app.put('/*', function(req, res) {
    res.set('content-type', 'application/atom+xml');
    res.set('x-pp-user', 'user1');
    copyReqHeadersToRes(req, res)
    res.send(getResStatus(req, 201), '{"server":"updated successfully"}');
});

app.delete('/*', function(req, res) {
    copyReqHeadersToRes(req, res)
    res.send(getResStatus(req, 204), '{"server":"deleted successfully"}');
});

app.post('/*', function(req, res) {
    copyReqHeadersToRes(req, res)
    res.send(getResStatus(req, 201), '{"server":"added successfully"}');
});

function copyReqHeadersToRes(req, res) {
    var doCopy = req.header('Copy-Req-Hdr-To-Res')
    if (doCopy && doCopy.trim().toLowerCase().startsWith('t')) {
        var headers = req.rawHeaders
        var len = (headers.length / 2);
        for (var i = 0; i < len; i++) {
            // Accounts for NodeJS clobbering duplicate header names with new values.
            res.set('ReqHdr-'+pad(i, 3)+'-'+headers[i*2], headers[(i*2)+1])
        }
    }
}

function pad(num, size) {
    var s = "000000000" + num;
    return s.substr(s.length-size);
}

function getResStatus(req, status) {
    var resStatusHdr = req.header('Mock-Origin-Res-Status')
    if (resStatusHdr) {
        return parseInt(resStatusHdr)
    } else {
        return status
    }
}

app.listen(10000);
