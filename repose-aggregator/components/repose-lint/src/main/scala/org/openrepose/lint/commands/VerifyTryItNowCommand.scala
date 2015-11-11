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
package org.openrepose.lint.commands

import java.io.File

import org.openrepose.lint.LintConfig

import scala.xml.{NodeSeq, XML}

/**
  * Ensures that the Repose configuration within a provided directory supports "Try-It-Now" traffic.
  */
object VerifyTryItNowCommand extends Command {

  private final val SYSTEM_MODEL_FILENAME = "system-model.cfg.xml"
  private final val AUTH_Z_FILTER_NAME = "client-authorization"
  private final val DEFAULT_AUTH_Z_FILENAME = "openstack-authorization.cfg.xml"
  private final val AUTH_N_FILTER_NAME = "client-auth"
  private final val DEFAULT_AUTH_N_FILENAME = "client-auth-n.cfg.xml"
  private final val KEYSTONEV2_FILTER_NAME = "keystone-v2"
  private final val DEFAULT_KEYSTONEV2_FILENAME = "keystone-v2.cfg.xml"
  private final val IDENTITYV3_FILTER_NAME = "openstack-identity-v3"
  private final val DEFAULT_IDENTITYV3_FILENAME = "openstack-identity-v3.cfg.xml"

  override def getCommandToken: String = {
    "verify-try-it-now"
  }

  override def getCommandDescription: String = {
    "verifies that tenant-less try-it-now traffic is supported by your Repose configuration"
  }

  override def perform(config: LintConfig): Unit = {
    def getFile(filename: String): Option[File] = {
      val file = new File(config.configDir, filename)

      if (file.exists && file.isFile && file.canRead) {
        Some(file)
      } else {
        println(s"$filename not found")
        None
      }
    }

    def getClusters(systemModel: File): NodeSeq = {
      val systemModelRoot = XML.loadFile(systemModel)
      systemModelRoot \ "repose-cluster"
    }

    def getFiltersNamed(filterName: String)(filters: NodeSeq): NodeSeq = {
      filters.filter(filter => (filter \ "@name").text.equals(filterName))
    }

    // TODO: extract common structure for checking configs
    def checkAuthN(filters: NodeSeq): Unit = {
      val authNFilters = getFiltersNamed(AUTH_N_FILTER_NAME)(filters)

      if (authNFilters.isEmpty) {
        println("auth-n filter IS NOT listed in the system-model")
      } else {
        var filterIndex = 1

        authNFilters foreach { filter =>
          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_AUTH_N_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").nonEmpty) {
            println(s"auth-n filter (#$filterIndex) IS filtered by uri-regex")
          } else {
            println(s"auth-n filter (#$filterIndex) IS NOT filtered by uri-regex")
          }

          config match {
            case Some(configRoot) =>
              val tenanted = (configRoot \ "openstack-auth" \ "@tenanted").map(node => node.text.toBoolean).headOption.getOrElse(true)
              if (tenanted) {
                println(s"auth-n filter (#$filterIndex) IS IN tenanted mode")
              } else {
                println(s"auth-n filter (#$filterIndex) IS NOT IN tenanted mode")
              }

              (configRoot \ "openstack-auth" \ "service-admin-roles" \ "role").find(node => node.text.equals("foyer")) match {
                case Some(_) => println(s"auth-n filter (#$filterIndex) HAS service-admin role foyer")
                case None => println(s"auth-n filter (#$filterIndex) DOES NOT HAVE service-admin role foyer")
              }

              (configRoot \ "openstack-auth" \ "ignore-tenant-roles" \ "role").find(node => node.text.equals("foyer")) match {
                case Some(_) => println(s"auth-n filter (#$filterIndex) HAS ignore-tenant role foyer")
                case None => println(s"auth-n filter (#$filterIndex) DOES NOT HAVE ignore-tenant role foyer")
              }
            case None =>
              println(s"auth-n (#$filterIndex) configuration NOT FOUND")
          }

          filterIndex += 1
        }
      }
    }

