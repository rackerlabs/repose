package com.rackspace.papi.components.keystone.v3.objects

// Note: The XSD asserts that either password or token must be chosen. To allow for either, I have defaulted both to None.
//       As a result, this case class could be constructed with one argument of type MethodsType which would create an
//       invalid AuthIdentityRequest. This is wrong. Don't construct this class with one argument.
case class AuthIdentityRequest(methods : List[String] = None, password : PasswordCredentials = None, token : TokenCredentials = None)
