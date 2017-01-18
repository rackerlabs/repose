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
package org.openrepose.filters.samlpolicy

import javax.inject.{Inject, Named}

import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}

import scala.util.Try

/**
  * Handles all of the API interactions necessary for the [[SamlPolicyTranslationFilter]].
  *
  * The default singleton scope is used, and works, due entirely to the separation of
  * Spring contexts between filters.
  */
@Named
class SamlPolicyProvider @Inject()(akkaServiceClientFactory: AkkaServiceClientFactory) {

  @volatile
  private var tokenServiceClient: ServiceClient = _

  @volatile
  private var policyServiceClient: ServiceClient = _

  /**
    * Obtains service clients for the provided connection pool IDs.
    * These service clients will be used when making calls to external APIs.
    */
  def using(tokenPoolId: Option[String], policyPoolId: Option[String]): Unit = {
    val optTokenServiceClient = Option(tokenServiceClient)
    if (optTokenServiceClient.isEmpty || tokenServiceClient.poolId != tokenPoolId) {
      optTokenServiceClient.foreach(_.akkaServiceClient.destroy())
      tokenServiceClient = ServiceClient(
        tokenPoolId,
        tokenPoolId.map(akkaServiceClientFactory.newAkkaServiceClient)
          .getOrElse(akkaServiceClientFactory.newAkkaServiceClient)
      )
    }

    val optPolicyServiceClient = Option(policyServiceClient)
    if (optPolicyServiceClient.isEmpty || policyServiceClient.poolId != policyPoolId) {
      optPolicyServiceClient.foreach(_.akkaServiceClient.destroy())
      policyServiceClient = ServiceClient(
        policyPoolId,
        policyPoolId.map(akkaServiceClientFactory.newAkkaServiceClient)
          .getOrElse(akkaServiceClientFactory.newAkkaServiceClient)
      )
    }
  }

  /**
    * Retrieves a token from Identity that will be used for authorization on all other calls to Identity.
    *
    * @return the token if successful, or a failure if unsuccessful
    */
  def getToken: Try[String] = {
    ???
  }

  /**
    * Retrieves a policy from Identity that will be used during SAMLResponse translation.
    *
    * @return the policy if successful, or a failure if unsuccessful
    */
  def getPolicy: Try[String] = {
    ???
    // TODO: Handle encoding when creating a String
  }

  private case class ServiceClient(poolId: Option[String], akkaServiceClient: AkkaServiceClient)

}
