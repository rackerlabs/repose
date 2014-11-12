package org.openrepose.spring

import org.junit.runner.RunWith
import org.openrepose.core.container.config.ContainerConfiguration
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.valve.spring.ValveRunner
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}


@RunWith(classOf[JUnitRunner])
class ValveRunnerTest extends FunSpec with Matchers {
  val log = LoggerFactory.getLogger(this.getClass)

  val fakeConfigService = new FakeConfigService()

  import scala.concurrent.ExecutionContext.Implicits.global
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
        runner.getActiveNodes shouldBe empty

        val containerListener = fakeConfigService.getListener[ContainerConfiguration]("container.cfg.xml")
        val containerConfig = Marshaller.containerConfig("/valveTesting/without-keystore.xml")
        containerListener.configurationUpdated(containerConfig)

        //it should not have triggered any nodes
        runner.getActiveNodes shouldBe empty

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
        runner.getActiveNodes shouldBe empty

        val containerListener = fakeConfigService.getListener[SystemModel]("system-model.cfg.xml")
        val systemModel = Marshaller.systemModel("/valveTesting/system-model-1.cfg.xml")
        containerListener.configurationUpdated(systemModel)

        //it should not have triggered any nodes
        runner.getActiveNodes shouldBe empty

      } finally {
        runner.stop()
      }
    }
    it("Starts up nodes as configured in the system-model when hit with a system model before a container config") {
      val runner = new ValveRunner(fakeConfigService)

      Future {
        runner.run("/root", false)
      }

      try {
        runner.getActiveNodes shouldBe empty

        val systemModelListener = fakeConfigService.getListener[SystemModel]("system-model.cfg.xml")
        val systemModel = Marshaller.systemModel("/valveTesting/system-model-1.cfg.xml")
        systemModelListener.configurationUpdated(systemModel)

        val containerListener = fakeConfigService.getListener[ContainerConfiguration]("container.cfg.xml")
        val containerConfig = Marshaller.containerConfig("/valveTesting/without-keystore.xml")
        containerListener.configurationUpdated(containerConfig)

        runner.getActiveNodes.size shouldBe 1

      } finally {
        runner.stop()
      }

    }
    it("Starts up nodes as configured in the system-model when given a container config before a system-model") {
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
