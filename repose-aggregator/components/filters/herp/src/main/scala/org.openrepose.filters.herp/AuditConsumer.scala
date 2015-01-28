package org.openrepose.filters.herp

import java.io.StringWriter

import com.github.jknack.handlebars.Template
import com.hazelcast.core.TransactionalQueue
import com.hazelcast.transaction.TransactionContext
import org.slf4j.Logger

class AuditConsumer(logger: Logger, template: Template, queue: TransactionalQueue[TemplateValues], transactionContext: TransactionContext) extends Runnable {

  private var runState: Thread.State = Thread.State.NEW

  override def run(): Unit = {
    runState = Thread.State.RUNNABLE
    while (runState == Thread.State.RUNNABLE) {
      try {
        transactionContext.beginTransaction()
        val templateValues = queue.take()
        val templateOutput = new StringWriter
        template.apply(templateValues, templateOutput)

        logger.info(templateOutput.toString)
        transactionContext.commitTransaction()
      } catch {
        case _: Throwable =>
          transactionContext.rollbackTransaction()
      }
    }
  }

  def terminate(): Unit = {
    runState = Thread.State.TERMINATED
  }
}
