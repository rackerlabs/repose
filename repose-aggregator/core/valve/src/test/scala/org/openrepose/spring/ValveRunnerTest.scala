package org.openrepose.spring

import org.junit.runner.RunWith
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.container.config.ContainerConfiguration
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.valve.spring.ValveRunner
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}


@RunWith(classOf[JUnitRunner])
class ValveRunnerTest extends FunSpec with Matchers {
  val log = LoggerFactory.getLogger(this.getClass)

  val fakeConfigService = new FakeConfigService()

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  def withRunner(configRoot: String = "/config/root", insecure: Boolean = false)(f: ValveRunner => Unit) = {
    val runner = new ValveRunner(fakeConfigService)
    val runnerTask = Future {
      runner.run(configRoot, insecure)
    }

    try {
      f(runner)
    } finally {
      runner.stop()
    }
    Await.ready(runnerTask, 3 seconds)
  }

  def updateSystemModel(resource:String):UpdateListener[SystemModel] = {
    val systemModelListener = fakeConfigService.getListener[SystemModel]("system-model.cfg.xml")
    val systemModel = Marshaller.systemModel(resource)
    systemModelListener.configurationUpdated(systemModel)
    systemModelListener
  }

  def updateContainerConfig(resource:String):UpdateListener[ContainerConfiguration] = {
    val containerListener = fakeConfigService.getListener[ContainerConfiguration]("container.cfg.xml")
    val containerConfig = Marshaller.containerConfig(resource)
    containerListener.configurationUpdated(containerConfig)
    containerListener
  }

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
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty

        updateContainerConfig("/valveTesting/without-keystore.xml")

        //it should not have triggered any nodes
        runner.getActiveNodes shouldBe empty
      }

    }
    it("doesn't start nodes if only the system-model has been configured") {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty

        updateSystemModel("/valveTesting/system-model-1.cfg.xml")

        //it should not have triggered any nodes
        runner.getActiveNodes shouldBe empty
      }
    }
    it("Starts up nodes as configured in the system-model when hit with a system model before a container config") {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty

        updateSystemModel("/valveTesting/system-model-1.cfg.xml")
        updateContainerConfig("/valveTesting/without-keystore.xml")

        runner.getActiveNodes.size shouldBe 1
      }
    }
    it("Starts up nodes as configured in the system-model when given a container config before a system-model") {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty

        updateContainerConfig("/valveTesting/without-keystore.xml")
        updateSystemModel("/valveTesting/system-model-1.cfg.xml")

        runner.getActiveNodes.size shouldBe 1
      }
    }
  }

  describe("When started with a single node") {
    it("restarts that node when a change to the container.cfg.xml happens") {
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
  describe("When started with multiple local nodes") {
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
