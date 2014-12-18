package org.openrepose.commons.utils.classloader

case class ReallySimpleEarClassLoaderContext(earDescriptor: EarDescriptor, classLoader: ClassLoader) extends EarClassLoaderContext {
  override def getEarDescriptor: EarDescriptor = earDescriptor

  override def getClassLoader: ClassLoader = classLoader
}
