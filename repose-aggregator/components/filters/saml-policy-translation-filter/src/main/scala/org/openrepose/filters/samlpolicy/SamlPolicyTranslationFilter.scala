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
import javax.servlet.{FilterChain, ServletRequest, ServletResponse}

import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.samlpolicy.config.SamlPolicyConfig

/**
  * Created by adrian on 12/12/16.
  */
@Named
class SamlPolicyTranslationFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[SamlPolicyConfig](configurationService){

  override val DEFAULT_CONFIG: String = "saml-policy.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/config/schema/saml-policy.xsd"

  override def doWork(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = chain.doFilter(request, response)
}
