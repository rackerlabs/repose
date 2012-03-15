package com.rackspace.papi.commons.util.classloader;

public interface ResourceRegistry extends Cloneable {

   /**
    *
    * @param classPath Class path definition should be delimited by '/' instead of '.'
    * @return ResourceDescriptor
    */
   ResourceDescriptor getDescriptorForResource(String classPath);

   boolean hasMatchingIdentity(ResourceDescriptor resource);

   boolean hasResourceDescriptorRegistered(ResourceDescriptor resource);

   void register(ResourceDescriptor resource);
}
