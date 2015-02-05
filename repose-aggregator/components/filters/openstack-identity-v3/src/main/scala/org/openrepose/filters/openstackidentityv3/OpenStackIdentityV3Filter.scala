package org.openrepose.filters.openstackidentityv3

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.httpclient.HttpClientService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.openstackidentityv3.config.OpenstackIdentityV3Config

@Named
class OpenStackIdentityV3Filter @Inject() (configurationService: ConfigurationService,
                                           datastoreService: DatastoreService,
                                           httpClientService: HttpClientService,
                                           akkaServiceClient: AkkaServiceClient) extends Filter with LazyLogging {

  private final val DEFAULT_CONFIG = "openstack-identity-v3.cfg.xml"

  private var config: String = _
  private var handlerFactory: OpenStackIdentityV3HandlerFactory = _

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + config)
    handlerFactory = new OpenStackIdentityV3HandlerFactory(akkaServiceClient, datastoreService)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/openstack-identity-v3.xsd")
    // TODO: Clean up the asInstanceOf below, if possible?
    configurationService.subscribeTo(filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[OpenstackIdentityV3Config]],
      classOf[OpenstackIdentityV3Config])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(handlerFactory.newHandler)
  }

  override def destroy() {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
