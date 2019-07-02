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
import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.xml.transform.stream.StreamSource

import com.rackspace.com.papi.components.checker.handler._
import com.rackspace.com.papi.components.checker.wadl.WADLException
import com.rackspace.com.papi.components.checker.{Config, Validator, ValidatorException}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.lang3.StringUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.filters.simplerbac.config.SimpleRbacConfig
import org.springframework.beans.factory.annotation.Value

import scala.io.Source
import scala.util.Try

@Named
class SimpleRbacFilter @Inject()(configurationService: ConfigurationService,
                                 @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) configurationRoot: String)

  extends Filter
  with UpdateListener[SimpleRbacConfig]
  with StrictLogging {

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
    clearValidator()
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!initialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      logger.trace("Simple RBAC filter processing request...")
      validator.validate(
        servletRequest.asInstanceOf[HttpServletRequest],
        servletResponse.asInstanceOf[HttpServletResponse],
        filterChain)
    }
    logger.trace("Simple RBAC filter returning response...")
  }

  override def configurationUpdated(configurationObject: SimpleRbacConfig): Unit = {
    configuration = configurationObject
    config.enableRaxRolesExtension = true
    config.checkPlainParams = true
    config.setParamDefaults = true
    config.maskRaxRoles403 = configuration.isMaskRaxRoles403
    config.setResultHandler(getHandler)

    val rbacWadl = Option(configuration.getResources).flatMap { resources =>
        rbacToWadl(Option(resources.getValue).filter(_.trim.nonEmpty)).orElse(
          Option(resources.getHref).flatMap { fileName =>
              rbacToWadl(readResource(
                configurationService.getResourceResolver.resolve(fileName).newInputStream()
              ))
          }
        )
    }
    rbacWadl.foreach { wadl =>
        Option(configuration.getWadlOutput).foreach { wadlOutput =>
            val wadlPath: Path = Paths.get(StringUriUtilities.formatUri(getPath(wadlOutput, configurationRoot))).toAbsolutePath
            try {
              if (Files.exists(wadlPath) && Files.isWritable(wadlPath) || !Files.exists(wadlPath) && Files.createFile(wadlPath) != null) {
                Files.write(wadlPath, wadl.getBytes)
                logger.debug("Saved WADL to: " + wadlPath)
              } else {
                logger.warn("Cannot write to WADL file: " + wadlPath)
                logger.debug(s"Generated WADL:\n\n$wadl\n")
              }
            } catch {
              case ex: IOException =>
                logger.warn("Cannot write to WADL file: " + wadlPath, ex)
                logger.debug(s"Generated WADL:\n\n$wadl\n")
            }
           logger.debug(s"Generated WADL:\n\n$wadl\n")
        }
        initialized = reinitValidator(
          s"SimpleRbacValidator",
          new StreamSource(new ByteArrayInputStream(wadl.getBytes), "file://simple-rbac.wadl"),
          config
        )
    }
    if (rbacWadl.isEmpty) {
      logger.error("Unable to generate the WADL; check the provided resources.")
    }
    if (!isInitialized) {
      logger.error("Failed to initialize; check the provided resources.")
    }
  }

  override def isInitialized: Boolean = initialized

  private def getHandler: ResultHandler = {
    val dispatchResultHandler = new DispatchResultHandler
    Option(configuration.getDelegating) match {
      case Some(delegating) =>
        dispatchResultHandler.addHandler(new MethodLabelHandler)
        dispatchResultHandler.addHandler(new DelegationHandler(delegating.getQuality, delegating.getComponentName))
      case _ =>
        dispatchResultHandler.addHandler(new ServletResultHandler)
    }
    if (configuration.isEnableApiCoverage) {
      dispatchResultHandler.addHandler(new InstrumentedHandler)
      dispatchResultHandler.addHandler(new ApiCoverageHandler)
    }
    Option(configuration.getDotOutput).foreach { dotName =>
        if (StringUtils.isNotBlank(dotName)) {
          val dotPath: String = StringUriUtilities.formatUri(getPath(dotName, configurationRoot))
          val out: File = new File(dotPath)
          try {
            if (out.exists && out.canWrite || !out.exists && out.createNewFile) {
              dispatchResultHandler.addHandler(new SaveDotHandler(out, true, true))
            } else {
              logger.warn("Cannot write to DOT file: " + dotPath)
            }
          } catch {
            case ex: IOException =>
              logger.warn("Cannot write to DOT file: " + dotPath, ex)
          }
        }
    }
    dispatchResultHandler
  }

  private def getPath(path: String, configRoot: String): String = {
    val file: File = new File(path)
    if (file.isAbsolute) {
      file.getAbsolutePath
    } else {
      new File(configRoot, path).getAbsolutePath
    }
  }

  def readResource(resourceStream: InputStream): Option[String] = {
    Try(Some(Source.fromInputStream(resourceStream).getLines().mkString("\n"))).getOrElse(None)
  }

  private def rbacToWadl(rbac: Option[String]): Option[String] = {

    case class Resource(path: String, methods: Set[String], roles: Set[String])

    def parseLine(line: String): Option[Resource] = {
      val values = line.trim.split("\\s+")
      values.length match {
        case x if x >= 3 =>
          if (x > 3) {
            logger.info(s"Roles with spaces detected in: $line")
          }
          Some(new Resource(
            values(0),
            Try(values(1).split(',').toSet[String].map(_.trim)).getOrElse(Set.empty),
            Try(
              values.
                slice(2, values.length).
                mkString(" ").
                split(',').
                toSet[String].map(
                  _.trim.replaceAll(" ", "&#xA0;")
                )
            ).getOrElse(Set.empty)
          ))
        case 1 if values(0).length == 0 =>
          None
        case _ =>
          logger.warn(s"Malformed RBAC Resource: $line")
          None
      }
    }

    val parsed = rbac.flatMap { lines =>
        Some(lines.replaceAll("[\r?\n?]", "\n").split('\n').toList.flatMap(parseLine))
    }

    parsed.flatMap { values =>
        val header = s"""<application xmlns="http://wadl.dev.java.net/2009/02"
                        |             xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        |             xmlns:rax="http://docs.rackspace.com/api"
                        |>
                        |  <resources base="http://localhost">
                        |""".stripMargin
        val tenants = Option(configuration.getTenantsHeaderName) match {
          case Some(tenantsHeader) =>
            s""">
               |        <request>
               |          <param name="$tenantsHeader"
               |                 style="header"
               |                 required="true"
               |                 type="xsd:string"
               |                 repeating="true"
               |                 rax:isTenant="true"/>
               |        </request>
               |      </method>""".stripMargin
          case _ =>
            "/>"
        }
      val resources = values.map { value =>
          def toParams(resource: Resource) =
            """\{[^\}]*\}""".r.findAllIn(resource.path).map { param =>
              val name = param.substring(1, param.length-1)
              s"""      <param  name="$name" style="template"/>\n"""
            }.mkString
          def toMethods(resource: Resource, uuid: UUID) = {
            val roles = resource.roles.mkString(" ")
            val raxRoles = roles match {
              case anyAll if roles.equalsIgnoreCase("ANY") || roles.equalsIgnoreCase("ALL") => ""
              case _ => s"""rax:roles="$roles""""
            }
            resource.methods.map {
              _ match {
                case anyAll if anyAll.equalsIgnoreCase("ANY") || anyAll.equalsIgnoreCase("ALL") =>
                  s"""      <method name="GET"    id="_$uuid-GET"    $raxRoles$tenants
                     |      <method name="PUT"    id="_$uuid-PUT"    $raxRoles$tenants
                     |      <method name="POST"   id="_$uuid-POST"   $raxRoles$tenants
                     |      <method name="DELETE" id="_$uuid-DELETE" $raxRoles$tenants""".stripMargin
                case method =>
                  val methodToUpper = (method.toUpperCase+"\"").padTo(7,' ')
                  s"""      <method name="$methodToUpper id="_$uuid-$methodToUpper $raxRoles$tenants"""
              }
            }.mkString("\n")
          }
          val path = value.path
          val uuid = UUID.randomUUID
          val params = toParams(value)
          val methods = toMethods(value, uuid)
          s"""    <resource id="_$uuid" path="$path">
             |$params$methods
             |    </resource>""".stripMargin
        }.mkString("\n")
        val footer = s"""
                        |  </resources>
                        |</application>""".stripMargin

        Some(s"$header$resources$footer")
    }
  }

  private def initValidator(name : String, source : javax.xml.transform.Source, config : com.rackspace.com.papi.components.checker.Config): Boolean = {
    Option(validator) match {
      case Some(_) => true
      case _ =>
        try {
          logger.debug(s"Calling the validator creation method for $name. From thread {}", Thread.currentThread.getName)
          validator = Validator.apply(name + System.currentTimeMillis, source, config)
          true
        } catch {
          case e@(_: ValidatorException | _: WADLException) =>
            logger.warn("Error loading validator for WADL!!! ", e)
            false
        }
    }
  }

  private def clearValidator() {
    Option(validator) match {
      case Some(_) =>
        logger.debug (s"Destroying: $validator. From thread {}", Thread.currentThread.getName)
        validator.destroy
        validator = null
      case _ =>
    }
  }

  private def reinitValidator(name : String, source : javax.xml.transform.Source, config : com.rackspace.com.papi.components.checker.Config): Boolean = {
    clearValidator()
    initValidator(name, source, config)
  }
}
