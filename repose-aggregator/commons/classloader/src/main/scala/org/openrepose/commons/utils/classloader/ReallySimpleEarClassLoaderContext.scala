package org.openrepose.commons.utils.classloader

import org.openrepose.commons.utils.classloader.ear.{EarDescriptor, EarClassLoaderContext}

case class ReallySimpleEarClassLoaderContext(earDescriptor: EarDescriptor, classLoader: ClassLoader) extends EarClassLoaderContext {
  override def getEarDescriptor: EarDescriptor = earDescriptor

  override def getClassLoader: ClassLoader = classLoader
}
