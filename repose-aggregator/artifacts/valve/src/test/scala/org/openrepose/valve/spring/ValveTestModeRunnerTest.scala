/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.valve.spring

import java.lang.management.ManagementFactory
import javax.management.{JMX, ObjectName}

import com.typesafe.scalalogging.slf4j.StrictLogging
import org.junit.runner.RunWith
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.container.config.ContainerConfiguration
import org.openrepose.core.spring.CoreSpringProvider
import org.openrepose.core.systemmodel.config.SystemModel
import org.openrepose.nodeservice.test.FakeContainerConfigurationService
import org.openrepose.valve.jmx.ValvePortMXBean
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}


@RunWith(classOf[JUnitRunner])
class ValveTestModeRunnerTest extends FunSpec with Matchers with StrictLogging {
  val log = LoggerFactory.getLogger(this.getClass)

  val fakeConfigService = new FakeConfigService()
  val fakeContainerConfigurationService =
    CoreSpringProvider.getInstance().getNodeContext("node").getBean(classOf[FakeContainerConfigurationService])

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  /**
   * For this class, the test mode is going to be true!
 *
   * @param configRoot
   * @param insecure
   * @param testMode
   * @param f
   * @return
   */
  def withRunner(configRoot: String = "/config/root",
                 insecure: Boolean = false,
                 testMode: Boolean = true)(f: ValveRunner => Unit) = {
    val runner = new ValveRunner(fakeConfigService)
    val runnerTask = Future {
      runner.run(configRoot, insecure, testMode)
    }

    runnerTask.onFailure {
      case t => fail("Future didn't go!", t)
    }
    try {
      f(runner)
    } finally {
      runner.destroy()
    }
    Await.ready(runnerTask, 3 seconds)
  }

  def getValvePortMXBean: ValvePortMXBean = {
    val mbs = ManagementFactory.getPlatformMBeanServer
    val name = new ObjectName(ValvePortMXBean.OBJECT_NAME)
    if (mbs.isRegistered(name)) {
      println("Getting the registered mbean!")
      val thing = JMX.newMBeanProxy(mbs, name, classOf[ValvePortMXBean])
      thing
    } else {
      fail("Unable to get ValvePort MX Bean!")
    }
  }

  def updateSystemModel(resource: String): UpdateListener[SystemModel] = {
    val systemModelListener = fakeConfigService.getListener[SystemModel]("system-model.cfg.xml")
    val systemModel = Marshaller.systemModel(resource)
    systemModelListener.configurationUpdated(systemModel)
    systemModelListener
  }

  def updateContainerConfig(resource: String): UpdateListener[ContainerConfiguration] = {
    val containerListener = fakeConfigService.getListener[ContainerConfiguration]("container.cfg.xml")
    val containerConfig = Marshaller.containerConfig(resource)
    containerListener.configurationUpdated(containerConfig)
    fakeContainerConfigurationService.deploymentConfiguration = containerConfig.getDeploymentConfig
    containerListener
  }

  it("still has a blocking run method") {
    val runner = new ValveRunner(fakeConfigService)

    val future = Future {
      runner.run("/root", false)
    }
    future.isCompleted shouldBe false

    runner.destroy()
    Await.ready(future, 1 second)
    future.isCompleted shouldBe true
  }

