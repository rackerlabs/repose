/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.openstackidentityv3.utilities

object OpenStackIdentityV3Headers {
  final val X_AUTH_TOKEN = "X-Auth-Token"
  final val X_AUTHORIZATION = "X-Authorization"
  final val X_DEFAULT_REGION = "X-Default-Region"
  final val X_IDENTITY_STATUS = "X-Identity-Status"
  final val X_PROJECT_NAME = "X-Project-Name"
  final val X_PROJECT_ID = "X-Project-ID"
  final val X_USER_NAME = "X-User-Name"
  final val X_USER_ID = "X-User-ID"
  final val X_ROLES = "X-Roles"
  final val X_TOKEN_EXPIRES = "X-Token-Expires"
  final val X_SUBJECT_TOKEN = "X-Subject-Token"
  final val X_IMPERSONATOR_NAME = "X-Impersonator-Name"
  final val X_IMPERSONATOR_ID = "X-Impersonator-Id"
  final val WWW_AUTHENTICATE = "WWW-Authenticate"

  final val X_AUTH_PROXY = "Proxy"
  final val X_DELEGATED = "Delegated"
}
