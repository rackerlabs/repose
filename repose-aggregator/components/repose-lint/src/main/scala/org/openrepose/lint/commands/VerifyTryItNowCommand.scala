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
import play.api.libs.json._

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

  override def perform(lintConfig: LintConfig): Unit = {
    val reposeVersion = lintConfig.reposeVersion

    def versionGreaterThanOrEqualTo(otherVersion: String): Boolean = {
      val reposeVersionSplit = reposeVersion.split('.').map(_.toInt)
      val otherVersionSplit = otherVersion.split('.').map(_.toInt)

      if (reposeVersionSplit.sameElements(otherVersionSplit)) {
        true
      } else {
        if (reposeVersionSplit.length < otherVersionSplit.length) {
          reposeVersionSplit.indices.exists(i => reposeVersionSplit(i) > otherVersionSplit(i))
        } else {
          otherVersionSplit.indices.exists(i => otherVersionSplit(i) > reposeVersionSplit(i))
        }
      }
    }

    def versionLessThan(otherVersion: String): Boolean = !versionGreaterThanOrEqualTo(otherVersion)

    def getFile(filename: String): Option[File] = {
      val file = new File(lintConfig.configDir, filename)

      if (file.exists && file.isFile && file.canRead) {
        Some(file)
      } else {
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
    def checkAuthN(filters: NodeSeq): JsValue = {
      case class AuthNFilterCheck(filteredByUriRegex: Boolean = true,
                                  missingConfiguration: Boolean = true,
                                  inTenantedMode: Boolean = false,
                                  foyerAsServiceAdmin: Boolean = false,
                                  foyerAsIgnoreTenant: Boolean = false,
                                  foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.NotReady)

      case class AuthNCheck(listedInSystemModel: Boolean = false,
                            authNFilterChecks: Seq[AuthNFilterCheck] = Seq.empty,
                            foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.NotReady)

      implicit val AuthNFilterCheckWrites = new Writes[AuthNFilterCheck] {
        override def writes(anfc: AuthNFilterCheck): JsValue = Json.obj(
          "filteredByUriRegex" -> anfc.filteredByUriRegex,
          "missingConfiguration" -> anfc.missingConfiguration,
          "inTenantedMode" -> anfc.inTenantedMode,
          "foyerAsServiceAdmin" -> anfc.foyerAsServiceAdmin,
          "foyerAsIgnoreTenant" -> anfc.foyerAsIgnoreTenant,
          "foyerStatus" -> anfc.foyerStatus.toString
        )
      }

      implicit val AuthNCheckWrites = new Writes[AuthNCheck] {
        override def writes(anc: AuthNCheck): JsValue = Json.obj(
          "filterName" -> AUTH_N_FILTER_NAME,
          "filters" -> anc.authNFilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r),
          "foyerStatus" -> anc.foyerStatus.toString
        )
      }

      var check = AuthNCheck()
      val authNFilters = getFiltersNamed(AUTH_N_FILTER_NAME)(filters)

      if (authNFilters.nonEmpty) {
        val authNFilterChecks = authNFilters map { filter =>
          var filterCheck = AuthNFilterCheck()

          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_AUTH_N_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").isEmpty) {
            filterCheck = filterCheck.copy(filteredByUriRegex = false)
          }

          config foreach { configRoot =>
            filterCheck = filterCheck.copy(missingConfiguration = false)

            val tenanted = (configRoot \ "openstack-auth" \ "@tenanted").map(node => node.text.toBoolean).headOption.getOrElse(true)
            if (tenanted) {
              filterCheck = filterCheck.copy(inTenantedMode = true)
            }

            val isFoyerServiceAdmin = (configRoot \ "openstack-auth" \ "service-admin-roles" \ "role").exists(node => node.text.equals("foyer"))
            if (isFoyerServiceAdmin) {
              filterCheck = filterCheck.copy(foyerAsServiceAdmin = true)
            }

            val isFoyerIgnoreTenant = (configRoot \ "openstack-auth" \ "ignore-tenant-roles" \ "role").exists(node => node.text.equals("foyer"))
            if (isFoyerIgnoreTenant) {
              filterCheck = filterCheck.copy(foyerAsIgnoreTenant = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Unknown)
          } else if (filterCheck.missingConfiguration || !filterCheck.inTenantedMode) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotReady)
          } else if (versionLessThan("4.1.0") && filterCheck.foyerAsServiceAdmin) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Ready)
          } else if (versionGreaterThanOrEqualTo("4.1.0") && versionLessThan("7.1.4.0") && filterCheck.foyerAsServiceAdmin && filterCheck.foyerAsIgnoreTenant) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Ready)
          } else if (filterCheck.foyerAsServiceAdmin || filterCheck.foyerAsIgnoreTenant) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Ready)
          }

          filterCheck
        }

        val checkFoyerStatus = if (authNFilterChecks.exists(ifc => FoyerStatus.Ready.equals(ifc.foyerStatus))) {
          FoyerStatus.Ready
        } else if (authNFilterChecks.exists(ifc => FoyerStatus.Unknown.equals(ifc.foyerStatus))) {
          FoyerStatus.Unknown
        } else {
          FoyerStatus.NotReady
        }

        check = check.copy(authNFilterChecks = authNFilterChecks, foyerStatus = checkFoyerStatus)
      }

      Json.toJson(check)
    }

    def checkAuthZ(filters: NodeSeq): JsValue = {
      case class AuthZFilterCheck(filteredByUriRegex: Boolean = true,
                                  missingConfiguration: Boolean = true,
                                  foyerAsIgnoreTenant: Boolean = false,
                                  foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.NotReady)

      case class AuthZCheck(listedInSystemModel: Boolean = false,
                            authZFilterChecks: Seq[AuthZFilterCheck] = Seq.empty,
                            foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.NotReady)

      implicit val AuthZFilterCheckWrites = new Writes[AuthZFilterCheck] {
        override def writes(azfc: AuthZFilterCheck): JsValue = Json.obj(
          "filteredByUriRegex" -> azfc.filteredByUriRegex,
          "missingConfiguration" -> azfc.missingConfiguration,
          "foyerAsIgnoreTenant" -> azfc.foyerAsIgnoreTenant,
          "foyerStatus" -> azfc.foyerStatus.toString
        )
      }

      implicit val AuthZCheckWrites = new Writes[AuthZCheck] {
        override def writes(azc: AuthZCheck): JsValue = Json.obj(
          "filterName" -> AUTH_Z_FILTER_NAME,
          "filters" -> azc.authZFilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r),
          "foyerStatus" -> azc.foyerStatus.toString
        )
      }

      var check = AuthZCheck()
      val authZFilters = getFiltersNamed(AUTH_Z_FILTER_NAME)(filters)

      if (authZFilters.nonEmpty) {
        val authZFilterChecks = authZFilters map { filter =>
          var filterCheck = AuthZFilterCheck()

          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_AUTH_Z_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").isEmpty) {
            filterCheck = filterCheck.copy(filteredByUriRegex = false)
          }

          config foreach { configRoot =>
            val ignoreTenantRoles = (configRoot \ "ignore-tenant-roles" \ "role") ++ (configRoot \ "ignore-tenant-roles" \ "ignore-tenant-role")
            if (ignoreTenantRoles.exists(node => node.text.equals("foyer"))) {
              filterCheck = filterCheck.copy(foyerAsIgnoreTenant = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Unknown)
          } else if (filterCheck.missingConfiguration) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotReady)
          } else if (versionLessThan("4.1.0")) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Ready)
          } else if (!filterCheck.foyerAsIgnoreTenant) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Ready)
          }

          filterCheck
        }

        val checkFoyerStatus = if (authZFilterChecks.exists(ifc => FoyerStatus.Ready.equals(ifc.foyerStatus))) {
          FoyerStatus.Ready
        } else if (authZFilterChecks.exists(ifc => FoyerStatus.Unknown.equals(ifc.foyerStatus))) {
          FoyerStatus.Unknown
        } else {
          FoyerStatus.NotReady
        }

        check = check.copy(authZFilterChecks = authZFilterChecks, foyerStatus = checkFoyerStatus)
      }

      Json.toJson(check)
    }

    def checkKeystoneV2(filters: NodeSeq): JsValue = {
      case class KeystoneV2FilterCheck(filteredByUriRegex: Boolean = true,
                                       missingConfiguration: Boolean = true,
                                       inTenantedMode: Boolean = false,
                                       foyerAsPreAuth: Boolean = false,
                                       catalogAuthorization: Boolean = false,
                                       foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.NotReady)

      case class KeystoneV2Check(listedInSystemModel: Boolean = false,
                                 keystoneV2FilterChecks: Seq[KeystoneV2FilterCheck] = Seq.empty,
                                 foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.NotReady)

      implicit val KeystoneV2FilterCheckWrites = new Writes[KeystoneV2FilterCheck] {
        override def writes(kfc: KeystoneV2FilterCheck): JsValue = Json.obj(
          "filteredByUriRegex" -> kfc.filteredByUriRegex,
          "missingConfiguration" -> kfc.missingConfiguration,
          "inTenantedMode" -> kfc.inTenantedMode,
          "foyerAsPreAuthorized" -> kfc.foyerAsPreAuth,
          "catalogAuthorization" -> kfc.catalogAuthorization,
          "foyerStatus" -> kfc.foyerStatus.toString
        )
      }

      implicit val KeystoneV2CheckWrites = new Writes[KeystoneV2Check] {
        override def writes(kc: KeystoneV2Check): JsValue = Json.obj(
          "filterName" -> AUTH_Z_FILTER_NAME,
          "filters" -> kc.keystoneV2FilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r),
          "foyerStatus" -> kc.foyerStatus.toString
        )
      }

      var check = KeystoneV2Check()
      val keystoneV2Filters = getFiltersNamed(KEYSTONEV2_FILTER_NAME)(filters)

      if (keystoneV2Filters.nonEmpty) {
        val keystoneV2FilterChecks = keystoneV2Filters map { filter =>
          var filterCheck = KeystoneV2FilterCheck()

          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_KEYSTONEV2_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").isEmpty) {
            filterCheck = filterCheck.copy(filteredByUriRegex = false)
          }

          config foreach { configRoot =>
            if ((configRoot \ "tenant-handling" \ "validate-tenant").nonEmpty) {
              filterCheck = filterCheck.copy(inTenantedMode = true)
            }

            if ((configRoot \ "pre-authorized-roles" \ "role").exists(node => node.text.equals("foyer"))) {
              filterCheck = filterCheck.copy(foyerAsPreAuth = true)
            }

            if ((configRoot \ "require-service-endpoint").nonEmpty) {
              filterCheck = filterCheck.copy(catalogAuthorization = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Unknown)
          } else if (filterCheck.missingConfiguration || !filterCheck.inTenantedMode) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotReady)
          } else if (versionGreaterThanOrEqualTo("7.1.5.1") && filterCheck.foyerAsPreAuth && filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Ready)
          }

          filterCheck
        }

        val checkFoyerStatus = if (keystoneV2FilterChecks.exists(ifc => FoyerStatus.Ready.equals(ifc.foyerStatus))) {
          FoyerStatus.Ready
        } else if (keystoneV2FilterChecks.exists(ifc => FoyerStatus.Unknown.equals(ifc.foyerStatus))) {
          FoyerStatus.Unknown
        } else {
          FoyerStatus.NotReady
        }

        check = check.copy(keystoneV2FilterChecks = keystoneV2FilterChecks, foyerStatus = checkFoyerStatus)
      }

      Json.toJson(check)
    }

    def checkIdentityV3(filters: NodeSeq): JsValue = {
      case class IdentityV3FilterCheck(filteredByUriRegex: Boolean = true,
                                       missingConfiguration: Boolean = true,
                                       inTenantedMode: Boolean = false,
                                       foyerAsBypassTenant: Boolean = false,
                                       catalogAuthorization: Boolean = false,
                                       foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.NotReady)

      case class IdentityV3Check(listedInSystemModel: Boolean = false,
                                 identityV3FilterChecks: Seq[IdentityV3FilterCheck] = Seq.empty,
                                 foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.NotReady)

      implicit val IdentityV3FilterCheckWrites = new Writes[IdentityV3FilterCheck] {
        override def writes(ifc: IdentityV3FilterCheck): JsValue = Json.obj(
          "filteredByUriRegex" -> ifc.filteredByUriRegex,
          "missingConfiguration" -> ifc.missingConfiguration,
          "inTenantedMode" -> ifc.inTenantedMode,
          "foyerAsBypassTenant" -> ifc.foyerAsBypassTenant,
          "catalogAuthorization" -> ifc.catalogAuthorization,
          "foyerStatus" -> ifc.foyerStatus.toString
        )
      }

      implicit val IdentityV3CheckWrites = new Writes[IdentityV3Check] {
        override def writes(ic: IdentityV3Check): JsValue = Json.obj(
          "filterName" -> AUTH_Z_FILTER_NAME,
          "filters" -> ic.identityV3FilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r),
          "foyerStatus" -> ic.foyerStatus.toString
        )
      }

      var check = IdentityV3Check()
      val identityV3Filters = getFiltersNamed(IDENTITYV3_FILTER_NAME)(filters)

      if (identityV3Filters.nonEmpty) {
        val identityV3FilterChecks = identityV3Filters map { filter =>
          var filterCheck = IdentityV3FilterCheck()

          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_IDENTITYV3_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").isEmpty) {
            filterCheck = filterCheck.copy(filteredByUriRegex = false)
          }

          config foreach { configRoot =>
            if ((configRoot \ "validate-project-id-in-uri").nonEmpty) {
              filterCheck = filterCheck.copy(inTenantedMode = true)
            }

            if ((configRoot \ "roles-which-bypass-project-id-check" \ "role").exists(node => node.text.equals("foyer"))) {
              filterCheck = filterCheck.copy(foyerAsBypassTenant = true)
            }

            if ((configRoot \ "service-endpoint").nonEmpty) {
              filterCheck = filterCheck.copy(catalogAuthorization = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Unknown)
          } else if (filterCheck.missingConfiguration || !filterCheck.inTenantedMode) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotReady)
          } else if (versionGreaterThanOrEqualTo("7.0.0.0") && filterCheck.foyerAsBypassTenant && filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Ready)
          }

          filterCheck
        }

        val checkFoyerStatus = if (identityV3FilterChecks.exists(ifc => FoyerStatus.Ready.equals(ifc.foyerStatus))) {
          FoyerStatus.Ready
        } else if (identityV3FilterChecks.exists(ifc => FoyerStatus.Unknown.equals(ifc.foyerStatus))) {
          FoyerStatus.Unknown
        } else {
          FoyerStatus.NotReady
        }

        check = check.copy(identityV3FilterChecks = identityV3FilterChecks, foyerStatus = checkFoyerStatus)
      }

      Json.toJson(check)
    }

    val clusters = getFile(SYSTEM_MODEL_FILENAME).map(getClusters) match {
      case Some(c) => c
      case None => throw new Exception("System model configuration file not found")
    }

    val clusterJsonObjects = clusters map { cluster =>
      val filters = cluster \ "filters" \ "filter"

      Json.obj("clusterId" -> (cluster \ "@id").head.text,
        "authNCheck" -> checkAuthN(filters),
        "authZCheck" -> checkAuthZ(filters),
        "keystoneV2Check" -> checkKeystoneV2(filters),
        "identityV3Check" -> checkIdentityV3(filters)
      )
    }
    val clustersArray = clusterJsonObjects.foldLeft(Json.arr())((arr, obj) => arr :+ obj)

    println(Json.prettyPrint(Json.obj("clusters" -> clustersArray)))
  }

  object FoyerStatus extends Enumeration {
    type FoyerStatus = Value
    val Ready, NotReady, Unknown = Value
  }

}