    def checkAuthZ(filters: NodeSeq): Unit = {
      val authNFilters = getFiltersNamed(AUTH_Z_FILTER_NAME)(filters)

      if (authNFilters.isEmpty) {
        println("auth-z filter IS NOT listed in the system-model")
      } else {
        var filterIndex = 1

        authNFilters foreach { filter =>
          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_AUTH_Z_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").nonEmpty) {
            println(s"auth-z (#$filterIndex) filter IS filtered by uri-regex")
          } else {
            println(s"auth-z (#$filterIndex) filter IS NOT filtered by uri-regex")
          }

          config match {
            case Some(configRoot) =>
              val ignoreTenantRoles = (configRoot \ "ignore-tenant-roles" \ "role") ++ (configRoot \ "ignore-tenant-roles" \ "ignore-tenant-role")
              ignoreTenantRoles.find(node => node.text.equals("foyer")) match {
                case Some(_) => println(s"auth-z filter (#$filterIndex) HAS ignore-tenant role foyer")
                case None => println(s"auth-z filter (#$filterIndex) DOES NOT HAVE ignore-tenant role foyer")
              }
            case None =>
              println(s"auth-z (#$filterIndex) configuration NOT FOUND")
          }

          filterIndex += 1
        }
      }
    }

    def checkKeystoneV2(filters: NodeSeq): Unit = {
      val keystoneV2Filters = getFiltersNamed(KEYSTONEV2_FILTER_NAME)(filters)

      if (keystoneV2Filters.isEmpty) {
        println("keystone-v2 filter IS NOT listed in the system-model")
      } else {
        var filterIndex = 1

        keystoneV2Filters foreach { filter =>
          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_KEYSTONEV2_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").nonEmpty) {
            println(s"keystone-v2 filter (#$filterIndex) IS filtered by uri-regex")
          } else {
            println(s"keystone-v2 filter (#$filterIndex) IS NOT filtered by uri-regex")
          }

          config match {
            case Some(configRoot) =>
              val tenanted = (configRoot \ "tenant-handling" \ "validate-tenant").nonEmpty
              if (tenanted) {
                println(s"keystone-v2 filter (#$filterIndex) IS IN tenanted mode")
              } else {
                println(s"keystone-v2 filter (#$filterIndex) IS NOT IN tenanted mode")
              }

              (configRoot \ "pre-authorized-roles" \ "role").find(node => node.text.equals("foyer")) match {
                case Some(_) => println(s"keystone-v2 filter (#$filterIndex) HAS pre-authorized role foyer")
                case None => println(s"keystone-v2 filter (#$filterIndex) DOES NOT HAVE pre-authorized role foyer")
              }

              if ((configRoot \ "require-service-endpoint").nonEmpty) {
                println(s"keystone-v2 filter (#$filterIndex) HAS service catalog authorization enabled")
              } else {
                println(s"keystone-v2 filter (#$filterIndex) DOES NOT HAVE service catalog authorization enabled")
              }
            case None =>
              println(s"keystone-v2 (#$filterIndex) configuration NOT FOUND")
          }

          filterIndex += 1
        }
      }
    }

    def checkIdentityV3(filters: NodeSeq): Unit = {
      val identityV3Filters = getFiltersNamed(IDENTITYV3_FILTER_NAME)(filters)

      if (identityV3Filters.isEmpty) {
        println("identity-v3 filter IS NOT listed in the system-model")
      } else {
        var filterIndex = 1

        identityV3Filters foreach { filter =>
          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_IDENTITYV3_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").nonEmpty) {
            println(s"identity-v3 filter (#$filterIndex) IS filtered by uri-regex")
          } else {
            println(s"identity-v3 filter (#$filterIndex) IS NOT filtered by uri-regex")
          }

          config match {
            case Some(configRoot) =>
              val tenanted = (configRoot \ "validate-project-id-in-uri").nonEmpty
              if (tenanted) {
                println(s"identity-v3 filter (#$filterIndex) IS IN tenanted mode")
              } else {
                println(s"identity-v3 filter (#$filterIndex) IS NOT IN tenanted mode")
              }

              (configRoot \ "roles-which-bypass-project-id-check" \ "role").find(node => node.text.equals("foyer")) match {
                case Some(_) => println(s"identity-v3 filter (#$filterIndex) HAS tenant bypass role foyer")
                case None => println(s"identity-v3 filter (#$filterIndex) DOES NOT HAVE tenant bypass role foyer")
              }

              if ((configRoot \ "service-endpoint").nonEmpty) {
                println(s"identity-v3 filter (#$filterIndex) HAS service catalog authorization enabled")
              } else {
                println(s"identity-v3 filter (#$filterIndex) DOES NOT HAVE service catalog authorization enabled")
              }
            case None =>
              println(s"identity-v3 (#$filterIndex) configuration NOT FOUND")
          }

          filterIndex += 1
        }
      }
    }

    val clusters = getFile(SYSTEM_MODEL_FILENAME).map(getClusters).get
    clusters foreach { cluster =>
      println(s"Verifying cluster: ${cluster \ "@id"}")

      val filters = cluster \ "filters" \ "filter"
      checkAuthN(filters)
      checkAuthZ(filters)
      checkKeystoneV2(filters)
      checkIdentityV3(filters)

      println()
    }

    // TODO: Output JSON (defer printing until here)
  }
}
