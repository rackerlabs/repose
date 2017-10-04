const bodyParser = require('body-parser')
const express = require('express');
const libxml = require('libxmljs');
const uuidV4 = require('uuid/v4');
require('date-utils');

const app = express();
app.disable('etag');

// Always parse the body as a string using the body-parser middleware.
app.use(bodyParser.text({ type: '*/*' }))

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
    const samlResponse = libxml.parseXmlString(req.body);
    const namespaces = {
        saml2p: 'urn:oasis:names:tc:SAML:2.0:protocol',
        saml2: 'urn:oasis:names:tc:SAML:2.0:assertion'
    }

    const roles = samlResponse.find('/saml2p:Response/saml2:Assertion[1]/saml2:AttributeStatement/saml2:Attribute[@Name="roles"]//saml2:AttributeValue', namespaces)
        .map(function(element) {
            return element.text().trim();
        });
    const username = samlResponse.get('/saml2p:Response/saml2:Assertion[1]/saml2:Subject/saml2:NameID', namespaces).text().trim();
    const idpAuthBy = samlResponse.get('/saml2p:Response/saml2:Assertion[1]/saml2:AuthnStatement/saml2:AuthnContext/saml2:AuthnContextClassRef', namespaces).text().trim();

    const authBy = ['FEDERATION'];
    if (idpAuthBy === 'urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport') {
        authBy.push('PASSWORD');
    } else if (idpAuthBy === 'urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken') {
        authBy.push('RSAKEY');
    }

    const headerAccept = req.get('accept');
    if (headerAccept == undefined || headerAccept.indexOf('json') < 0) {
        res.set('Content-Type', 'application/xml');
        res.status(200).send(createAccessXmlWithValues({
            authBy: authBy,
            roles: roles,
            username: username
        }));
    } else {
        res.set('Content-Type', 'application/json');
        res.status(200).send(createAccessJsonWithValues({
            authBy: authBy,
            roles: roles,
            username: username
        }));
    }
});

app.get('/v2.0/RAX-AUTH/federation/identity-providers', function (req, res) {
    res.set('Content-Type', 'application/json');
    res.status(200).send(createIdpJsonWithValues({
        issuer: req.query.issuer
    }));
});

app.get('/v2.0/RAX-AUTH/federation/identity-providers/:idp_id/mapping', function (req, res) {
    res.set('Content-Type', 'application/json');
    res.status(200).send(createMappingJsonWithValues());
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
