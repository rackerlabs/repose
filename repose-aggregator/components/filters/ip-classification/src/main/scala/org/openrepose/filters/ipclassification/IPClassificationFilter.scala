package org.openrepose.filters.ipclassification

import javax.inject.{Inject, Named}
import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.core.services.config.ConfigurationService

@Named
class IPClassificationFilter @Inject()(configurationService: ConfigurationService) extends Filter with LazyLogging {

  override def init(filterConfig: FilterConfig): Unit = ???

  override def destroy(): Unit = ???

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = ???
}
