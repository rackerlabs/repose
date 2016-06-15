var express = require('express');
var libxmljs = require('libxmljs');
require('date-utils');

var app = express();
app.disable('etag');

function generateJsonTokenResponse(token, expires, tenantid, tenantidtwo, tenantname, userid, username) {
    console.log('JSON: token=%s', token);
    console.log('JSON: tenantId=%s', tenantid);
    return '{' +
        '  "access" : {' +
        '    "serviceCatalog" : [' +
        '      {' +
        '        "name": "cloudServersOpenStack",' +
        '        "type": "compute",' +
        '        "endpoints": [' +
        '          {' +
        '            "publicURL": "https://ord.servers.api.rackspacecloud.com/v2/' + tenantid + '",' +
        '            "region": "ORD",' +
        '            "tenantId": "' + tenantid + '",' +
        '            "versionId": "2",' +
        '            "versionInfo": "https://ord.servers.api.rackspacecloud.com/v2",' +
        '            "versionList": "https://ord.servers.api.rackspacecloud.com/"' +
        '          },' +
        '          {' +
        '            "publicURL": "https://dfw.servers.api.rackspacecloud.com/v2/' + tenantid + '",' +
        '            "region": "DFW",' +
        '            "tenantId": "' + tenantid + '",' +
        '            "versionId": "2",' +
        '            "versionInfo": "https://dfw.servers.api.rackspacecloud.com/v2",' +
        '            "versionList": "https://dfw.servers.api.rackspacecloud.com/"' +
        '          }' +
        '        ]' +
        '      },' +
        '      {' +
        '        "name" : "cloudFilesCDN",' +
        '        "type" : "rax:object-cdn",' +
        '        "endpoints" : [' +
        '          {' +
        '            "publicURL" : "https://cdn.stg.clouddrive.com/v1/' + tenantidtwo + '",' +
        '            "tenantId" : "' + tenantidtwo + '",' +
        '            "region" : "DFW"' +
        '          },' +
        '          {' +
        '            "publicURL" : "https://cdn.stg.clouddrive.com/v1/' + tenantidtwo + '",' +
        '            "tenantId" : "' + tenantidtwo + '",' +
        '            "region" : "ORD"' +
        '          }' +
        '        ]' +
        '      },' +
        '      {' +
        '        "name" : "cloudFiles",' +
        '        "type" : "object-store",' +
        '        "endpoints" : [' +
        '          {' +
        '            "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/' + tenantidtwo + '",' +
        '            "publicURL" : "https://storage.stg.swift.racklabs.com/v1/' + tenantidtwo + '",' +
        '            "tenantId" : "' + tenantidtwo + '",' +
        '            "region" : "ORD"' +
        '          },' +
        '          {' +
        '            "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/' + tenantidtwo + '",' +
        '            "publicURL" : "https://storage.stg.swift.racklabs.com/v1/' + tenantidtwo + '",' +
        '            "tenantId" : "' + tenantidtwo + '",' +
        '            "region" : "DFW"' +
        '          }' +
        '        ]' +
        '      }' +
        '    ],' +
        '    "user" : {' +
        '      "roles" : [' +
        '        {' +
        '          "tenantId" : "' + tenantid + '",' +
        '          "name" : "compute:default",' +
        '          "id" : "684",' +
        '          "description" : "A Role that allows a user access to keystone Service methods"' +
        '        },' +
        '        {' +
        '          "tenantId" : "' + tenantidtwo + '",' +
        '          "name" : "object-store:default",' +
        '          "id" : "5",' +
        '          "description" : "A Role that allows a user access to keystone Service methods"' +
        '        },' +
        '        {' +
        '          "name" : "identity:admin",' +
        '          "id" : "1",' +
        '          "description" : "Admin Role."' +
        '        }' +
        '      ],' +
        '      "RAX-AUTH:defaultRegion" : "the-default-region",' +
        '      "name" : "' + username + '",' +
        '      "id" : "' + userid + '"' +
        '    },' +
        '    "token" : {' +
        '      "tenant" : {' +
        '        "id" : "' + tenantid + '",' +
        '        "name" : "' + tenantname + '"' +
        '      },' +
        '      "id" : "' + token + '",' +
        '      "expires" : "' + expires + '"' +
        '    }' +
        '  }' +
        '}'
}

