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
import java.util
import java.util.UUID
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.xml.transform.stream.StreamSource

import com.rackspace.com.papi.components.checker.handler._
import com.rackspace.com.papi.components.checker.{Config, Validator}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.lang3.StringUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.filters.simplerbac.config.SimpleRbacConfig
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConversions._
import scala.io.Source
import scala.util.Try

@Named
class SimpleRbacFilter @Inject()(configurationService: ConfigurationService,
                                 @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) configurationRoot: String)

  extends Filter
  with UpdateListener[SimpleRbacConfig]
  with LazyLogging {

  private final val DEFAULT_CONFIG = "simple-rbac.cfg.xml"

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: SimpleRbacConfig = _
  var initialized = false
  var validator: Validator = _
  val config = new Config
  val validatorLock = new AnyRef

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
      logger.error("Simple RBAC filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(500)
    } else {
      val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
      val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

      logger.trace("Simple RBAC filter processing request...")
      validatorLock synchronized {
        validator.validate(mutableHttpRequest, mutableHttpResponse, filterChain)
      }
    }
    logger.trace("Simple RBAC filter returning response...")
  }

  override def configurationUpdated(configurationObject: SimpleRbacConfig): Unit = {
    configuration = configurationObject
    config.enableRaxRolesExtension = true
    config.checkPlainParams = true
    config.maskRaxRoles403 = configuration.isEnableMasking403S
    config.setResultHandler(getHandlers)

    val rbacWadl = rbacToWadl(Option(configuration.getResources)).orElse(
      Option(configuration.getResourcesFileName: String) match {
        case Some(fileName) =>
          rbacToWadl(readResource(
              configurationService.getResourceResolver.resolve(fileName).newInputStream()
          ))
        case _ => None
      }
    )
    rbacWadl match {
      case Some(wadl) =>
        Option(configuration.getWadlOutput: String) match {
          case Some(wadlOutput) =>
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
          case _ =>
        }
        validatorLock synchronized {
          initialized = reinitValidator(
            s"SimpleRbacValidator",
            new StreamSource(new ByteArrayInputStream(wadl.getBytes), "file://simple-rbac.wadl"),
            config
          )
        }
       logger.error("Unable to generate the WADL; check the provided resources.")
    }
  }

  override def isInitialized: Boolean = initialized

  private def getHandlers: DispatchHandler = {
    val handlers: util.List[ResultHandler] = new util.ArrayList[ResultHandler]
    Option(configuration.getDelegating) match {
      case Some(delegating) =>
        handlers.add(new MethodLabelHandler)
        handlers.add(new DelegationHandler(delegating.getQuality))
        handlers.add(new ServletResultHandler)
      case _ =>
    }
    if (configuration.isEnableApiCoverage) {
      handlers.add(new InstrumentedHandler)
      handlers.add(new ApiCoverageHandler)
    }
    Option(configuration.getDotOutput) match {
      case Some(dotName) =>
        if (StringUtils.isNotBlank(dotName)) {
          val dotPath: String = StringUriUtilities.formatUri(getPath(dotName, configurationRoot))
          val out: File = new File(dotPath)
          try {
            if (out.exists && out.canWrite || !out.exists && out.createNewFile) {
              handlers.add(new SaveDotHandler(out, true, true))
            } else {
              logger.warn("Cannot write to DOT file: " + dotPath)
            }
          } catch {
            case ex: IOException =>
              logger.warn("Cannot write to DOT file: " + dotPath, ex)
          }
        }
      case _ =>
    }
    new DispatchHandler(handlers.toList:_*)
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
      val values = line.split("\\s+")
      if (values.length == 3) {
        Some(new Resource(
          values(0),
          Try(values(1).split(',').toSet[String].map(_.trim)).getOrElse(Set.empty),
          Try(values(2).split(',').toSet[String].map(_.trim)).getOrElse(Set.empty)
        ))
      } else {
        logger.warn(s"Malformed RBAC Resource: $line")
        None
      }
    }

    val parsed = rbac match {
      case Some(lines) =>
        Some(lines.replaceAll("[\r?\n?]", "\n").split('\n').toList.flatMap(parseLine))
      case _ => None
    }

    parsed match {
      case Some(values) =>
        val header = s"""<application xmlns:rax="http://docs.rackspace.com/api"
                        |         xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        |         xmlns="http://wadl.dev.java.net/2009/02"
                        |    >
                        |  <resources base="http://localhost">""".stripMargin
        val resources = values.map { value =>
          def toMethods(resource: Resource, uuid: UUID) = {
            val roles = resource.roles.mkString(" ")
            val raxRoles = roles match {
              case anyAll if roles.equalsIgnoreCase("ANY") || roles.equalsIgnoreCase("ALL") => ""
              case _ => s"""rax:roles="$roles""""
            }
            resource.methods.map {
              _ match {
                case anyAll if anyAll.equalsIgnoreCase("ANY") || anyAll.equalsIgnoreCase("ALL") =>
                  s"""      <method name="GET"    id="_$uuid-GET"    $raxRoles/>
                     |      <method name="PUT"    id="_$uuid-PUT"    $raxRoles/>
                     |      <method name="POST"   id="_$uuid-POST"   $raxRoles/>
                     |      <method name="DELETE" id="_$uuid-DELETE" $raxRoles/>""".stripMargin
                case method =>
                  s"""      <method name="$method"   id="_$uuid-$method"    $raxRoles/>"""
              }
            }.mkString("\n")
          }
          val path = value.path
          val uuid = UUID.randomUUID
          val methods = toMethods(value, uuid)
          s"""    <resource id="_$uuid" path=\"$path">
             |$methods
             |    </resource>""".stripMargin
        }.mkString("\n")
        val footer = s"""  </resources>
                        |</application>""".stripMargin

        Some(s"$header\n$resources\n$footer")
      case _ => None
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
          case ex: Throwable =>
            logger.warn("Error loading validator for WADL!!! ", ex)
            false
        }
    }
  }

  def clearValidator() {
    Option(validator) match {
      case Some(_) =>
        logger.debug (s"Destroying: $validator. From thread {}", Thread.currentThread.getName)
        validator.destroy
        validator = null
      case _ =>
    }
  }

  def reinitValidator(name : String, source : javax.xml.transform.Source, config : com.rackspace.com.papi.components.checker.Config): Boolean = {
    clearValidator()
    initValidator(name, source, config)
  }
}
