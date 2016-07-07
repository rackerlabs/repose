# The following was used to create the truststore.jks
```
keytool -genkey -alias selfsigned -keyalg RSA -keysize 2048 -validity 36500 -keystore truststore.jks -storepass truststore-secret

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

Enter key password for <selfsigned>
        (RETURN if same as keystore password):
```