function generateXmlTokenResponse(token, expires, tenantid, tenantidtwo, tenantname, userid, username) {
    console.log('XML: token=%s', token);
    console.log('XML: tenantId=%s', tenantid);
    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<access xmlns="http://docs.openstack.org/identity/api/v2.0">' +
        '    <token id="' + token + '"' +
        '           expires="' + expires + '">' +
        '        <tenant id="' + tenantid + '"' +
        '                name="' + tenantname + '"/>' +
        '    </token>' +
        '    <user xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"' +
        '          id="' + userid + '"' +
        '          name="' + username + '"' +
        '          rax-auth:defaultRegion="the-default-region">' +
        '        <roles>' +
        '            <role id="684"' +
        '                  name="compute:default"' +
        '                  description="A Role that allows a user access to keystone Service methods"' +
        '                  serviceId="0000000000000000000000000000000000000001"' +
        '                  tenantId="' + tenantid + '"/>' +
        '            <role id="5"' +
        '                  name="object-store:default"' +
        '                  description="A Role that allows a user access to keystone Service methods"' +
        '                  serviceId="0000000000000000000000000000000000000002"' +
        '                  tenantId="' + tenantidtwo + '"/>' +
        '            <role id="6"' +
        '                  name="\${serviceadmin}"' +
        '                  description="A Role that allows a user access to keystone Service methods"' +
        '                  serviceId="0000000000000000000000000000000000000002"' +
        '                  tenantId="' + tenantid + '"/>' +
        '        </roles>' +
        '    </user>' +
        '    <serviceCatalog>' +
        '        <service type="compute"' +
        '                 name="cloudServersOpenStack">' +
        '            <endpoint region="ORD"' +
        '                      tenantId="' + tenantid + '"' +
        '                      publicURL="https://ord.servers.api.rackspacecloud.com/v2/' + tenantid + '"' +
        '                      versionId="2"' +
        '                      versionInfo="https://ord.servers.api.rackspacecloud.com/v2"' +
        '                      versionList="https://ord.servers.api.rackspacecloud.com/"/>' +
        '            <endpoint region="DFW"' +
        '                      tenantId="' + tenantid + '"' +
        '                      publicURL="https://dfw.servers.api.rackspacecloud.com/v2/' + tenantid + '"' +
        '                      versionId="2"' +
        '                      versionInfo="https://dfw.servers.api.rackspacecloud.com/v2"' +
        '                      versionList="https://dfw.servers.api.rackspacecloud.com/"/>' +
        '        </service>' +
        '        <service type="rax:object-cdn"' +
        '                 name="cloudFilesCDN">' +
        '            <endpoint region="DFW"' +
        '                      tenantId="' + tenantidtwo + '"' +
        '                      publicURL="https://cdn.stg.clouddrive.com/v1/' + tenantidtwo + '"/>' +
        '            <endpoint region="ORD"' +
        '                      tenantId="' + tenantidtwo + '"' +
        '                      publicURL="https://cdn.stg.clouddrive.com/v1/' + tenantidtwo + '"/>' +
        '        </service>' +
        '        <service type="object-store"' +
        '                 name="cloudFiles">' +
        '            <endpoint region="ORD"' +
        '                      tenantId="' + tenantidtwo + '"' +
        '                      publicURL="https://storage.stg.swift.racklabs.com/v1/' + tenantidtwo + '"' +
        '                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/' + tenantidtwo + '"/>' +
        '            <endpoint region="DFW"' +
        '                      tenantId="' + tenantidtwo + '"' +
        '                      publicURL="https://storage.stg.swift.racklabs.com/v1/' + tenantidtwo + '"' +
        '                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/' + tenantidtwo + '"/>' +
        '        </service>' +
        '    </serviceCatalog>' +
        '</access>';
}

// Successful generate token response
app.post('/v2.0/tokens', function (req, res) {
    var body = req.body;
    var token = 'this-is-the-admin-token';
    var expires = new Date().addDays(1).toFormat('YYYY-MM-DDTHH24:MI:SSZ');
    var tenantid = 'this-is-the-admin-tenant-id';
    var tenantname = 'this-is-the-admin-tenant-name';
    var tenantidtwo = 12345;
    var userid = '67890';
    var username = 'admin-username';

    var headerAccept = req.get('accept');
    if (headerAccept == undefined || headerAccept.indexOf('json') < 0) {
        res.set('Content-Type', 'application/xml');
        res.send(200, generateXmlTokenResponse(token, expires, tenantid, tenantidtwo, tenantname, userid, username));
    } else {
        res.set('Content-Type', 'application/json');
        res.send(200, generateJsonTokenResponse(token, expires, tenantid, tenantidtwo, tenantname, userid, username));
    }
});

app.get('/v2.0/tokens/:token_id', function (req, res) {
    var token = req.params.token_id;
    var expires = new Date().addDays(1).toFormat('YYYY-MM-DDTHH24:MI:SSZ');
    var tenantid = token.substr(0, 10);
    var tenantidtwo = 12345;
    var tenantname = 'this-is-the-tenant-name';
    var userid = token.substr(0, 10);
    var username = 'username';

    var headerAccept = req.get('accept');
    if (headerAccept == undefined || headerAccept.indexOf('json') < 0) {
        res.set('Content-Type', 'application/xml');
        res.send(200, generateXmlTokenResponse(token, expires, tenantid, tenantidtwo, tenantname, userid, username));
    } else {
        res.set('Content-Type', 'application/json');
        res.send(200, generateJsonTokenResponse(token, expires, tenantid, tenantidtwo, tenantname, userid, username));
    }
});

app.get('/v2.0/users/:user_id/RAX-KSGRP', function (req, res) {
    var headerAccept = req.get('accept');
    if (headerAccept !== undefined && headerAccept.indexOf('json') > -1) {
        res.set('Content-Type', 'application/json');
        res.send(200, '{' +
            '  "RAX-KSGRP:groups": [' +
            '    {' +
            '      "id": "0",' +
            '      "name": "Default",' +
            '      "description": "Default Limits"' +
            '    }' +
            '  ]' +
            '}'
        );
    } else {
        res.set('Content-Type', 'application/xml');
        res.send(200, '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
            '<groups xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0">' +
            ' <group id="0" name="Default">' +
            '   <description>Default Limits</description>' +
            ' </group>' +
            '</groups>');
    }
});

app.get('/', function (req, res) {
    res.send('hello world\n');
});

var svr = app.listen(9090, function () {
    var host = svr.address().address;
    var port = svr.address().port;

    console.log('Listening at http://%s:%s', host, port);
});
