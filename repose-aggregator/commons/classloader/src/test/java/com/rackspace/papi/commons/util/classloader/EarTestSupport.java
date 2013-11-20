package com.rackspace.papi.commons.util.classloader;

import com.rackspace.papi.commons.util.classloader.ear.DefaultEarArchiveEntryHelper;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.jar.test.EmptyClass;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.fail;

public abstract class EarTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(EarTestSupport.class);
    public static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    private String earName;
    private File deploymentDestination;

    public EarTestSupport(String deploymentDirectoryName) {
        this.earName = UUID.randomUUID().toString() + ".ear";
        this.deploymentDestination = createDeploymentDestination(deploymentDirectoryName);
    }

    public String getEarName() {
        return earName;
    }

    public File getDeploymentDestination() {
        return deploymentDestination;
    }

    @Before
    public final void standUp() throws Exception {
        if (!TMP_DIR.canRead() || !TMP_DIR.canWrite()) {
            throw new RuntimeException("Temp dir is not readable or writable. Please check your permissions.");
        }

        final byte[] embeddedJar = buildEmbeddedJar();

        final File ear = new File(TMP_DIR, getEarName());

        final JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(ear));
        packageResource(embeddedJar, jarOutputStream, new JarEntry("lib/embedded.jar"));

        final byte[] testXml = getResource("/ear-contents/test.xml"),
                testTxt = getResource("/ear-contents/test.txt"),
                manifest = getResource("/ear-contents/MANIFEST.MF"),
                testProperties = getResource("/ear-contents/test.properties");

        packageResource(testXml, "/META-INF/test.xml", jarOutputStream);
        packageResource(testTxt, "META-INF/test.txt", jarOutputStream);
        packageResource(manifest, "META-INF/MANIFEST.MF", jarOutputStream);
        packageResource(testProperties, "META-INF/config/test.properties", jarOutputStream);

        jarOutputStream.close();

        onSetup();
    }
    protected abstract void onSetup();

    private byte[] getResource(String resourcePath) throws IOException {
        return EarClassLoader.createOutPutStream(getClass().getResourceAsStream(resourcePath)).toByteArray();
    }

    private static void packageResource(byte[] resourceBytes, String jarResourceName, JarOutputStream jarOutputStream) throws IOException {
        packageResource(resourceBytes, jarOutputStream, new JarEntry(jarResourceName));
    }

    private static void packageResource(byte[] resourceBytes, JarOutputStream jarOutputStream, JarEntry entry) throws IOException {
        entry.setSize(resourceBytes.length);
        entry.setTime(System.currentTimeMillis());

        jarOutputStream.putNextEntry(entry);
        jarOutputStream.write(resourceBytes);
        jarOutputStream.closeEntry();
    }

    private byte[] buildEmbeddedJar() throws IOException {
        final String emptyClassResourceName = EmptyClass.class.getName().replaceAll("\\.", "/") + ".class";

        final ByteArrayOutputStream jarByteArray = new ByteArrayOutputStream();
        final JarOutputStream jos = new JarOutputStream(jarByteArray);

        packageResource(getResource("/" + emptyClassResourceName), emptyClassResourceName, jos);
        jos.close();

        return jarByteArray.toByteArray();
    }

    @After
    public void cleanUp() {
        final File earFile = new File(TMP_DIR, getEarName());

        if (!earFile.delete()) {
            String message = "Failed to delete EAR file! [" + TMP_DIR + ", " + getEarName();

            LOG.warn(message);
            throw new RuntimeException(message);
        }
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        LOG.warn("failed to delete test directory");
                    }
                }
            }
        }

        return path.delete();
    }

    private static File createDeploymentDestination(String target) {
        final File deploymentDestination = new File(TMP_DIR, target);

        if (!deploymentDestination.exists() && !deploymentDestination.mkdirs()) {
            fail("failed to make directory: {" + TMP_DIR + ",  " + target + "}");
        }

        return deploymentDestination;
    }

    protected static DefaultEarArchiveEntryHelper createEarArchiveEntryListener(File deploymentDirectory) {
        return new DefaultEarArchiveEntryHelper(deploymentDirectory);
    }

    protected File createEarFile() {
        return new File(TMP_DIR, getEarName());
    }
}