  describe("Starting fresh") {
    it("Does nothing if the container-config is updated before the system-model") {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty

        updateContainerConfig("/valveTesting/without-keystore.cfg.xml")

        //it should not have triggered any nodes
        runner.getActiveNodes shouldBe empty
        getValvePortMXBean.getPort("repose_node1") shouldBe 0
      }

    }
    it("doesn't start nodes if only the system-model has been configured") {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty

        updateSystemModel("/valveTesting/1node/system-model-1.cfg.xml")

        //it should not have triggered any nodes
        runner.getActiveNodes shouldBe empty
        getValvePortMXBean.getPort("repose_node1") shouldBe 0
      }
    }
    it("Starts up nodes as configured in the system-model when hit with a system model before a container config") {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty

        updateSystemModel("/valveTesting/1node/system-model-1.cfg.xml")
        updateContainerConfig("/valveTesting/without-keystore.cfg.xml")

        runner.getActiveNodes.size shouldBe 1
        val port = getValvePortMXBean.getPort("repose_node1")
        logger.debug(s"PORT IS: $port")
        port shouldNot be(0)
        port shouldNot be(10234) //It shouldn't match exactly what we configured...
      }
    }
    it("Starts up nodes as configured in the system-model when given a container config before a system-model") {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty

        updateContainerConfig("/valveTesting/without-keystore.cfg.xml")
        updateSystemModel("/valveTesting/1node/system-model-1.cfg.xml")

        runner.getActiveNodes.size shouldBe 1
        val port = getValvePortMXBean.getPort("repose_node1")
        logger.debug(s"PORT IS: $port")
        port shouldNot be(0)
        port shouldNot be(10234) //It shouldn't match exactly what we configured...
      }
    }
  }

  describe("When started with a single node") {
    def withSingleNodeRunner(f: ValveRunner => Unit) = {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty
        updateContainerConfig("/valveTesting/without-keystore.cfg.xml")
        updateSystemModel("/valveTesting/1node/system-model-1.cfg.xml")
        f(runner)
      }
    }

    it("restarts that node when an update event to the container.cfg.xml happens") {
      withSingleNodeRunner { runner =>
        runner.getActiveNodes.size shouldBe 1
        val node = runner.getActiveNodes.head
        val oldPort = node.runningHttpPort
        val jmxOldPort = getValvePortMXBean.getPort("repose_node1")

        oldPort shouldEqual jmxOldPort

        updateContainerConfig("/valveTesting/without-keystore.cfg.xml")
        runner.getActiveNodes.size shouldBe 1
        runner.getActiveNodes.head shouldNot be(node)

        val jmxNewPort = getValvePortMXBean.getPort("repose_node1")
        jmxNewPort shouldNot equal(0)
        jmxNewPort shouldEqual runner.getActiveNodes.head.runningHttpPort
      }
    }
    describe("When updating the system-model") {
      it("A node needs to be changed if it's ports don't match, even in testing mode") {
        withSingleNodeRunner { runner =>
          val node = runner.getActiveNodes.head
          node.nodeId shouldBe "repose_node1"
          node.httpPort.isDefined shouldEqual true
          node.httpPort.get shouldEqual 10234 //Configured port
          node.runningHttpPort shouldNot equal(10234) //Actually running port

          val jmxOldPort = getValvePortMXBean.getPort("repose_node1")
          jmxOldPort shouldNot equal(0)

          updateSystemModel("/valveTesting/1node/change-node-1-port.cfg.xml")
          runner.getActiveNodes.size shouldBe 1
          val newNode = runner.getActiveNodes.head
          newNode shouldNot be(node)
          newNode.httpPort.isDefined shouldEqual true
          newNode.httpPort.get shouldEqual 10235 //configured port
          newNode.runningHttpPort shouldNot equal(10235) //actually running port

          val jmxNewPort = getValvePortMXBean.getPort("repose_node1")
          jmxNewPort shouldNot equal(0)
          jmxNewPort shouldEqual newNode.runningHttpPort

          newNode.nodeId shouldBe "repose_node1"
        }
      }
      it("restarts the changed node") {
        withSingleNodeRunner { runner =>
          val node = runner.getActiveNodes.head
          node.nodeId shouldBe "repose_node1"
          val beforePort = getValvePortMXBean.getPort("repose_node1")
          beforePort shouldNot equal(0)

          updateSystemModel("/valveTesting/1node/change-node-1.cfg.xml")
          runner.getActiveNodes.size shouldBe 1
          runner.getActiveNodes.head shouldNot be(node)
          val afterPort = getValvePortMXBean.getPort("le_repose_node")
          afterPort shouldNot equal(0)

          getValvePortMXBean.getPort("repose_node1") shouldEqual 0

          val changedNode = runner.getActiveNodes.head
          changedNode.nodeId shouldBe "le_repose_node"
        }
      }
      it("will not do anything if the nodes are the same") {
        withSingleNodeRunner { runner =>
          val node = runner.getActiveNodes.head
          node.nodeId shouldBe "repose_node1"
          val beforePort = getValvePortMXBean.getPort("repose_node1")
          beforePort shouldNot equal(0)

          updateSystemModel("/valveTesting/1node/system-model-1.cfg.xml")
          runner.getActiveNodes.size shouldBe 1
          runner.getActiveNodes.head shouldBe node
          beforePort shouldEqual getValvePortMXBean.getPort("repose_node1")
        }
      }
    }
  }
  describe("When started with multiple local nodes") {
    def withTwoNodeRunner(f: ValveRunner => Unit) = {
      withRunner() { runner =>
        runner.getActiveNodes shouldBe empty
        updateContainerConfig("/valveTesting/without-keystore.cfg.xml")
        updateSystemModel("/valveTesting/2node/system-model-2.cfg.xml")
        f(runner)
      }
    }

    it("restarts all nodes when a change to the container.cfg.xml happens") {
      withTwoNodeRunner { runner =>
        runner.getActiveNodes.size shouldBe 2
        val node1 = runner.getActiveNodes.find(_.nodeId == "repose_node1").get
        val node2 = runner.getActiveNodes.find(_.nodeId == "repose_node2").get

        val node1Port = getValvePortMXBean.getPort("repose_node1")
        val node2Port = getValvePortMXBean.getPort("repose_node2")
        node1Port shouldNot equal(node2Port)
        node1Port shouldNot equal(0)
        node2Port shouldNot equal(0)

        updateContainerConfig("/valveTesting/without-keystore.cfg.xml")

        runner.getActiveNodes.size shouldBe 2

        val newNode1 = runner.getActiveNodes.find(_.nodeId == "repose_node1").get
        val newNode2 = runner.getActiveNodes.find(_.nodeId == "repose_node2").get

        newNode1 shouldNot be(node1)
        newNode2 shouldNot be(node2)

        newNode1.nodeId shouldBe node1.nodeId
        newNode2.nodeId shouldBe node2.nodeId
        val newNode1Port = getValvePortMXBean.getPort("repose_node1")
        val newNode2Port = getValvePortMXBean.getPort("repose_node2")
        newNode1Port shouldNot equal(newNode2Port)

        newNode1Port shouldNot equal(0)
        newNode2Port shouldNot equal(0)

        newNode1Port shouldNot equal(node1Port)
        newNode2Port shouldNot equal(node2Port)
      }
    }
    describe("When updating the system-model") {
      it("restarts only the changed nodes") {
        withTwoNodeRunner { runner =>
          val node2 = runner.getActiveNodes.find(_.nodeId == "repose_node2").get
          val node1Port = getValvePortMXBean.getPort("repose_node1")
          val node2Port = getValvePortMXBean.getPort("repose_node2")

          updateSystemModel("/valveTesting/2node/change-node-2.cfg.xml")

          runner.getActiveNodes.size shouldBe 2
          val newNode2 = runner.getActiveNodes.find(_.nodeId == "le_changed_node").get
          newNode2 shouldNot be(node2)
          val newNode2Port = getValvePortMXBean.getPort("repose_node2")
          newNode2Port shouldNot equal(node2Port)

          getValvePortMXBean.getPort("repose_node1") shouldEqual node1Port
        }
      }
      it("Stops removed nodes") {
        withTwoNodeRunner { runner =>
          runner.getActiveNodes.size shouldBe 2
          val node1 = runner.getActiveNodes.find(_.nodeId == "repose_node1").get

          updateSystemModel("/valveTesting/2node/remove-node-2.cfg.xml")
          runner.getActiveNodes.size shouldBe 1
          val stillNode1 = runner.getActiveNodes.head

          stillNode1 shouldBe node1

          getValvePortMXBean.getPort("repose_node2") shouldEqual 0
          getValvePortMXBean.getPort("repose_node1") shouldNot equal(0)
        }
      }
      it("starts new nodes") {
        withTwoNodeRunner { runner =>
          runner.getActiveNodes.size shouldBe 2
          val node1 = runner.getActiveNodes.find(_.nodeId == "repose_node1").get
          val node2 = runner.getActiveNodes.find(_.nodeId == "repose_node2").get

          val node1Port = getValvePortMXBean.getPort("repose_node1")
          val node2Port = getValvePortMXBean.getPort("repose_node2")
          val node3Port = getValvePortMXBean.getPort("repose_node3")

          node3Port shouldEqual 0

          updateSystemModel("/valveTesting/2node/add-node-3.cfg.xml")

          runner.getActiveNodes.size shouldBe 3
          val newNode1 = runner.getActiveNodes.find(_.nodeId == "repose_node1").get
          val newNode2 = runner.getActiveNodes.find(_.nodeId == "repose_node2").get
          val newNode3 = runner.getActiveNodes.find(_.nodeId == "repose_node3").get

          newNode1 shouldBe node1
          newNode2 shouldBe node2

          newNode3.nodeId shouldBe "repose_node3"

          val newNode1Port = getValvePortMXBean.getPort("repose_node1")
          val newNode2Port = getValvePortMXBean.getPort("repose_node2")
          val newNode3Port = getValvePortMXBean.getPort("repose_node3")

          newNode1Port shouldEqual node1Port
          newNode2Port shouldEqual node2Port
          newNode3Port shouldNot equal(0)

        }
      }
      it("will not do anything if the nodes are the same") {
        withTwoNodeRunner { runner =>
          runner.getActiveNodes.size shouldBe 2
          val node1 = runner.getActiveNodes.find(_.nodeId == "repose_node1").get
          val node2 = runner.getActiveNodes.find(_.nodeId == "repose_node2").get

          val node1Port = getValvePortMXBean.getPort("repose_node1")
          val node2Port = getValvePortMXBean.getPort("repose_node2")

          updateSystemModel("/valveTesting/2node/system-model-2.cfg.xml")

          runner.getActiveNodes.size shouldBe 2
          val newNode1 = runner.getActiveNodes.find(_.nodeId == "repose_node1").get
          val newNode2 = runner.getActiveNodes.find(_.nodeId == "repose_node2").get

          newNode1 shouldBe node1
          newNode2 shouldBe node2

          node1Port shouldEqual getValvePortMXBean.getPort("repose_node1")
          node2Port shouldEqual getValvePortMXBean.getPort("repose_node2")
        }
      }
    }
  }

  describe("Error states") {
    it("if no local nodes are detected, it shuts down valve!") {
      val runner = new ValveRunner(fakeConfigService)
      val runnerTask = Future {
        runner.run("/config/root", false)
      }

      runner.getActiveNodes shouldBe empty

      updateContainerConfig("/valveTesting/without-keystore.cfg.xml")
      updateSystemModel("/valveTesting/0node/system-model-0.cfg.xml")
      runner.getActiveNodes shouldBe empty
      val exitCode = Await.result(runnerTask, 1 second)
      exitCode shouldBe 0
    }
  }
}
