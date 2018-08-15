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

const bodyParser = require('body-parser')
const express = require('express');
const libxml = require('libxmljs');
const uuidV4 = require('uuid/v4');
const fs = require("fs");
require('date-utils');

const app = express();
app.disable('etag');

const largeYamlPolicy = readLargePolicyFile('large-policy.yaml');

// Always parse the body as a string using the body-parser middleware.
app.use(bodyParser.text({ type: '*/*' }))

function readLargePolicyFile(filename) {
    try {
        return fs.readFileSync(filename);
    } catch(err) {
        return undefined;
    }
}

function createIdpJsonWithValues(values = {}) {
    // Define a function to generate unique IDs.
    const generateUniqueIdpId = function() {
        return uuidV4().replace(/-/g, '');
    };

    // Return the IDP JSON string.
    return JSON.stringify({
        'RAX-AUTH:identityProviders': [
            {
                name: values.name || 'External IDP',
                federationType: values.federationType || 'DOMAIN',
                approvedDomainIds: values.approvedDomainIds || ['77366'],
                description: values.description || 'An External IDP Description',
                id: values.id || generateUniqueIdpId(),
                issuer: values.issuer || 'http://idp.external.com'
            }
        ]
    });
}

function createMappingJsonWithDefaults() {
    return `{
  "mapping": {
    "rules": [
      {
        "remote": [
          {
            "name":"http://schemas.xmlsoap.org/claims/Group",
            "multiValue":true
          },
          {
            "name":"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"
          }
        ],
        "local": {
          "user": {
            "domain":"5821006",
            "name":"{D}",
            "email":"{1}",
            "roles": {
              "value":"{0}",
              "multiValue":true
            },
            "expire":"{D}"
          }
        }
      },
      {
        "remote": [
          {
            "name":"http://schemas.xmlsoap.org/claims/Group",
            "anyOneOf": ["test_group_2"]
          }
        ],
        "local": {
          "faws": {
            "canAddAWSAccount":true,
            "991049284483": [
              "fanatical_aws:admin",
              "AdminstratorAccess"
            ],
            "042423532529": [
              "fanatical_aws:observer",
              "RackspaceReadOnly"
            ]
          }
        }
      }
    ],
    "version":"RAX-1"
  }
}`;
}

function createMappingJsonWithValues(values = {}) {
    // Create the initial mapping object.
    const mapping = {
        version: 'RAX-1',
        description: values.description || 'Default description',
        rules: [
            {
                local: {
                    user: {
                        domain: values.domain || '{D}',
                        email: values.email || '{D}',
                        expire: values.expire || '{D}',
                        name: values.name || '{D}',
                        roles: values.roles || '{D}'
                    }
                }
            }
        ].concat(values.rules || [])
    };

    // Add extra attributes to the user property, if provided.
    if (values.userExtAttribs) {
        for (const property in values.userExtAttribs) {
            mapping.rules[0].local.user[property] = values.userExtAttribs[property];
        }
    }

    // Add properties to the local property, if provided.
    if (values.local) {
        for (const property in values.local) {
            mapping.rules[0].local[property] = values.local[property];
        }
    }

    // Set the remote property, if provided.
    if (values.remote) {
        mapping.rules[0].remote = values.remote;
    }

    // Return the mapping JSON string.
    return JSON.stringify({ mapping: mapping });
}

function createAccessJsonWithDefaults() {
    return `{
    "access": {
        "serviceCatalog": [
            {
                "endpoints": [
                    {
                        "publicURL": "https://ord.servers.api.rackspacecloud.com/v2/default-token-tenant-id",
                        "region": "ORD",
                        "tenantId": "default-token-tenant-id",
                        "versionId": "2",
                        "versionInfo": "https://ord.servers.api.rackspacecloud.com/v2",
                        "versionList": "https://ord.servers.api.rackspacecloud.com/"
                    },
                    {
                        "publicURL": "https://dfw.servers.api.rackspacecloud.com/v2/default-token-tenant-id",
                        "region": "DFW",
                        "tenantId": "default-token-tenant-id",
                        "versionId": "2",
                        "versionInfo": "https://dfw.servers.api.rackspacecloud.com/v2",
                        "versionList": "https://dfw.servers.api.rackspacecloud.com/"
                    }
                ],
                "name": "cloudServersOpenStack",
                "type": "compute"
            }
        ],
        "token": {
            "RAX-AUTH:authenticatedBy": [
                "FEDERATION",
                "PASSWORD"
            ],
            "expires": "2017-02-16T13:28:07Z",
            "id": "default-token",
            "tenant": {
                "id": "default-token-tenant-id",
                "name": "default-token-tenant-id"
            }
        },
        "user": {
            "RAX-AUTH:contactId": "default-token-contact-id",
            "RAX-AUTH:defaultRegion": "the-default-region",
            "id": "default-token-user-id",
            "name": "john.doe",
            "roles": [
                {
                    "id": "0",
                    "name": "nova:admin"
                }
            ]
        }
    }
}`;
}

