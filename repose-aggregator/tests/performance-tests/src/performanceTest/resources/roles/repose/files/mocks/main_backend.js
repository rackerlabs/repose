var sleep = require('sleep');
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
    res.set('content-type', 'application/atom+xml');
    res.set('x-pp-user', 'user1');
    copyReqHeadersToRes(req, res)
    res.send(getResStatus(req, 201), '<?xml version="1.0" encoding="UTF-8"?>' +
        '<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns="http://docs.rackspace.com/core/event" xmlns:cb-bin="http://docs.rackspace.com/usage/cloudbackup/bandwidthIn">' +
        '<atom:id>urn:uuid:8d89673c-c989-11e1-895a-0b3d632a8a89</atom:id>' +
        '<atom:category term="tid:1234" />' +
        '<atom:category term="rgn:DFW" />' +
        '<atom:category term="dc:DFW1" />' +
        '<atom:category term="rid:3863d42a-ec9a-11e1-8e12-df8baa3ca440" />' +
        '<atom:category term="cloudbackup.bandwidthIn.agent.usage" />' +
        '<atom:category term="type:cloudbackup.bandwidthIn.agent.usage" />' +
        '<atom:content type="application/xml">' +
        '<event dataCenter="DFW1" endTime="2012-06-15T10:19:52Z" environment="PROD" id="8d89673c-c989-11e1-895a-0b3d632a8a89" region="DFW" resourceId="3863d42a-ec9a-11e1-8e12-df8baa3ca440" startTime="2012-06-14T10:19:52Z" tenantId="1234" type="USAGE" version="1">' +
        '<cb-bin:product bandwidthIn="192998" resourceType="AGENT" serverID="944576fa-ec99-11e1-bb8e-ebb21b47fa86" serviceCode="CloudBackup" version="1" />' +
        '</event>' +
        '</atom:content>' +
        '<atom:link href="https://ord.feeds.api.rackspacecloud.com/backup/events/entries/urn:uuid:8d89673c-c989-11e1-895a-0b3d632a8a89" rel="self" />' +
        '<atom:updated>2013-02-28T19:28:57.758Z</atom:updated>' +
        '<atom:published>2013-02-28T19:28:57.758Z</atom:published>' +
        '</atom:entry>');
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

app.listen(8080);
