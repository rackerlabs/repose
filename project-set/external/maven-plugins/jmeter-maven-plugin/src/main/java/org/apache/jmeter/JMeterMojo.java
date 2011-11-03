package org.apache.jmeter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.TransformerException;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DirectoryScanner;

/**
 * JMeter Maven plugin.
 *
 * @author Tim McCune
 * @goal jmeter
 * @requiresProject true
 */
public class JMeterMojo extends AbstractMojo {

    /**
     * Path to a Jmeter test XML file.
     * Relative to srcDir.
     * May be declared instead of the parameter includes.
     *
     * @parameter
     */
    private File jmeterTestFile;
    /**
     * Sets the list of include patterns to use in directory scan for JMeter Test XML files.
     * Relative to srcDir.
     * May be declared instead of a single jmeterTestFile.
     * Ignored if parameter jmeterTestFile is given.
     *
     * @parameter
     */
    private List<String> includes;
    /**
     * Sets the list of exclude patterns to use in directory scan for Test files.
     * Relative to srcDir.
     * Ignored if parameter jmeterTestFile file is given.
     *
     * @parameter
     */
    private List<String> excludes;
    /**
     * Path under which JMeter test XML files are stored.
     *
     * @parameter expression="${jmeter.testfiles.basedir}"
     *          default-value="${basedir}/src/test/jmeter"
     */
    private File srcDir;
    /**
     * Directory in which the reports are stored.
     *
     * @parameter expression="${jmeter.reports.dir}"
     *          default-value="${basedir}/target/jmeter-report"
     */
    private File reportDir;
    /**
     * Whether or not to generate reports after measurement.
     *
     * @parameter default-value="true"
     */
    private boolean enableReports;
    /**
     * Custom Xslt which is used to create the report.
     *
     * @parameter
     */
    private File reportXslt;
    /**
     * Absolute path to JMeter default properties file.
     * The default properties file is part of a JMeter installation and sets basic properties needed for running JMeter.
     *
     * @parameter expression="${jmeter.properties}"
     *          default-value="${basedir}/src/test/jmeter/jmeter.properties"
     * @required
     */
    private File jmeterDefaultPropertiesFile;
    /**
     * Absolute path to JMeter custom (test dependent) properties file.
     *
     * @parameter
     */
    private File jmeterCustomPropertiesFile;
    /**
     * JMeter Properties that override those given in jmeterProps
     *
     * @parameter
     */
    @SuppressWarnings("rawtypes")
    private Map jmeterUserProperties;
    /**
     * Use remote JMeter installation to run tests
     *
     * @parameter default-value=false
     */
    private boolean remote;
    /**
     * Sets whether ErrorScanner should ignore failures in JMeter result file.
     *
     * @parameter expression="${jmeter.ignore.failure}" default-value=false
     */
    private boolean jmeterIgnoreFailure;
    /**
     * Sets whether ErrorScanner should ignore errors in JMeter result file.
     *
     * @parameter expression="${jmeter.ignore.error}" default-value=false
     */
    private boolean jmeterIgnoreError;
    /**
     * @parameter expression="${project}"
     * @required
     */
    @SuppressWarnings("unused")
    private MavenProject mavenProject;
    /**
     * @parameter expression="${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     */
    private ArtifactResolver artifactResolver;
    /**
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;
    /**
     * HTTP proxy host name.
     * @parameter
     */
    private String proxyHost;
    /**
     * HTTP proxy port.
     * @parameter expression="80"
     */
    private Integer proxyPort;
    /**
     * HTTP proxy username.
     * @parameter
     */
    private String proxyUsername;
    /**
     * HTTP proxy user password.
     * @parameter
     */
    private String proxyPassword;
    /**
     * Postfix to add to report file.
     *
     * @parameter default-value="-report.html"
     */
    private String reportPostfix;
    /**
     * Sets whether the test execution shall preserve the order of patterns in include clauses.
     *
     * @parameter expression="${jmeter.preserve.includeOrder}" default-value=false
     */
    private String jmeterhome;
    /**
     * Sets the Jmeter home directory. Allows changing of different jmeter version
     * 
     * @parameter
     */
    private String dateFormat;
    /**
     * Sets whether or not to append a date format to the end of the result file.
     * 
     * @parameter date format string. default is no string eg: test-file.jmx -> test-file.html
     * /
     */
    private boolean jmeterPreserveIncludeOrder;
    private File workDir;
    private File jmeterLog;
    private static final String JMETER_ARTIFACT_GROUPID = "org.apache.jmeter";

