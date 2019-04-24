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
package com.atlassian.oai.validator.report

import com.atlassian.oai.validator.report.ValidationReport.{Level, Message}

/**
  * Builds a new [[ValidationReport]], bypassing access control.
  *
  * TODO: Usage of this object should be replaced with `ValidationReport.from`
  * TODO: once Scala compilation supports static methods on interfaces (i.e.,
  * TODO: targeting Java 1.8).
  */
object ValidationReportScala {
  def from(messages: Message*): ValidationReport = {
    Option(messages)
      .map(new ImmutableValidationReport(_: _*))
      .getOrElse(empty)
  }

  def singleton(message: Message): ValidationReport = {
    Option(message)
      .map(new ImmutableValidationReport(_))
      .getOrElse(empty)
  }

  def empty: ValidationReport = {
    new EmptyValidationReport()
  }

  object Message {
    def create(key: String, message: String): Message = {
      new ImmutableMessage(key, Level.ERROR, message, Seq.empty[String]: _*)
    }
  }

}
