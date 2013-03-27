package com.rackspace.papi.commons.util.classloader;

import java.util.Map;
import java.util.TreeMap;

public class ResourceIdentityTree implements ResourceRegistry, Cloneable {

   private final Map<String, ResourceDescriptor> classPathIdentityTree;

   public ResourceIdentityTree() {
      classPathIdentityTree = new TreeMap<String, ResourceDescriptor>();
   }

   public ResourceIdentityTree(ResourceIdentityTree classPathIdentityTreeToCopy) {
      classPathIdentityTree = new TreeMap<String, ResourceDescriptor>(classPathIdentityTreeToCopy.classPathIdentityTree);
   }

   @Override
   public void register(ResourceDescriptor resource) {
      classPathIdentityTree.put(resource.archiveEntry().fullName(), resource);
   }

   @Override
   public boolean hasResourceDescriptorRegistered(ResourceDescriptor resource) {
      return classPathIdentityTree.containsKey(resource.archiveEntry().fullName());
   }

   @Override
   public ResourceDescriptor getDescriptorForResource(String classPath) {
      return classPathIdentityTree.get(classPath);
   }

   @Override
   public boolean hasMatchingIdentity(ResourceDescriptor resource) {
      final ResourceDescriptor internalDescriptor = classPathIdentityTree.get(resource.archiveEntry().fullName());

      boolean matches = false;

      if (internalDescriptor != null) {
         final byte[] internalIdentityDigest = internalDescriptor.digestBytes(),
                 externalIdentityDigest = resource.digestBytes();

         if (internalIdentityDigest.length == externalIdentityDigest.length) {
            for (int i = 0; i < internalIdentityDigest.length; i++) {
               matches = internalIdentityDigest[i] == externalIdentityDigest[i];

               if (!matches) {
                  break;
               }
            }
         }
      }

      return matches;
   }

   @SuppressWarnings({"PMD.ProperCloneImplementation","PMD.CloneMethodMustImplementCloneable","CloneDoesntCallSuperClone"}) 
   @Override
   public Object clone() throws CloneNotSupportedException {
      return new ResourceIdentityTree(this);
   }
}
