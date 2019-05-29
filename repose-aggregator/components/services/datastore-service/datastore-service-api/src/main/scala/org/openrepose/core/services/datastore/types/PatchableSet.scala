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

import scala.collection.mutable

/**
  * Created by adrian on 1/21/16.
  */
class PatchableSet[A] extends mutable.HashSet[A]
  with Patchable[PatchableSet[A], SetPatch[A]] {
  override def applyPatch(patch: SetPatch[A]): PatchableSet[A] = {
    val returnedSet = PatchableSet(this.toList: _*)
    this.add(patch.patchValue)
    returnedSet.add(patch.patchValue)
    returnedSet
  }
}

case class SetPatch[A](patchValue: A) extends Patch[PatchableSet[A]] {
  override def newFromPatch(): PatchableSet[A] = PatchableSet(patchValue)
}

object PatchableSet {
  def apply[A](xs: A*): PatchableSet[A] = new PatchableSet[A] ++= xs

  def empty[A]: PatchableSet[A] = new PatchableSet[A]
}
