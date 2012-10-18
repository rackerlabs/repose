package com.rackspace.papi.commons.util.classloader.ear;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.classloader.ResourceDescriptor;
import com.rackspace.papi.commons.util.classloader.ResourceIdentityTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class EarClassLoader extends ClassLoader {

   private static final Logger LOG = LoggerFactory.getLogger(EarClassLoader.class);
   private static final int BYTE_BUFFER_SIZE = 1024;
   private final ResourceIdentityTree classPathIdentityTree;
   private final File unpackedArchiveRoot;
   private final ClassLoader parent;

   public EarClassLoader(File unpackedArchiveRoot) {
      this(getSystemClassLoader().getParent(), unpackedArchiveRoot);
   }

   public EarClassLoader(ClassLoader parent, File unpackedArchiveRoot) {
      super(parent);

      this.parent = parent;
      this.unpackedArchiveRoot = unpackedArchiveRoot;
      classPathIdentityTree = new ResourceIdentityTree();
   }

   public boolean register(ResourceDescriptor resourceDescriptor) {
      if (!classPathIdentityTree.hasResourceDescriptorRegistered(resourceDescriptor)) {
         classPathIdentityTree.register(resourceDescriptor);

         return true;
      }

      return classPathIdentityTree.hasMatchingIdentity(resourceDescriptor);
   }

   @Override
   public Class<?> loadClass(String name) throws ClassNotFoundException {
      return loadClass(name, false);
   }

   @Override
   protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class c = findLoadedClass(name);

      if (c == null) {
         try {
            if (parent != null) {
               c = parent.loadClass(name);
            } else {
               c = findSystemClass(name);
            }
         } catch (ClassNotFoundException e) {
            // ClassNotFoundException thrown if class not found
            // from the non-null parent class loader
            c = findClass(name);
         }

         if (c == null) {
            // If still not found throw an exception
            throw new ClassNotFoundException(name);
         }
      }

      if (resolve) {
         resolveClass(c);
      }

      return c;
   }

   @Override
   protected Class<?> findClass(String classPath) throws ClassNotFoundException {
      final String resourcePath = classPath.replaceAll("\\.", "/") + ".class";
      final ResourceDescriptor descriptor = classPathIdentityTree.getDescriptorForResource(resourcePath);

      if (descriptor != null) {
         try {
            return defineClass(descriptor);
         } catch (IOException ioe) {
            LOG.error("Failed to resolve registered class: " + classPath, ioe);
         }
      }

      return null;
   }

   //findResource("/META-INF/stuff")
   @Override
   protected URL findResource(String resourcePath) {
      URL resourceUrl = super.findResource(resourcePath);

      if (resourceUrl == null) {
         final ResourceDescriptor descriptor = classPathIdentityTree.getDescriptorForResource(resourcePath);

         if (descriptor != null) {
            resourceUrl = descriptorToUrl(descriptor);
         } else if (parent instanceof EarClassLoader) {
            LOG.debug("Unable to find resource: " + resourcePath);
         }
      }

      return resourceUrl;
   }

   final Class<?> defineClass(ResourceDescriptor descriptor) throws IOException {
      final URL resourceUrl = descriptorToUrl(descriptor);

      if (resourceUrl == null) {
         return null;
      }

      final ByteArrayOutputStream out = createOutputStream(resourceUrl);

      final String packageName = StringUtilities.trim(descriptor.archiveEntry().getPrefix(), "/").replaceAll("/", ".");

      if (getPackage(packageName) == null) {
         definePackage(packageName, null, null, null, null, null, null, null);
      }

      final byte[] classBytes = out.toByteArray();

      return defineClass(packageName + "." + descriptor.archiveEntry().getSimpleName(), classBytes, 0, classBytes.length);
   }

   private ByteArrayOutputStream createOutputStream(URL resourceUrl) throws IOException {
      final InputStream resourceInputStream = resourceUrl.openStream();

      return createOutPutStream(resourceInputStream);
   }

   public static ByteArrayOutputStream createOutPutStream(InputStream resourceInputStream) throws IOException {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      final byte[] byteBuffer = new byte[BYTE_BUFFER_SIZE];
      int read;

      while ((read = resourceInputStream.read(byteBuffer)) != -1) {
         byteArrayOutputStream.write(byteBuffer, 0, read);
      }

      resourceInputStream.close();
      return byteArrayOutputStream;
   }

   private URL descriptorToUrl(ResourceDescriptor descriptor) {
      try {
         return new URL(!descriptor.archiveEntry().isRootArchiveEntry()
                 ? buildJarResourceUrl(descriptor)
                 : buildFileResourceUrl(descriptor));

      } catch (MalformedURLException murle) {
         return null;
      }
   }

   private String buildFileResourceUrl(ResourceDescriptor descriptor) {
      final StringBuilder urlBuffer = new StringBuilder();
      urlBuffer.append("file:").append(unpackedArchiveRoot.getAbsolutePath()).append('/').append(descriptor.archiveEntry().fullName());

      return urlBuffer.toString();
   }

   private String buildJarResourceUrl(ResourceDescriptor descriptor) {
      final StringBuilder urlBuffer = new StringBuilder();
      urlBuffer.append("jar:file:").append(unpackedArchiveRoot.getAbsolutePath()).append('/').append(descriptor.archiveEntry().getArchiveName()).append("!/").append(descriptor.archiveEntry().fullName());

      return urlBuffer.toString();
   }
}
