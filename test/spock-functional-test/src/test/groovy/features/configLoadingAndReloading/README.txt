
Stories
-------
  - B-39898 "Behavior loading and reloading of configs"
  - B-43371 "Allow Missing RMS Config"
  - D-14108 "Api Validator Filter Returns 200s After Loading a Bad Config"

Specification
-------------
This story formalizes the expected behavior of Repose in response to invalid
configuration files. In short, the following behaviors are required:

  1. When Repose is started with one or more invalid configs, then it should
     return 503's for all incoming requests.
  2. After Repose is started with invalid configs, if valid configs are loaded
     at run-time, then Repose should return whatever response is appropriate
     given the new configs.
  3. If Repose is already running with good configs, and the files are replaced
     with bad/invalid configs, then Repose should log an error and refuse to
     load the new configs, instead behaving as though nothing has changed.
  4. This behavior must be tested for each config file that Repose uses.
  5. Because the container config and the system model config are both required
     for Repose to be able to listen on a port and return 503's, bad versions
     of them won't be able to return anything. Therefore, if either of those
     config files is bad, it shouldn't be possible to make an HTTP connection
     in the first place.

Additionally, for the RMS config file:

  1. When there is a missing configuration that is not required by default or
     by the filter Repose should start without missing configuration file error
     and should pick it up and work if the missing configuration that is good
     is added later and throw validation error if there added missing
     configuration has any mal-formed XML.

Additionally, for the API Validation filter:

  1. When the <validators> element has a @version attribute with the value "2",
     and a <validator> element has a @use-saxon attribute with any value, then
     Repose should return 503's.

For the purposes of this test, "bad" or "invalid" refers to mal-formed XML.
Future tests will need to be created to check configs against the XSD schemas
defined for them. The "good" versions of the configs will all include the
Repose mock server as the only available destination in the system model,
ensuring a 200 response code for valid states.

The configuration files to be tested are:

  - system-model.cfg.xml
  - container.cfg.xml
  - response-messaging.cfg.xml
  - rate-limiting.cfg.xml
  - versioning.cfg.xml
  - translation.cfg.xml
  - client-auth-n.cfg.xml
  - openstack-authorization.cfg.xml
  - dist-datastore.cfg.xml
  - http-logging.cfg.xml
  - uri-identity.cfg.xml
  - header-identity.cfg.xml
  - ip-identity.cfg.xml
  - validator.cfg.xml

For each of these, there will be one good version and one bad version. The bad
versions will be copies of the good versions with missing closing tags.
Additionally, for each filter config, a system model file will be created
specifying that filter, so that it is included when Repose runs. "Missing"
configs means that the configuration file does not exist in the Repose config
directory.

Procedure
---------
The tests will proceed as follows. For each config to be tested:

  1. Repose will be started with the "bad" version of the configs. An HTTP
     requests will be sent to Repose, and a 503 error code will be expected
     (except for the system model or container configs, in which case no
     connection will allowed).
  2. Next, the configs will be changed to the "good" version. An HTTP requests
     will be sent to Repose, and a 200 success code will be expected.
  3. Next, the configs will be changed back to the "bad" version. An HTTP
     request will be sent to Repose, and a 200 success code will be expected.
  4. Repose will be shut down
  5. Repose will be started with the "good" version of the configs. An HTTP
     request will be sent to Repose, and a 200 success code will be expected.

In addition to the RMS config alone:

  1. Repose will be started with the "missing" config that is not required. An
     HTTP request will be sent to Repose, and a 200 success code will be
     expected.


