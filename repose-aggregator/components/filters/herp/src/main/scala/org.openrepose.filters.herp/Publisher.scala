package org.openrepose.filters.herp

import com.typesafe.scalalogging.slf4j.LazyLogging

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/26/15
 * Time: 7:54 AM
 */
class Publisher extends LazyLogging {
  def sendEvent(eventBody :String): Unit = {
    logger.error("***************************************************************Butts*********************************************************************\n" + eventBody)
  }
}