function createAccessJsonWithValues(values = {}) {
    // Parse parameters and set defaults.
    const token = values.token || 'default-token';
    const expires = values.expires || new Date().addDays(1).toFormat('YYYY-MM-DDTHH24:MI:SSZ');
    const tenantId = values.tenantId || token.substr(0, 20) + '-tenant-id';
    const userId = values.userId || token.substr(0, 20) + '-user-id';
    const username = values.username || 'default-username';
    const contactId = values.contactId || token.substr(0, 20) + '-contact-id';
    const roleNames = values.roles || ['identity:admin'];

    // Create and return the access object.
    return JSON.stringify({
        access: {
            token: {
                id: token,
                expires: expires,
                tenant: {
                    id: tenantId,
                    name: tenantId
                },
                'RAX-AUTH:authenticatedBy': function() {
                    return values.authBy || undefined;
                }()
            },
            user: {
                id: userId,
                name: username,
                'RAX-AUTH:defaultRegion': 'the-default-region',
                'RAX-AUTH:contactId': contactId,
                roles: function() {
                    return roleNames.reduce(function(acc, cur, i) {
                        acc[i] = { id: i.toString(), name: cur };
                        return acc;
                    }, []);
                }()
            },
            serviceCatalog: [
                {
                    name: 'cloudServersOpenStack',
                    type: 'compute',
                    endpoints: [
                        {
                            publicURL: 'https://ord.servers.api.rackspacecloud.com/v2/' + tenantId,
                            region: 'ORD',
                            tenantId: tenantId,
                            versionId: '2',
                            versionInfo: 'https://ord.servers.api.rackspacecloud.com/v2',
                            versionList: 'https://ord.servers.api.rackspacecloud.com/'
                        },
                        {
                            publicURL: 'https://dfw.servers.api.rackspacecloud.com/v2/' + tenantId,
                            region: 'DFW',
                            tenantId: tenantId,
                            versionId: '2',
                            versionInfo: 'https://dfw.servers.api.rackspacecloud.com/v2',
                            versionList: 'https://dfw.servers.api.rackspacecloud.com/'
                        }
                    ]
                }
            ]
        }
    });
}

function createAccessXmlWithDefaults() {
    return `<?xml version="1.0" encoding="UTF-8"?><access xmlns="http://docs.openstack.org/identity/api/v2.0" xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
  <token expires="2017-02-15T17:19:48Z" id="default-token">
    <tenant id="default-token-tenant-id" name="default-token-tenant-id"/>
    <rax-auth:authenticatedBy>
      <rax-auth:credential>FEDERATION</rax-auth:credential>
      <rax-auth:credential>PASSWORD</rax-auth:credential>
    </rax-auth:authenticatedBy>
  </token>
  <user id="default-token-user-id" name="john.doe" rax-auth:contactId="default-token-contact-id" rax-auth:defaultRegion="the-default-region">
    <roles>
      <role id="0" name="nova:admin"/>
    </roles>
  </user>
  <serviceCatalog>
    <service name="cloudServersOpenStack" type="compute">
      <endpoint publicURL="https://ord.servers.api.rackspacecloud.com/v2/default-token-tenant-id" region="ORD" tenantId="default-token-tenant-id" versionId="2" versionInfo="https://ord.servers.api.rackspacecloud.com/v2" versionList="https://ord.servers.api.rackspacecloud.com/"/>
      <endpoint publicURL="https://dfw.servers.api.rackspacecloud.com/v2/default-token-tenant-id" region="DFW" tenantId="default-token-tenant-id" versionId="2" versionInfo="https://dfw.servers.api.rackspacecloud.com/v2" versionList="https://dfw.servers.api.rackspacecloud.com/"/>
    </service>
  </serviceCatalog>
</access>`;
}

