package org.openrepose.filters.herp

import java.io.StringWriter

import com.github.jknack.handlebars.Template
import com.hazelcast.core.TransactionalQueue
import org.slf4j.Logger

class AuditConsumer(logger: Logger, template: Template, queue: TransactionalQueue[TemplateValues]) extends Runnable {

  private var runState: Thread.State = Thread.State.NEW

  override def run(): Unit = {
    context.begingTransaction
    runState = Thread.State.RUNNABLE
    while (runState == Thread.State.RUNNABLE) {
      val templateValues = queue.take()
      val templateOutput = new StringWriter
      template.apply(templateValues, templateOutput)

      logger.info(templateOutput.toString)
    }
  }

  def terminate(): Unit = {
    runState = Thread.State.TERMINATED
  }
}
