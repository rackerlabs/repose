package org.openrepose.filters.herp

import java.io.StringWriter
import java.util.concurrent.ExecutorService

import com.github.jknack.handlebars.Template
import com.hazelcast.core.TransactionalQueue
import com.hazelcast.transaction.TransactionContext
import org.slf4j.Logger

class AuditConsumer(executorService: ExecutorService,
                    logger: Logger,
                    template: Template,
                    queue: TransactionalQueue[TemplateValues],
                    transactionContext: TransactionContext) extends Runnable {

  override def run(): Unit = {
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
        executorService.submit(new AuditConsumer(executorService, logger, template, queue, transactionContext))
    }
  }
}
