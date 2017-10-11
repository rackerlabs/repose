# The following was used to create the server.jks
```
keytool -genkeypair -keystore server.jks -storepass password -alias server -keypass password -keyalg RSA -keysize 2048 -validity 36500 -sigalg SHA256withRSA 

What is your first and last name?
  [Unknown]:  localhost
What is the name of your organizational unit?
  [Unknown]:  Repose
What is the name of your organization?
  [Unknown]:  Rackspace
What is the name of your City or Locality?
  [Unknown]:  San Antonio
What is the name of your State or Province?
  [Unknown]:  TX
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=localhost, OU=Repose, O=Rackspace, L=San Antonio, ST=TX, C=US correct?
  [no]:  yes
```

# The following was used to create the client.jks
```
keytool -genkeypair -keystore client.jks -storepass password -alias client -keypass password -keyalg RSA -keysize 2048 -validity 36500 -sigalg SHA256withRSA

What is your first and last name?
  [Unknown]:  Stabby the Narwhal
What is the name of your organizational unit?
  [Unknown]:  Repose
What is the name of your organization?
  [Unknown]:  Rackspace
What is the name of your City or Locality?
  [Unknown]:  San Antonio
What is the name of your State or Province?
  [Unknown]:  TX
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=Stabby the Narwhal, OU=Repose, O=Rackspace, L=San Antonio, ST=TX, C=US correct?
  [no]:  yes
```

# The following was used to create the single.jks
```
keytool -importkeystore -srckeystore server.jks -destkeystore single.jks -srcalias server -srcstorepass password -deststorepass password
keytool -importkeystore -srckeystore client.jks -destkeystore single.jks -srcalias client -srcstorepass password -deststorepass password
```

# The following was used to create the server.pem for use with CURL
```
keytool -exportcert -rfc -keystore server.jks -storepass password -alias server > server.pem
```

# The following was used to create the client.pfx and client.p12 for use with CURL
```
keytool -importkeystore -srckeystore client.jks -destkeystore client.pfx -deststoretype PKCS12 -srcalias client -deststorepass password
openssl pkcs12 -in client.pfx -out client.p12 -nodes
```

# The server.jks was copied for other testing:
```
cp server.jks ../../../../../../artifacts/valve/src/test/resources/valveTesting/sslTesting/
```
AKA
```
cp server.jks <REPOSE>/repose-aggregator/artifacts/valve/src/test/resources/valveTesting/sslTesting/
```

# These are the CURL results against a Repose server as configured for the `SSLClientAuthenticationTest` with each of the given conditions:

## If both the server certs and client certs are provided:
$ curl --verbose --cacert server.pem --cert client.p12 https://localhost:10009/
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 10009 (#0)
* found 1 certificates in server.pem
* ALPN, offering http/1.1
* SSL connection using TLS1.2 / ECDHE_RSA_AES_128_GCM_SHA256
*        server certificate verification OK
*        server certificate status verification SKIPPED
*        common name: localhost (matched)
*        server certificate expiration date OK
*        server certificate activation date OK
*        certificate public key: RSA
*        certificate version: #3
*        subject: C=US,ST=TX,L=San Antonio,O=Rackspace,OU=Repose,CN=localhost
*        start date: Fri, 22 Jul 2016 17:32:49 GMT
*        expire date: Sun, 28 Jun 2116 17:32:49 GMT
*        issuer: C=US,ST=TX,L=San Antonio,O=Rackspace,OU=Repose,CN=localhost
*        compression: NULL
* ALPN, server did not agree to a protocol
> GET / HTTP/1.1
> Host: localhost:10009
> User-Agent: curl/7.47.0
> Accept: */*
> 
< HTTP/1.1 200 OK
< Date: Fri, 22 Jul 2016 20:39:47 GMT
< Date: Fri, 22 Jul 2016 20:39:47 GMT
< Via: 1.1 Repose (Repose/8.0.2.0-SNAPSHOT)
< x-trans-id: eyJyZXF1ZXN0SWQiOiI4ZmVkODg5OS00MjYyLTQwYWUtOGQwNC0xYmJjY2E1OTY3NjEiLCJvcmlnaW4iOm51bGx9
< Content-Length: 0
< Server: Jetty(9.2.z-SNAPSHOT)
< 
* Connection #0 to host localhost left intact

## If only the client certs are provided:
$ curl --verbose --cert client.p12 https://localhost:10009/
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 10009 (#0)
* ALPN, offering http/1.1
* SSL connection using TLS1.2 / ECDHE_RSA_AES_128_GCM_SHA256
* server certificate verification failed. CAfile: /etc/ssl/certs/ca-certificates.crt CRLfile: none
* Closing connection 0
curl: (60) server certificate verification failed. CAfile: /etc/ssl/certs/ca-certificates.crt CRLfile: none
More details here: http://curl.haxx.se/docs/sslcerts.html

curl performs SSL certificate verification by default, using a "bundle"
 of Certificate Authority (CA) public keys (CA certs). If the default
 bundle file isn't adequate, you can specify an alternate file
 using the --cacert option.
If this HTTPS server uses a certificate signed by a CA represented in
 the bundle, the certificate verification probably failed due to a
 problem with the certificate (it might be expired, or the name might
 not match the domain name in the URL).
If you'd like to turn off curl's verification of the certificate, use
 the -k (or --insecure) option.

## If only the server certs are provided:
$ curl --verbose --cacert server.pem https://localhost:10009/
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 10009 (#0)
* found 1 certificates in server.pem
* ALPN, offering http/1.1
* gnutls_handshake() failed: The TLS connection was non-properly terminated.
* Closing connection 0
curl: (35) gnutls_handshake() failed: The TLS connection was non-properly terminated.

## If no certs are provided:
$ curl --verbose https://localhost:10009/
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 10009 (#0)
* ALPN, offering http/1.1
* gnutls_handshake() failed: The TLS connection was non-properly terminated.
* Closing connection 0
curl: (35) gnutls_handshake() failed: The TLS connection was non-properly terminated.

These instructions were based on the documentation locate at 

 - http://www.eclipse.org/jetty/documentation/current/configuring-ssl.html#generating-key-pairs-and-certificates
 
And the tutorial located at:

 - http://commandlinefanatic.com/cgi-bin/showarticle.cgi?article=art042
