package org.openrepose.spring

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.config.manager.{UpdateListener, ConfigurationUpdateManager}
import org.openrepose.commons.config.parser.common.ConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.container.config.ContainerConfiguration
import org.openrepose.core.jmx.ConfigurationInformation
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.valve.spring.ValveRunner
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}


@RunWith(classOf[JUnitRunner])
class ValveRunnerTest extends FunSpec with Matchers {
  val log = LoggerFactory.getLogger(this.getClass)

  val fakeConfigService = new FakeConfigService()

  import ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  it("has a blocking run method") {
    val runner = new ValveRunner(fakeConfigService)

    val future = Future {
      runner.run("/root", false)
    }
    future.isCompleted shouldBe false

    runner.stop()
    Await.ready(future, 1 second)
    future.isCompleted shouldBe true
  }

  it("passes through configRoot and insecure to each node") {
    pending
  }

  describe("Starting fresh") {
    it("Does nothing if the container-config is updated before the system-model") {
      val runner = new ValveRunner(fakeConfigService)

      Future {
        runner.run("/root", false)
      }

      try {
        runner.activeNodes shouldBe empty

        val containerListener = fakeConfigService.getListener[ContainerConfiguration]("container.cfg.xml")
        val containerConfig = Marshaller.containerConfig("/valveTesting/with-keystore.xml")
        containerListener.configurationUpdated(containerConfig)

        //it should not have triggered any nodes
        runner.activeNodes shouldBe empty

      } finally {
        runner.stop()
      }

    }
    it("doesn't start nodes if only the system-model has been configured") {
      val runner = new ValveRunner(fakeConfigService)

      Future {
        runner.run("/root", false)
      }

      try {
        runner.activeNodes shouldBe empty

        val containerListener = fakeConfigService.getListener[SystemModel]("system-model.cfg.xml")
        val systemModel = Marshaller.systemModel("/valveTesting/system-model-1.cfg.xml")
        containerListener.configurationUpdated(systemModel)

        //it should not have triggered any nodes
        runner.activeNodes shouldBe empty

      } finally {
        runner.stop()
      }


    }
    it("Starts up nodes as configured in the system-model") {
      pending
    }
  }

  describe("When started with a single node") {
    it("restarts all nodes when a change to the container.cfg.xml happens") {
      pending
    }
    describe("When updating the system-model") {
      it("restarts only the changed nodes") {
        pending
      }
      it("Stops removed nodes") {
        pending
      }
      it("starts new nodes") {
        pending
      }
      it("will not do anything if the nodes are the same") {
        pending
      }
    }
  }
}
