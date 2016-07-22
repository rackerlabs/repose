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

These instructions were based on the documentation locate at 

 - http://www.eclipse.org/jetty/documentation/current/configuring-ssl.html#generating-key-pairs-and-certificates
 
And the tutorial located at:

 - http://commandlinefanatic.com/cgi-bin/showarticle.cgi?article=art042