Test cases for B-29898
    +---------------------------------+----------------+-----------------+
    | Component                       | Transition     | Expected Result |
    +=================================+================+=================+
    | system-model.cfg.xml            | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | system-model.cfg.xml            | Start Bad      | Can't connect   |
    +---------------------------------+----------------+-----------------+
    | system-model.cfg.xml            | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | system-model.cfg.xml            | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | container.cfg.xml               | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | container.cfg.xml               | Start Bad      | Can't connect   |
    +---------------------------------+----------------+-----------------+
    | container.cfg.xml               | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | container.cfg.xml               | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | response-messaging.cfg.xml      | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | response-messaging.cfg.xml      | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | response-messaging.cfg.xml      | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | response-messaging.cfg.xml      | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | rate-limiting.cfg.xml           | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | rate-limiting.cfg.xml           | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | rate-limiting.cfg.xml           | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | rate-limiting.cfg.xml           | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | versioning.cfg.xml              | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | versioning.cfg.xml              | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | versioning.cfg.xml              | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | versioning.cfg.xml              | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | translation.cfg.xml             | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | translation.cfg.xml             | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | translation.cfg.xml             | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | translation.cfg.xml             | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | client-auth-n.cfg.xml           | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | client-auth-n.cfg.xml           | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | client-auth-n.cfg.xml           | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | client-auth-n.cfg.xml           | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | openstack-authorization.cfg.xml | Start Good     | 401             |
    +---------------------------------+----------------+-----------------+
    | openstack-authorization.cfg.xml | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | openstack-authorization.cfg.xml | Good to Bad    | 401             |
    +---------------------------------+----------------+-----------------+
    | openstack-authorization.cfg.xml | Bad to Good    | 401             |
    +---------------------------------+----------------+-----------------+
    | dist-datastore.cfg.xml          | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | dist-datastore.cfg.xml          | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | dist-datastore.cfg.xml          | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | dist-datastore.cfg.xml          | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | http-logging.cfg.xml            | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | http-logging.cfg.xml            | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | http-logging.cfg.xml            | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | http-logging.cfg.xml            | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | uri-identity.cfg.xml            | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | uri-identity.cfg.xml            | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | uri-identity.cfg.xml            | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | uri-identity.cfg.xml            | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | header-identity.cfg.xml         | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | header-identity.cfg.xml         | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | header-identity.cfg.xml         | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | header-identity.cfg.xml         | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+
    | ip-identity.cfg.xml             | Start Good     | 200             |
    +---------------------------------+----------------+-----------------+
    | ip-identity.cfg.xml             | Start Bad      | 503             |
    +---------------------------------+----------------+-----------------+
    | ip-identity.cfg.xml             | Good to Bad    | 200             |
    +---------------------------------+----------------+-----------------+
    | ip-identity.cfg.xml             | Bad to Good    | 200             |
    +---------------------------------+----------------+-----------------+


Test cases for B-43371
    +---------------------------------+----------------+-----------------+
    | Component                       | Transition     | Expected Result |
    +=================================+================+=================+
    | response-messaging.cfg.xml      | Start Missing  | 200             |
    +---------------------------------+----------------+-----------------+


Test cases for D-14108
    +------------------------+--------------------+-----------------+
    | Component              | Transition         | Expected Result |
    +========================+====================+=================+
    | validator.cfg.xml      | Start Good         | 200             |
    +------------------------+--------------------+-----------------+
    | validator.cfg.xml      | Start Bad          | 503             |
    +------------------------+--------------------+-----------------+
    | validator.cfg.xml      | Good to Bad        | 200             |
    +------------------------+--------------------+-----------------+
    | validator.cfg.xml      | Bad to Good        | 200             |
    +------------------------+--------------------+-----------------+
    | validator.cfg.xml      | Start v2-use-saxon | 503             |
    +------------------------+--------------------+-----------------+


Items not tested
----------------
As mentioned before, this test will not test whether or or how Repose handles
config files that fail XSD validation.

These tests do not cover the following components' configuration files:

  - Content Normalization filter
  - Destination Router
  - Header Identity Mapping filter
  - Header Normalization
  - Rackspace Auth 1.1 Content Identity filter
  - Root Context Router
  - Service Authentication filter
  - URI Normalization filter