function createAccessXmlWithValues(values = {}) {
    // Parse parameters and set defaults.
    const token = values.token || 'default-token';
    const expires = values.expires || new Date().addDays(1).toFormat('YYYY-MM-DDTHH24:MI:SSZ');
    const tenantId = values.tenantId || token.substr(0, 20) + '-tenant-id';
    const userId = values.userId || token.substr(0, 20) + '-user-id';
    const username = values.username || 'default-username';
    const contactId = values.contactId || token.substr(0, 20) + '-contact-id';
    const roleNames = values.roles || ['identity:admin'];
    const namespaces = {
        '': 'urn:oasis:names:tc:SAML:2.0:protocol',
        'rax-auth': 'urn:oasis:names:tc:SAML:2.0:assertion'
    }

    // Create the access document.
    const doc = new libxml.Document();
    doc.node('access')
        .node('token').attr({id: token, expires: expires})
            .node('tenant').attr({id: tenantId, name: tenantId})
            .parent()
        .parent()
        .node('user').attr({id: userId, name: username, 'rax-auth:defaultRegion': 'the-default-region', 'rax-auth:contactId': contactId})
            .node('roles')
            .parent()
        .parent()
        .node('serviceCatalog')
            .node('service').attr({type: 'compute', name: 'cloudServersOpenStack'})
                .node('endpoint').attr({region: 'ORD', tenantId: tenantId, publicURL: 'https://ord.servers.api.rackspacecloud.com/v2/' + tenantId, versionId: '2', versionInfo: 'https://ord.servers.api.rackspacecloud.com/v2', versionList: 'https://ord.servers.api.rackspacecloud.com/'})
                .parent()
                .node('endpoint').attr({region: 'DFW', tenantId: tenantId, publicURL: 'https://dfw.servers.api.rackspacecloud.com/v2/' + tenantId, versionId: '2', versionInfo: 'https://dfw.servers.api.rackspacecloud.com/v2', versionList: 'https://dfw.servers.api.rackspacecloud.com/'});

    // Add the authenticatedBy node if applicable.
    const tokenNode = doc.get('/access/token');
    if (values.authBy.length !== 0) {
        const authByNode = tokenNode.node('rax-auth:authenticatedBy');
        values.authBy.forEach(function(s) {
            authByNode.node('rax-auth:credential', s);
        });
    }

    // Add the role nodes if applicable.
    const rolesNode = doc.get('/access/user/roles');
    roleNames.reduce(function(acc, cur, i) {
        acc[i] = { id: i.toString(), name: cur };
        return acc;
    }, []).forEach(function(role) {
        rolesNode.node('role').attr({name: role.name, id: role.id});
    });

    // Add the XML namespaces.
    doc.root().defineNamespace('http://docs.openstack.org/identity/api/v2.0');
    doc.root().defineNamespace('rax-auth', 'http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0');

    return doc.toString();
}

app.post('/v2.0/RAX-AUTH/federation/saml/auth', function (req, res) {
    const headerAccept = req.get('accept');
    if (headerAccept == undefined || headerAccept.indexOf('json') < 0) {
        res.set('Content-Type', 'application/xml');
        res.status(200).send(createAccessXmlWithDefaults());
    } else {
        res.set('Content-Type', 'application/json');
        res.status(200).send(createAccessJsonWithDefaults());
    }
});

app.get('/v2.0/RAX-AUTH/federation/identity-providers', function (req, res) {
    res.set('Content-Type', 'application/json');
    res.status(200).send(createIdpJsonWithValues({
        issuer: req.query.issuer
    }));
});

app.get('/v2.0/RAX-AUTH/federation/identity-providers/:idp_id/mapping', function (req, res) {
    if (largeYamlPolicy != undefined) {
        res.set('Content-Type', 'text/yaml');
        res.status(200).send(largeYamlPolicy);
    } else {
        res.set('Content-Type', 'application/json');
        res.status(200).send(createMappingJsonWithDefaults());
    }
});

app.post('/v2.0/tokens', function (req, res) {
    const body = req.body;
    const token = 'this-is-the-admin-token';
    const expires = new Date().addDays(1).toFormat('YYYY-MM-DDTHH24:MI:SSZ');
    const tenantid = 'this-is-the-admin-tenant-id';
    const tenantname = 'this-is-the-admin-tenant-name';
    const userid = '67890';
    const username = 'admin-username';
    const contactid = 654321;

    const headerAccept = req.get('accept');
    if (headerAccept == undefined || headerAccept.indexOf('json') < 0) {
        res.set('Content-Type', 'application/xml');
        res.status(200).send(createAccessXmlWithValues({token: token, expires: expires, tenantId: tenantid, userId: userid, username: username, contactId: contactid}));
    } else {
        res.set('Content-Type', 'application/json');
        res.status(200).send(createAccessJsonWithValues({token: token, expires: expires, tenantId: tenantid, userId: userid, username: username, contactId: contactid}));
    }
});

app.get('/v2.0/tokens/:token_id', function (req, res) {
    const token = req.params.token_id;
    const expires = new Date().addDays(1).toFormat('YYYY-MM-DDTHH24:MI:SSZ');
    const tenantid = 'hybrid:' + token.substr(0, 10);
    const tenantname = 'this-is-the-tenant-name';
    const userid = token.substr(0, 10);
    const username = 'username';
    const contactid = 654321;

    const headerAccept = req.get('accept');
    if (headerAccept == undefined || headerAccept.indexOf('json') < 0) {
        res.set('Content-Type', 'application/xml');
        res.status(200).send(createAccessXmlWithValues({token: token, expires: expires, tenantId: tenantid, userId: userid, username: username, contactId: contactid}));
    } else {
        res.set('Content-Type', 'application/json');
        res.status(200).send(createAccessJsonWithValues({token: token, expires: expires, tenantId: tenantid, userId: userid, username: username, contactId: contactid}));
    }
});

app.get('/v2.0/users/:user_id/RAX-KSGRP', function (req, res) {
    const headerAccept = req.get('accept');
    if (headerAccept !== undefined && headerAccept.indexOf('json') > -1) {
        res.set('Content-Type', 'application/json');
        res.status(200).send('{' +
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
        res.status(200).send('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
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

const svr = app.listen(9090, function () {
    const host = svr.address().address;
    const port = svr.address().port;

    console.log('Listening at http://%s:%s', host, port);
});
