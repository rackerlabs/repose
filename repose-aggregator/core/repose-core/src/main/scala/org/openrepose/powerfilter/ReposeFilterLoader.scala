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
package org.openrepose.powerfilter

import java.util
import java.util.UUID

import com.oracle.javaee6.FilterType
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}
import javax.servlet.{Filter, ServletContext, ServletException}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.classloader.EarClassLoaderContext
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.classloader.ClassLoaderManagerService
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.deploy.ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED
import org.openrepose.core.services.deploy.{ApplicationDeploymentEvent, ArtifactManager}
import org.openrepose.core.services.event.PowerFilterEvent.POWER_FILTER_CONFIGURED
import org.openrepose.core.services.event.{Event, EventListener, EventService}
import org.openrepose.core.services.healthcheck.{HealthCheckService, Severity}
import org.openrepose.core.services.jmx.ConfigurationInformation
import org.openrepose.core.spring.{CoreSpringProvider, ReposeSpringProperties}
import org.openrepose.core.systemmodel.config.{Filter => FilterConfig, _}
import org.openrepose.powerfilter.ReposeFilterLoader._
import org.openrepose.powerfilter.filtercontext.FilterConfigWrapper
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

@Named("reposeFilterLoader")
class ReposeFilterLoader @Inject()(@Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                                   configurationService: ConfigurationService,
                                   eventService: EventService,
                                   healthCheckService: HealthCheckService,
                                   artifactManager: ArtifactManager,
                                   applicationContext: ApplicationContext,
                                   configurationInformation: ConfigurationInformation,
                                   classLoaderManagerService: ClassLoaderManagerService)
  extends UpdateListener[SystemModel]
    with EventListener[ApplicationDeploymentEvent, util.List[String]]
    with StrictLogging {

  private val healthCheckServiceProxy = healthCheckService.register
  private var currentSystemModel: Option[SystemModel] = None
  private var currentFilterContextRegistrar: Option[FilterContextRegistrar] = None
  private var currentServletContext: Option[ServletContext] = None
  private val configurationLock = new Object
  private var initialized: Boolean = false

  @PostConstruct
  def init(): Unit = {
    logger.info("{} -- Initializing ReposeFilterLoader", nodeId)
    val xsdURL = getClass.getResource("/META-INF/schema/system-model/system-model.xsd")
    configurationService.subscribeTo("system-model.cfg.xml", xsdURL, this, classOf[SystemModel])
    eventService.listen(this, APPLICATION_COLLECTION_MODIFIED)
    logger.trace("{} -- initialized.", nodeId)
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.trace("{} -- destroying ...", nodeId)
    healthCheckServiceProxy.deregister()
    eventService.squelch(this, APPLICATION_COLLECTION_MODIFIED)
    configurationService.unsubscribeFrom("system-model.cfg.xml", this)
    logger.info("{} -- Destroyed ReposeFilterLoader", nodeId)
  }

  override def configurationUpdated(configurationObject: SystemModel): Unit = {
    logger.debug("{} New system model configuration provided", nodeId)
    currentSystemModel = Option(configurationObject)
    currentSystemModel.foreach { _ =>
      logger.debug("{} -- issuing POWER_FILTER_CONFIGURED event from a configuration update", nodeId)
      eventService.newEvent(POWER_FILTER_CONFIGURED, System.currentTimeMillis)
    }
    configurationCheck()
    initialized = true
  }

  override def isInitialized: Boolean = initialized

  override def onEvent(e: Event[ApplicationDeploymentEvent, util.List[String]]): Unit = {
    logger.info("{} -- Application collection has been modified. Application that changed: {}", nodeId, e.payload)
    val duplicates = e.payload().asScala.groupBy(identity).collect { case (x, List(_, _, _*)) => x }
    if (duplicates.isEmpty) {
      healthCheckServiceProxy.resolveIssue(ApplicationDeploymentHealthReport)
    } else {
      val message = s"Please review your artifacts directory, multiple versions of the artifact${if (duplicates.size > 1) "s" else ""} exist! (${duplicates.mkString(",")})"
      healthCheckServiceProxy.reportIssue(ApplicationDeploymentHealthReport, message, Severity.BROKEN)
      logger.error(message)
    }
    if (artifactManager.allArtifactsLoaded) {
      configurationCheck()
    }
  }

  def setServletContext(newServletContext: ServletContext): Unit = {
    currentServletContext = Option(newServletContext)
    configurationCheck()
  }

  def getFilterContextList: Option[FilterContextList] = {
    currentFilterContextRegistrar.map(_.bind())
  }

  def getTracingHeaderConfig: Option[TracingHeaderConfig] = currentSystemModel.map(_.getTracingHeader)

  /**
    * Triggered each time the event service triggers an app deploy and when the system model is updated.
    */
  private def configurationCheck(): Unit = {
    (currentSystemModel, currentServletContext, artifactManager.allArtifactsLoaded) match {
      case (Some(systemModel), Some(servletContext), true) =>
        updateConfiguration(systemModel, servletContext)
      case _ =>
        logger.trace("{} -- Not ready to update yet.", nodeId)
    }
  }

  private def updateConfiguration(systemModel: SystemModel, servletContext: ServletContext): Unit = {
    configurationLock.synchronized {
      val interrogator = new SystemModelInterrogator(nodeId)

      Option(interrogator.getLocalCluster(systemModel).orElse(null)) match {
        case Some(localCluster) =>
          healthCheckServiceProxy.resolveIssue(SystemModelConfigHealthReport)
          try {
            // Only if we've been configured with some filters should we get a new list
            // Sometimes we won't have any filters
            val newFilterChain = Option(localCluster.getFilters) match {
              case Some(listOfFilters: FilterList) => buildFilterContexts(servletContext, listOfFilters.getFilter.asScala.toList)
              case _ => List.empty[FilterContext]
            }

            // Set the new FilterContextRegistrar and
            // set the Close flag on the old one.
            currentFilterContextRegistrar.synchronized {
              val previousFilterContextRegistrar = currentFilterContextRegistrar
              currentFilterContextRegistrar = Option(new FilterContextRegistrar(newFilterChain, Option(localCluster.getFilters.getBypassUriRegex)))
              previousFilterContextRegistrar
            }.foreach(_.close())

            val filterChainInfo = newFilterChain.map(ctx => ctx.filter.getClass.getName).mkString(",")
            logger.debug("{} -- Repose filter chain: {}", nodeId, filterChainInfo)

            //Only log this repose ready if we're able to properly fire up a new filter chain
            logger.info("{} -- Repose ready", nodeId)
            //Update the JMX bean with our status
            configurationInformation.updateNodeStatus(nodeId, true)
          } catch {
            case fie: FilterInitializationException =>
              logger.error("{} -- Unable to create new filter chain.", nodeId, fie)
              //Update the JMX bean with our status
              configurationInformation.updateNodeStatus(nodeId, false)
          }
        case _ =>
          logger.error("{} -- Unhealthy system-model config (cannot identify local node, or no default destination) - please check your system-model.cfg.xml", nodeId)
          healthCheckServiceProxy.reportIssue(SystemModelConfigHealthReport,
            "Unable to identify the local host in the system model, or no default destination - please check your system-model.cfg.xml",
            Severity.BROKEN)
      }
    }
  }

  @throws[FilterInitializationException]
  private def buildFilterContexts(servletContext: ServletContext, filtersToCreate: List[FilterConfig]): List[FilterContext] = {
    filtersToCreate.map { filterToCreate =>
      if (classLoaderManagerService.hasFilter(filterToCreate.getName)) {
        loadFilterContext(classLoaderManagerService.getLoadedApplications, filterToCreate, servletContext)
      } else {
        val message = s"Unable to satisfy requested filter chain - none of the loaded artifacts supply a filter named ${filterToCreate.getName}"
        logger.error(message)
        throw new FilterInitializationException(message)
      }
    }
  }

  /**
    * Load a FilterContext for a filter
    *
    * @param loadedApplications The list of EarClassLoaders
    * @param filterConfig       The Jaxb filter configuration information from the system-model
    * @param servletContext     The Servlet Context to build the new Filter Config Wrapper with
    * @return a FilterContext containing an instance of the filter and metatadata
    * @throws org.openrepose.powerfilter.FilterInitializationException
    */
  @throws[FilterInitializationException]
  private def loadFilterContext(loadedApplications: util.Collection[EarClassLoaderContext], filterConfig: FilterConfig, servletContext: ServletContext): FilterContext = {
    loadedApplications.asScala.toStream.map { classLoaderContext =>
      (classLoaderContext.getEarDescriptor.getRegisteredFilters.get(filterConfig.getName), classLoaderContext.getClassLoader)
    }.find { case (filterType, _) => filterType != null } match {
      case Some((filterType, filterClassLoader)) =>
        // We got a Filter Type and a classloader, so do actual work
        getFilterContext(filterConfig, servletContext, filterType, filterClassLoader)
      case None =>
        throw new FilterInitializationException(s"Requested filter, ${filterConfig.getName} can not be found.")
    }
  }

  @throws[FilterInitializationException]
  private def getFilterContext(filterConfig: FilterConfig, servletContext: ServletContext, filterType: FilterType, filterClassLoader: ClassLoader): FilterContext = {
    val filterClassName = filterType.getFilterClass.getValue
    try {
      logger.info("Getting child application context for {} using classloader {}", filterClassName, filterClassLoader.toString)
      val filterContext = CoreSpringProvider.getContextForFilter(applicationContext, filterClassLoader, filterClassName, getUniqueContextName(filterConfig))
      //Get the specific class to load from the application context
      val filterClass: Class[_] = filterClassLoader.loadClass(filterClassName)
      val newFilterInstance: Filter = try {
        filterContext.getBean(filterClass).asInstanceOf[Filter]
      } catch {
        case e: NoSuchBeanDefinitionException =>
          logger.debug("Could not load the filter {} using Spring. Will try to manually load the class instead.", filterClassName, e)
          //Spring didn't load the filter as a bean, try manually creating a new instance of the class
          val rtnFilterInstance = filterClass.newInstance.asInstanceOf[Filter]
          //Add the instance to the application context using its full class name
          filterContext.getBeanFactory.registerSingleton(rtnFilterInstance.getClass.getName, rtnFilterInstance)
          rtnFilterInstance
      }
      newFilterInstance.init(new FilterConfigWrapper(servletContext, filterType, filterConfig.getConfiguration))
      logger.info("Filter Instance: {} successfully created", newFilterInstance)
      FilterContext(newFilterInstance, filterConfig, filterConfig.getFilterCriterion.evaluate, filterContext)
    } catch {
      case e: ClassNotFoundException =>
        throw new FilterInitializationException(s"Requested filter, $filterClassName does not exist in any loaded artifacts", e)
      case e: ServletException =>
        val message = s"Failed to initialize filter $filterClassName"
        logger.error(message)
        throw new FilterInitializationException(message, e)
      case e: ClassCastException =>
        throw new FilterInitializationException(s"Requested filter, $filterClassName is not of type javax.servlet.Filter", e)
      case e@(_: InstantiationException | _: IllegalAccessException) =>
        throw new FilterInitializationException(s"Requested filter, $filterClassName is not an annotated Component nor does it have a public zero-argument constructor.", e)
    }
  }

  private def getUniqueContextName(filterInfo: FilterConfig): String = {
    val sb = new StringBuilder
    if (filterInfo.getId != null) sb.append(filterInfo.getId).append("-")
    sb.append(filterInfo.getName).append("-")
    sb.append(UUID.randomUUID.toString)
    sb.toString
  }
}

