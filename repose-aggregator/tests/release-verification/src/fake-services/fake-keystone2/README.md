# Fake Keystone v2
This is a fake Keystone v2 service for testing purposes.

It was written using NodeJS leveraging the `express` module.
The module for this service is managed using NPM.

## Supported Calls
The following calls are supported, to some extent, by this service:

* **GET /** -
A test endpoint that returns a Hello World response.
* **POST /v2.0/tokens** -
The token creation endpoint.
Returns token data.
* **GET /v2.0/tokens/:token_id** -
The token validation endpoint.
Returns token data.
* **GET /v2.0/users/:user_id/RAX-KSGRP** -
The user groups endpoint.
Returns sample groups data of the groups belonging to a user.
* **GET /v2.0/RAX-AUTH/federation/identity-providers** -
The Issuer to IDP ID mapping endpoint.
Returns Identity providers data.
* **GET /v2.0/RAX-AUTH/federation/identity-providers/:idp_id/mapping** -
The SAML mapping policy endpoint.
Returns mapping policy data.
* **POST /v2.0/RAX-AUTH/federation/saml/auth** -
The SAML token creation endpoint.
Returns token data.

## Running this Service
To run this service, follow these steps:
1. Make sure that [NodeJS](https://nodejs.org/en/) and [NPM](https://www.npmjs.com/) are installed.
1. Navigate to the directory containing the `app.js` file.
1. Execute `npm install` to install all dependencies.
1. Run the service using `node app.js`.