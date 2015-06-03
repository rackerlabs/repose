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
package org.openrepose.filters.simplerbac

import java.io.{ByteArrayInputStream, File, IOException, InputStream}
import java.net.URL
import java.util
import java.util.UUID
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.xml.transform.stream.StreamSource

import com.rackspace.com.papi.components.checker.handler._
import com.rackspace.com.papi.components.checker.{Config, Validator}
import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.lang3.StringUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.simplerbac.config.SimpleRbacConfig

import scala.io.Source
import scala.util.Try

@Named
class SimpleRbacFilter @Inject()(configurationService: ConfigurationService)
  extends Filter
  with UpdateListener[SimpleRbacConfig]
  with HttpDelegationManager
  with LazyLogging {

  private final val DEFAULT_CONFIG = "simple-rbac.cfg.xml"

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: SimpleRbacConfig = _
  var initialized = false
  var validator: Validator = _
  val config = new Config

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configurationFile)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/simple-rbac.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      this,
      classOf[SimpleRbacConfig]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!initialized) {
      logger.error("Simple RBAC filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(500)
    } else {
      val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
      val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

      logger.trace("Simple RBAC filter processing request...")
      validator.validate(mutableHttpRequest,mutableHttpResponse,filterChain)
    }
    logger.trace("Simple RBAC filter returning response...")
  }

  override def configurationUpdated(configurationObject: SimpleRbacConfig): Unit = {
    configuration = configurationObject
    val isDelegating = configuration.getDelegating != null
    val delegationQuality = if (isDelegating) configuration.getDelegating.getQuality else 0.0
    config.enableRaxRolesExtension = true
    config.checkPlainParams = true
    config.maskRaxRoles403 = configuration.isEnableMasking403S
    config.setResultHandler(getHandlers(
      isDelegating,
      delegationQuality,
      true,
      "/tmp",
      "simple-rbac.dot"
    ))
    val rbacWadl = rbacToWadl(configuration.getResources).getOrElse(
      if (configuration.getResourcesFileName != null) {
        rbacToWadl(
          readResource(
            configurationService.getResourceResolver.resolve(configuration.getResourcesFileName).newInputStream()
          ).getOrElse("")
        ).getOrElse("")
      } else {
        ""
      }
    )
    val uuid = UUID.randomUUID
    validator = Validator.apply(
      s"SimpleRbacValidator_$uuid",
      new StreamSource(new ByteArrayInputStream(rbacWadl.getBytes), "file://simple-rbac.wadl"),
      config
    )
    initialized = true
  }

  override def isInitialized: Boolean = initialized

  private def getHandlers(isDelegating: Boolean,
                          delegationQuality: Double,
                          isEnableApiCoverage: Boolean,
                          configRoot: String,
                          dotOutput: String): DispatchHandler = {
    val handlers: util.List[ResultHandler] = new util.ArrayList[ResultHandler]
    if (isDelegating) {
      handlers.add(new MethodLabelHandler)
      handlers.add(new DelegationHandler(delegationQuality))
    } else {
      handlers.add(new ServletResultHandler)
    }
    if (isEnableApiCoverage) {
      handlers.add(new InstrumentedHandler)
      handlers.add(new ApiCoverageHandler)
    }
    if (StringUtils.isNotBlank(dotOutput)) {
      val dotPath: String = StringUriUtilities.formatUri(getPath(dotOutput, configRoot))
      val out: File = new File(dotPath)
      try {
        if (out.exists && out.canWrite || !out.exists && out.createNewFile) {
          handlers.add(new SaveDotHandler(out, isEnableApiCoverage, true))
        } else {
          logger.warn("Cannot write to DOT file: " + dotPath)
        }
      } catch {
        case ex: IOException => {
          logger.warn("Cannot write to DOT file: " + dotPath, ex)
        }
      }
    }
    new DispatchHandler(handlers.toArray(new Array[ResultHandler](0)))
  }

  private def getPath(path: String, configRoot: String): String = {
    val file: File = new File(path)
    if (file.isAbsolute) {
      file.getAbsolutePath
    } else {
      new File(configRoot, path).getAbsolutePath
    }
  }

  private def readResource(resourceStream: InputStream): Option[String] = {
    Try(Some(Source.fromInputStream(resourceStream).getLines().mkString("\n"))).getOrElse(None)
  }

  private def rbacToWadl(rbac: String): Option[String] = {
    val targetPort = 8080
//    val wadl = s"""<application xmlns:rax="http://docs.rackspace.com/api"
//                  |          xmlns:xsd="http://www.w3.org/2001/XMLSchema"
//                  |          xmlns="http://wadl.dev.java.net/2009/02"
//                  |     >
//                  | <resources base="http://localhost">
//                  |     <resource id="first" path="path">
//                  |         <resource id="second" path="to">
//                  |             <resource id="this" path="this">
//                  |                 <method name="GET"      id="getMethodThis"      rax:roles="role1 role2 role3 role4"/>
//                  |                 <method name="PUT"      id="putMethodThis"      rax:roles="role1 role2 role3"/>
//                  |                 <method name="POST"     id="postMethodThis"     rax:roles="role1 role2"/>
//                  |                 <method name="DELETE"   id="deleteMethodThis"   rax:roles="role1"/>
//                  |             </resource>
//                  |             <resource id="that" path="that">
//                  |                 <method name="GET"      id="getMethodThat"/>
//                  |                 <method name="PUT"      id="putMethodThat"/>
//                  |                 <method name="POST"     id="postMethodThat"     rax:roles="role1"/>
//                  |                 <method name="DELETE"   id="deleteMethodThat"   rax:roles="role1"/>
//                  |             </resource>
//                  |         </resource>
//                  |     </resource>
//                  | </resources>
//                  |</application>
//                  | """.stripMargin.trim()
    val wadl = s"""<application xmlns:rax="http://docs.rackspace.com/api"
                  |          xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  |          xmlns="http://wadl.dev.java.net/2009/02"
                  |     >
                  | <resources base="http://localhost">
                  |     <resource id="first" path="path">
                  |         <resource id="second" path="to">
                  |             <resource id="this" path="this">
                  |                 <method name="GET"      id="getMethodThis"      rax:roles="super useradmin admin user"/>
                  |                 <method name="PUT"      id="putMethodThis"      rax:roles="super useradmin admin"/>
                  |                 <method name="POST"     id="postMethodThis"     rax:roles="super useradmin"/>
                  |                 <method name="DELETE"   id="deleteMethodThis"   rax:roles="super"/>
                  |             </resource>
                  |             <resource id="that" path="that">
                  |                 <method name="GET"      id="getMethodThat"/>
                  |                 <method name="PUT"      id="putMethodThat"/>
                  |                 <method name="POST"     id="postMethodThat"     rax:roles="super"/>
                  |                 <method name="DELETE"   id="deleteMethodThat"   rax:roles="super"/>
                  |             </resource>
                  |             <resource id="test" path="test">
                  |                 <method name="GET"      id="getMethodTest"      rax:roles="useradmin user"/>
                  |                 <method name="POST"     id="putMethodTest"      rax:roles="useradmin user"/>
                  |             </resource>
                  |         </resource>
                  |     </resource>
                  | </resources>
                  |</application>
                  | """.stripMargin.trim()
    Some(wadl)
  }
}