object ReposeFilterLoader {

  case class FilterContext(filter: Filter, filterConfig: FilterConfig, shouldRun: HttpServletRequestWrapper => Boolean, appContext: AbstractApplicationContext)

  case class FilterContextList(registrar: FilterContextRegistrar, filterContexts: List[FilterContext], bypassUriRegex: Option[String]) extends AutoCloseable {
    override def close(): Unit = registrar.release(this)
  }

  class FilterContextRegistrar(val filterContexts: List[FilterContext], val bypassUriRegex: Option[String]) extends AutoCloseable {
    val inUse = ListBuffer.empty[FilterContextList]
    var doClose = false

    def bind(): FilterContextList = {
      // There is no need to check the doClose since this Registrar would already
      // have been replaced by the atomic getAndSet() before the close() is called.
      inUse.synchronized {
        val filterContextList = new FilterContextList(this, filterContexts, bypassUriRegex)
        inUse += filterContextList
        filterContextList
      }
    }

    def release(filterContextList: FilterContextList): Unit = {
      inUse.synchronized {
        inUse -= filterContextList
      }
      checkShutdown()
    }

    override def close(): Unit = {
      doClose = true
      checkShutdown()
    }

    private def checkShutdown(): Unit = {
      inUse.synchronized {
        if (doClose && inUse.isEmpty) {
          filterContexts.foreach { filterContext =>
            Option(filterContext.filter).foreach(_.destroy())
            Option(filterContext.appContext).foreach(_.close())
          }
        }
      }
    }
  }

  val SystemModelConfigHealthReport = "SystemModelConfigError"
  val ApplicationDeploymentHealthReport = "ApplicationDeploymentError"
}