    /**
     * Run all JMeter tests.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        initSystemProps();

        List<String> jmeterTestFiles = new ArrayList<String>();
        List<String> results = new ArrayList<String>();
        if (jmeterTestFile != null) {
            jmeterTestFiles.add(jmeterTestFile.getName());
        } else {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(srcDir);
            scanner.setIncludes(includes == null ? new String[]{"**/*.jmx"} : includes.toArray(new String[]{}));
            if (excludes != null) {
                scanner.setExcludes(excludes.toArray(new String[]{}));
            }
            scanner.scan();
            final List<String> includedFiles = Arrays.asList(scanner.getIncludedFiles());
            if (jmeterPreserveIncludeOrder) {
                Collections.sort(includedFiles, new IncludesComparator(includes));
            }
            jmeterTestFiles.addAll(includedFiles);
        }

        for (String file : jmeterTestFiles) {
            results.add(executeTest(new File(srcDir, file)));
        }
        if (this.enableReports) {
            makeReport(results);
        }
        checkForErrors(results);
    }

    private void makeReport(List<String> results) throws MojoExecutionException {
        try {
            ReportTransformer transformer;
            transformer = new ReportTransformer(getXslt());
            getLog().info("Building JMeter Report.");
            for (String resultFile : results) {
                final String outputFile = toOutputFileName(resultFile);
                getLog().info("transforming: " + resultFile + " to " + outputFile);
                transformer.transform(resultFile, outputFile);
            }
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Error writing report file jmeter file.", e);
        } catch (TransformerException e) {
            throw new MojoExecutionException("Error transforming jmeter results", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying resources to jmeter results", e);
        }
    }

    /**
     * returns the fileName with the configured reportPostfix
     *
     * @param fileName the String to modify
     *
     * @return modified fileName
     */
    private String toOutputFileName(String fileName) {
        if (fileName.endsWith(".xml")) {
            return fileName.replace(".xml", this.reportPostfix);
        } else {
            return fileName + this.reportPostfix;
        }
    }

    private InputStream getXslt() throws IOException {
        if (this.reportXslt == null) {
            //if we are using the default report, also copy the images out.

            // TODO:Reimplement - Reimplement this without IOUtils
//            IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("reports/collapse.jpg"), new FileOutputStream(this.reportDir.getPath() + File.separator + "collapse.jpg"));
//            IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("reports/expand.jpg"), new FileOutputStream(this.reportDir.getPath() + File.separator + "expand.jpg"));
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("reports/jmeter-results-detail-report_21.xsl");
        } else {
            return new FileInputStream(this.reportXslt);
        }
    }

    /**
     * Scan JMeter result files for "error" and "failure" messages
     *
     * @param results List of JMeter result files.
     *
     * @throws MojoExecutionException exception
     * @throws MojoFailureException exception
     */
    private void checkForErrors(List<String> results) throws MojoExecutionException, MojoFailureException {
        ErrorScanner scanner = new ErrorScanner(this.jmeterIgnoreError, this.jmeterIgnoreFailure);
        try {
            for (String file : results) {
                if (scanner.scanForProblems(new File(file))) {
                    getLog().warn("There were test errors.  See the jmeter logs for details");
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't read log file", e);
        }
    }

    /**
     * Initialize System Properties needed for JMeter run.
     *
     * @throws MojoExecutionException exception
     */
    private void initSystemProps() throws MojoExecutionException {
        workDir = new File("target" + File.separator + "jmeter");
        workDir.mkdirs();
        createTemporaryProperties();

        jmeterLog = new File(workDir, "jmeter.log");
        try {
            System.setProperty("log_file", jmeterLog.getCanonicalPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Can't get canonical path for log file", e);
        }
    }

    /**
     * Create temporary property files and set necessary System Properties.
     *
     * This mess is necessary because JMeter must load this info from a file.
     * Loading resources from classpath won't work.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          Exception
     */
    @SuppressWarnings("unchecked")
    private void createTemporaryProperties() throws MojoExecutionException {
        List<File> temporaryPropertyFiles = new ArrayList<File>();

        String jmeterTargetDir = File.separator + "target" + File.separator + "jmeter" + File.separator;
        File saveServiceProps = new File(workDir, "saveservice.properties");
        System.setProperty("saveservice_properties", jmeterTargetDir + saveServiceProps.getName());
        temporaryPropertyFiles.add(saveServiceProps);
        File upgradeProps = new File(workDir, "upgrade.properties");
        System.setProperty("upgrade_properties", jmeterTargetDir + upgradeProps.getName());
        temporaryPropertyFiles.add(upgradeProps);

        for (File propertyFile : temporaryPropertyFiles) {
            try {
                final OutputStream out = new FileOutputStream(propertyFile);
                final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyFile.getName());
                int value = -1;

                while ((value = in.read()) != -1) {
                    out.write(value);
                }

                out.close();
                in.close();
            } catch (IOException e) {
                throw new MojoExecutionException("Could not create temporary property file " + propertyFile.getName() + " in directory " + jmeterTargetDir, e);
            }
        }
    }

    /**
     * Executes a single JMeter test by building up a list of command line
     * parameters to pass to Jmeter
     * 
     * Re-implementation to not use the Security Manager to halt Jmeter from exiting.
     * 
     * @param test JMeter test JMX
     * @return the report file names.
     * @throws MojoExecutionException 
     */
    private String executeTest(File test) throws MojoExecutionException {

        Runtime rt = Runtime.getRuntime();
        String resultFileName;
        if (dateFormat != null) {
            getLog().info("Date Format chosen: " + dateFormat);
            DateFormat formt = new SimpleDateFormat(dateFormat);
            resultFileName = reportDir.toString() + File.separator + test.getName().substring(0, test.getName().lastIndexOf(".")) + "-" + formt.format(new Date()) + ".xml";
        } else {
            resultFileName = reportDir.toString() + File.separator + test.getName().substring(0, test.getName().lastIndexOf(".")) + ".xml";
        }
        //delete file if it already exists
        new File(resultFileName).delete();
        ArrayList<String> commandlist = new ArrayList<String>();
        String jmeterCmd = jmeterhome + "/bin/jmeter", arg1 = "-n", arg2 = "-t", arg3 = test.getAbsolutePath();
        String arg4 = "-l", arg5 = resultFileName, arg6 = "-j", arg7 = jmeterLog.getAbsolutePath();

        commandlist.add(jmeterCmd);
        commandlist.add(arg1);
        commandlist.add(arg2);
        commandlist.add(arg3);
        commandlist.add(arg4);
        commandlist.add(arg5);
        commandlist.add(arg6);
        commandlist.add(arg7);


        if (jmeterCustomPropertiesFile != null) {
            commandlist.add("-q");
            commandlist.add(jmeterCustomPropertiesFile.toString());
        }
        if (remote) {
            commandlist.add("-r");
        }

        if (proxyHost != null && !proxyHost.equals("")) {
            commandlist.add("-H");
            commandlist.add(proxyHost);
            commandlist.add("-P");
            commandlist.add(proxyPort.toString());
            getLog().info("Setting HTTP proxy to " + proxyHost + ":" + proxyPort);
        }

        if (proxyUsername != null && !proxyUsername.equals("")) {

            commandlist.add("-u");
            commandlist.add(proxyUsername);
            commandlist.add("-a");
            commandlist.add(proxyPassword);
            getLog().info("Logging with " + proxyUsername + ":" + proxyPassword);

        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("JMeter is called with the following command line arguments: " + commandlist.toString());
        }



        String[] cmd = commandlist.toArray(new String[commandlist.size()]);
        try {
            Process jm = rt.exec(cmd);
            jm.waitFor();
            getLog().info(String.valueOf(jm.exitValue()));


            writeProcessOutput(jm);

        } catch (IOException io) {
            getLog().info("could not execute! " + io.getMessage());
        } catch (Exception e) {
            getLog().info("Exception occured with command" + cmd.toString() + "with message" + e.getMessage());
        }

        return resultFileName;

    }

    static void writeProcessOutput(Process process) throws Exception {
        InputStreamReader tempReader = new InputStreamReader(
                new BufferedInputStream(process.getInputStream()));
        BufferedReader reader = new BufferedReader(tempReader);
        while (true) {
            String line = reader.readLine();

            if (line == null) {
                break;
            }
            System.out.println(line);
        }
    }

    private boolean checkForEndOfTest(BufferedReader in) throws MojoExecutionException {
        boolean testEnded = false;
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("Test has ended")) {
                    testEnded = true;
                    break;
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't read log file", e);
        }
        return testEnded;
    }

    /**
     * Translates map of jmeterUserProperties to List of JMeter compatible commandline flags.
     *
     * @return List of JMeter compatible commandline flags
     */
    @SuppressWarnings("unchecked")
    private ArrayList<String> getUserProperties() {
        ArrayList<String> propsList = new ArrayList<String>();
        if (jmeterUserProperties == null) {
            return propsList;
        }
        Set<String> keySet = (Set<String>) jmeterUserProperties.keySet();

        for (String key : keySet) {

            propsList.add("-J");
            propsList.add(key + "=" + jmeterUserProperties.get(key));
        }

        return propsList;
    }

    private static class ExitException extends SecurityException {

        private static final long serialVersionUID = 5544099211927987521L;
        public int _rc;

        public ExitException(int rc) {
            super(Integer.toString(rc));
            _rc = rc;
        }

        public int getCode() {
            return _rc;
        }
    }
}
