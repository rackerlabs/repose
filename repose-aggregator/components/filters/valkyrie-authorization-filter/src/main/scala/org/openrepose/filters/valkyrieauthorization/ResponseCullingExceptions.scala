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
package org.openrepose.filters.valkyrieauthorization

sealed trait ResponseCullingException extends Exception

case class UnexpectedJsonException(message: String, throwable: Throwable = null)
  extends Exception(message, throwable) with ResponseCullingException

case class TransformException(message: String, throwable: Throwable = null)
  extends Exception(message, throwable) with ResponseCullingException

case class MalformedJsonPathException(message: String, throwable: Throwable = null)
  extends Exception(message, throwable) with ResponseCullingException

case class InvalidJsonPathException(message: String, throwable: Throwable = null)
  extends Exception(message, throwable) with ResponseCullingException

case class InvalidJsonTypeException(message: String, throwable: Throwable = null)
  extends Exception(message, throwable) with ResponseCullingException

case class MalformedRegexException(message: String, throwable: Throwable = null)
  extends Exception(message, throwable) with ResponseCullingException

case class InvalidCaptureGroupException(message: String, throwable: Throwable = null)
  extends Exception(message, throwable) with ResponseCullingException

case class NonMatchingRegexException(message: String, throwable: Throwable = null)
  extends Exception(message, throwable) with ResponseCullingException
