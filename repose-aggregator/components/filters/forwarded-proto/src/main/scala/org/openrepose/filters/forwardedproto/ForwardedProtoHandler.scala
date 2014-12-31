package org.openrepose.filters.forwardedproto

import javax.servlet.http.HttpServletRequest

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.codec.binary.Base64
import org.openrepose.commons.utils.http._
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector}

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

/**
 * Created by eric7500 on 12/30/14.
 */
class ForwardedProtoHandler extends AbstractFilterLogicHandler with LazyLogging{

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val filterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.requestHeaderManager()
    headerManager.putHeader("X-Forwarded-Proto", request.getProtocol())
    filterDirector
  }

}



