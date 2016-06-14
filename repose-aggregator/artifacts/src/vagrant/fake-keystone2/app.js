var express = require('express');
var libxmljs = require('libxmljs');
require('date-utils');

var app = express();
app.disable('etag');

app.post('/v2.0/tokens', function(req, res){
  var body = req.body;
  res.set('Content-Type','application/xml');
  res.send(200, '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><access xmlns="http://docs.openstack.org/identity/api/v2.0" xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0" xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0" xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0" xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"><token id="this-is-the-admin-token" expires="2013-08-03T19:46:54Z"><tenant id="this-is-the-admin-tenant" name="this-is-the-admin-tenant"/></token><user xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" id="67890" name="admin_username" rax-auth:defaultRegion="the-default-region"><roles><role id="684" name="compute:default" description="A Role that allows a user access to keystone Service methods" serviceId="0000000000000000000000000000000000000001" tenantId="12345"/><role id="5" name="object-store:default" description="A Role that allows a user access to keystone Service methods" serviceId="0000000000000000000000000000000000000002" tenantId="12345"/><role id="6" name="cloudfeeds:service-admin" description="A Role that allows a user access to keystone Service methods" serviceId="0000000000000000000000000000000000000005" tenantId="12345"/></roles></user><serviceCatalog><service type="rax:object-cdn" name="cloudFilesCDN"><endpoint region="DFW" tenantId="this-is-the-admin-tenant" publicURL="https://cdn.stg.clouddrive.com/v1/this-is-the-admin-tenant"/><endpoint region="ORD" tenantId="this-is-the-admin-tenant" publicURL="https://cdn.stg.clouddrive.com/v1/this-is-the-admin-tenant"/></service><service type="object-store" name="cloudFiles"><endpoint region="ORD" tenantId="this-is-the-admin-tenant" publicURL="https://storage.stg.swift.racklabs.com/v1/this-is-the-admin-tenant" internalURL="https://snet-storage.stg.swift.racklabs.com/v1/this-is-the-admin-tenant"/><endpoint region="DFW" tenantId="this-is-the-admin-tenant" publicURL="https://storage.stg.swift.racklabs.com/v1/this-is-the-admin-tenant" internalURL="https://snet-storage.stg.swift.racklabs.com/v1/this-is-the-admin-tenant"/></service></serviceCatalog></access>');
});

app.get('/v2.0/tokens/:token_id', function(req,res){
  var token = req.params.token_id;
  var tempDate = new Date();

  var date = tempDate.addDays(1).toFormat('YYYY-MM-DDTHH24:MI:SSZ');
  var user_id = token.substr(0, 10);
  console.log('tenantId=%s', user_id);
  res.set('Content-Type','application/xml');
  res.send(200,'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><access xmlns="http://docs.openstack.org/identity/api/v2.0" xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0" xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0" xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0" xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"><token id="' + token + '" expires="' + date + '"><tenant id="' + user_id + '" name="' + user_id + '"/></token><user xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" id="' + user_id + '" name="username" rax-auth:defaultRegion="the-default-region"><roles><role id="684" name="compute:default" description="A Role that allows a user access to keystone Service methods" serviceId="0000000000000000000000000000000000000001" tenantId="' + user_id + '"/><role id="685" name="observer" description="A Role that allows a user access to keystone Service methods" serviceId="0000000000000000000000000000000000000001" tenantId="' + user_id + '"/><role id="6" name="cloudfeeds:service-admin" description="A Role that allows a user access to keystone Service methods" serviceId="0000000000000000000000000000000000000005" tenantId="12345"/><role id="5" name="object-store:default" description="A Role that allows a user access to keystone Service methods" serviceId="0000000000000000000000000000000000000002" tenantId="' + user_id + '"/></roles></user><serviceCatalog><service type="rax:object-cdn" name="cloudFilesCDN"><endpoint region="DFW" tenantId="this-is-the-tenant" publicURL="https://cdn.stg.clouddrive.com/v1/this-is-the-tenant"/><endpoint region="ORD" tenantId="this-is-the-tenant" publicURL="https://cdn.stg.clouddrive.com/v1/this-is-the-tenant"/></service><service type="object-store" name="cloudFiles"><endpoint region="ORD" tenantId="this-is-the-tenant" publicURL="https://storage.stg.swift.racklabs.com/v1/this-is-the-tenant" internalURL="https://snet-storage.stg.swift.racklabs.com/v1/this-is-the-tenant"/><endpoint region="DFW" tenantId="this-is-the-tenant" publicURL="https://storage.stg.swift.racklabs.com/v1/this-is-the-tenant" internalURL="https://snet-storage.stg.swift.racklabs.com/v1/this-is-the-tenant"/></service></serviceCatalog></access>');
});

app.get('/v2.0/users/:user_id/RAX-KSGRP', function(req,res){
  res.set('Content-Type','application/xml');
  var user = req.params.user_id;
  res.send(200,'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><groups xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0"><group id="0" name="Default"><description>Default Limits</description></group></groups>');
});

app.get('/', function(req, res){
  res.send('hello world');
});

var svr = app.listen(9090, function () {
  var host = svr.address().address;
  var port = svr.address().port;

  console.log('Listening at http://%s:%s', host, port);
});
