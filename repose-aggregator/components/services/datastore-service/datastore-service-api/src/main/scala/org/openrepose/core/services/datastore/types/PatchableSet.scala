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
package org.openrepose.core.services.datastore.types

import org.openrepose.core.services.datastore.{Patch, Patchable}

import scala.collection.immutable

/**
  * Created by adrian on 1/21/16.
  */
class PatchableSet[A](xs: A*) extends immutable.Set[A]
  with Patchable[PatchableSet[A], SetPatch[A]] {

  private final val set = immutable.Set(xs: _*)

  override def contains(elem: A): Boolean = set.contains(elem)

  override def +(elem: A): Set[A] = set + elem

  override def -(elem: A): Set[A] = set - elem

  override def iterator: Iterator[A] = set.iterator

  override def applyPatch(patch: SetPatch[A]): PatchableSet[A] = {
    new PatchableSet((set + patch.patchValue).toSeq: _*)
  }
}

case class SetPatch[A](patchValue: A) extends Patch[PatchableSet[A]] {
  override def newFromPatch(): PatchableSet[A] = new PatchableSet(patchValue)
}
