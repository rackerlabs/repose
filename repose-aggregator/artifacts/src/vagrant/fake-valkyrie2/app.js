var sleep = require('sleep');
var express = require('express');
var app = express();

function generateDeviceID() {
    return Math.floor(Math.random() * 900000) + 100000;
}

app.use(function (req, res, next) {
    var data = '';
    req.setEncoding('utf8');
    req.on('data', function (chunk) {
        data += chunk;
    });

    req.on('end', function () {
        req.body = data;
        next();
    });
});
app.disable('etag');

app.get('/account/:tenant/inventory', function (req, res) {
    var tenant = req.params.tenant;
    console.log('JSON: tenantId=%s', tenant);
    var deviceID = generateDeviceID();
    var deviceID2 = generateDeviceID();
    var deviceID3 = generateDeviceID();
    res.set('content-type', 'application/json');
    res.status(200).json({
        "inventory": [
            {
                "status": "Online",
                "datacenter": "Datacenter (ABC1)",
                "name": deviceID + "-hyp1.abc.rvi.local",
                "ipv6_network": "",
                "type": "Server",
                "primary_ipv4": "",
                "primary_ipv6": "",
                "primary_ipv4_gateway": "",
                "datacenter_id": 1,
                "platform": "Super Server",
                "nickname": null,
                "os": "Penguin Power",
                "account_number": req.params.tenant,
                "primary_ipv4_netmask": "",
                "id": deviceID,
                "ipv6_server_allocation_block": "",
                "permissions": [
                    "racker"
                ]
            },
            {
                "status": "Online",
                "datacenter": "Datacenter (ABC1)",
                "name": deviceID2 + "-hyp1.abc.rvi.local",
                "ipv6_network": "",
                "type": "Server",
                "primary_ipv4": "",
                "primary_ipv6": "",
                "primary_ipv4_gateway": "",
                "datacenter_id": 1,
                "platform": "Super Server",
                "nickname": null,
                "os": "Penguin Power",
                "account_number": req.params.tenant,
                "primary_ipv4_netmask": "",
                "id": deviceID2,
                "ipv6_server_allocation_block": "",
                "permissions": [
                    "racker"
                ]
            },
            {
                "status": "Online",
                "datacenter": "Datacenter (ABC1)",
                "name": deviceID3 + "-hyp1.abc.rvi.local",
                "ipv6_network": "",
                "type": "Server",
                "primary_ipv4": "",
                "primary_ipv6": "",
                "primary_ipv4_gateway": "",
                "datacenter_id": 1,
                "platform": "Super Server",
                "nickname": null,
                "os": "Penguin Power",
                "account_number": req.params.tenant,
                "primary_ipv4_netmask": "",
                "id": deviceID3,
                "ipv6_server_allocation_block": "",
                "permissions": [
                    "hyoog_json"
                ]
            }
        ]
    });

});

app.get('/account/:tenant/permissions/contacts/:devices/by_contact/:contact/effective', function (req, res) {
    var tenant = req.params.tenant;
    var devices = req.params.devices;
    var contact = req.params.contact;
    console.log('JSON: tenantId=%s', tenant);
    console.log('JSON: devices=%s', devices);
    console.log('JSON: tenantId=%s', contact);
    var deviceID = generateDeviceID();
    var deviceID2 = generateDeviceID();
    var deviceID3 = generateDeviceID();
    res.set('content-type', 'application/json');
    res.status(200).json({
        "contact_permissions": [
            {
                "item_type_id": 2,
                "permission_type_id": 15,
                "item_type_name": "devices",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_name": "butts",
                "item_id": deviceID,
                "id": 0
            },
            {
                "item_type_id": 2,
                "permission_type_id": 12,
                "item_type_name": "devices",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_name": "butts",
                "item_id": deviceID2,
                "id": 0
            },
            {
                "item_type_id": 2,
                "permission_type_id": 9,
                "item_type_name": "devices",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_name": "admin_product",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "permission_type_id": 2,
                "item_type_name": "devices",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_name": "edit_product",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "permission_type_id": 9,
                "item_type_name": "accounts",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_name": "upgrade_account",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "permission_type_id": 2,
                "item_type_name": "accounts",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_name": "edit_ticket",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "permission_type_id": 6,
                "item_type_name": "accounts",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_name": "view_domain",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "item_type_name": "accounts",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_type_id": 17,
                "permission_name": "view_reports",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "item_type_name": "accounts",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_type_id": 10,
                "permission_name": "manage_users",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "item_type_name": "accounts",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_type_id": 7,
                "permission_name": "edit_domain",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "item_type_name": "accounts",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_type_id": 15,
                "permission_name": "also_butts",
                "item_id": 862323,
                "id": 0
            },
            {
                "item_type_id": 2,
                "item_type_name": "devices",
                "contact_id": req.params.contact,
                "account_number": req.params.tenant,
                "permission_type_id": 7,
                "permission_name": "hyoog_json",
                "item_id": deviceID3,
                "id": 0
            }
        ]
    });
});

app.listen(6060);
