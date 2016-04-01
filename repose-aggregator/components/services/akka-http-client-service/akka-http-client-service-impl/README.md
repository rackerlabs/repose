AKKA TODO

1. Research which AKKA version to use
2. Mailbox limits / size

TokenRetrievalActor
  1. does not support query parameters, will break for rackspace auth!
  2. what happens with unhandled messages???


  AuthTokenFutureActor
  1.Add token expiration hashMap
  2.HashMap least frequently used


repose-core manually added reference.conf specific to akka-actor version.
Find a way to automate making reference.conf file available in the valve classpath.

