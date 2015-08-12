/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.commonjava.maven.ext.manip;

import com.google.common.io.ByteStreams;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.io.*;
import java.util.*;

/**
 * @author vdedik@redhat.com
 */
public class CliTestUtils {
    public static final String BUILD_DIR = System.getProperty("buildDirectory");
    public static final String IT_LOCATION = BUILD_DIR + "/it-cli";
    public static final String LOCAL_REPO = BUILD_DIR + "/local-repo";
    public static final Map<String, String> DEFAULT_MVN_PARAMS = new HashMap<String, String>() {{
        put("maven.repo.local", LOCAL_REPO);
    }};

    /**
     * Run pom-manipulation-cli.jar with java params (-D arguments) in workingDir directory.
     * Using test.properties in workingDir as -D arguments.
     *
     * @param workingDir - Working directory in which you want the cli to be run.
     * @return Exit value
     * @throws Exception
     */
    public static Integer runCli(String workingDir) throws Exception{
        return runCli(new ArrayList<String>(), workingDir);
    }

    /**
     * Run pom-manipulation-cli.jar with java params (-D arguments) in workingDir directory.
     * Using test.properties in workingDir as -D arguments.
     *
     * @param args - List of additional command line arguments
     * @param workingDir - Working directory in which you want the cli to be run.
     * @return Exit value
     * @throws Exception
     */
    public static Integer runCli(List<String> args, String workingDir) throws Exception{
        Properties testProperties = loadTestProps(workingDir);
        return runCli(args, testProperties, workingDir);
    }

    /**
     * Run pom-manipulation-cli.jar with java params (-D arguments) in workingDir directory.
     *
     * @param properties - Properties representing -D arguments
     * @param workingDir - Working directory in which you want the cli to be run.
     * @return Exit value
     * @throws Exception
     */
    public static Integer runCli(Properties properties, String workingDir) throws Exception {
        return runCli(null, properties, workingDir);
    }

    /**
     * Run pom-manipulation-cli.jar with java params (-D arguments) in workingDir directory.
     *
     * @param args - List of additional command line arguments
     * @param properties - Properties representing -D arguments
     * @param workingDir - Working directory in which you want the cli to be run.
     * @return Exit value
     * @throws Exception
     */
    public static Integer runCli(List<String> args, Properties properties, String workingDir) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        for (final String name: properties.stringPropertyNames()) {
            params.put(name, properties.getProperty(name));
        }

        return runCli(args, params, workingDir);
    }

    /**
     * Run pom-manipulation-cli.jar with java params (-D arguments) in workingDir directory.
     *
     * @param params - Map of String keys and String values representing -D arguments
     * @param workingDir - Working directory in which you want the cli to be run.
     * @return Exit value
     * @throws Exception
     */
    public static Integer runCli(Map<String, String> params, String workingDir) throws Exception {
        return runCli(null, params, workingDir);
    }

    /**
     * Run pom-manipulation-cli.jar with java params (-D arguments) in workingDir directory.
     *
     * @param args - List of additional command line arguments
     * @param params - Map of String keys and String values representing -D arguments
     * @param workingDir - Working directory in which you want the cli to be run.
     * @return Exit value
     * @throws Exception
     */
    public static Integer runCli(List<String> args, Map<String, String> params, String workingDir)
            throws Exception {
        String stringArgs = toArguments(args);
        String stringParams = toJavaParams(params);
        String command = String.format(
                "java -jar %s/pom-manipulation-cli.jar %s %s", BUILD_DIR, stringParams, stringArgs);

        return runCommandAndWait(command, workingDir, true);
    }

    /**
     * Run maven process with commands in workingDir directory.
     *
     * @param commands - String representing maven command(s), e.g. "clean install".
     * @param workingDir - Working directory.
     * @return Exit value
     * @throws Exception
     */
    public static Integer runMaven(String commands, String workingDir) throws Exception {
        return runMaven(commands, null, workingDir);
    }

    /**
     * Run maven process with commands and params (-D arguments) in workingDir directory.
     *
     * @param commands - String representing maven command(s), e.g. "clean install".
     * @param params - Map of String keys and values representing -D arguments.
     * @param workingDir - Working directory.
     * @return Exit value
     * @throws Exception
     */
    public static Integer runMaven(String commands, Map<String, String> params, String workingDir) throws Exception {
        String stringParams = toJavaParams(params);
        String commandMaven = String.format("mvn %s %s ", commands, stringParams);

        return runCommandAndWait(commandMaven, workingDir);
    }

    /**
     * Run verify.groovy script in workingDir directory.
     *
     * @param workingDir - Directory with verify.groovy script.
     * @throws Exception
     */
    public static void verify(String workingDir) throws Exception{
        Binding binding = new Binding();
        binding.setVariable("basedir", workingDir);
        GroovyScriptEngine engine = new GroovyScriptEngine(workingDir);
        engine.run("verify.groovy", binding);
    }

    /**
     * Load test.properties from workingDir directory.
     *
     * @param workingDir - Directory that contains test.properties.
     * @return Loaded properties
     * @throws Exception
     */
    public static Properties loadTestProps(String workingDir) throws Exception {
        File testPropsFile = new File(workingDir + "/test.properties");
        Properties testProps = new Properties();
        FileInputStream fis = new FileInputStream(testPropsFile);
        testProps.load(fis);

        return testProps;
    }

    /**
     * Get default location of integration test by test name.
     *
     * @param test - Test name.
     * @return Default location of integration test, e.g. ~/pom-manipulation-ext/integration-test/target/it-cli/it-test
     */
    public static String getDefaultTestLocation(String test) {
        return String.format("%s/%s", IT_LOCATION, test);
    }

    /**
     * Convert string parameters in a Map to a String of -D arguments
     *
     * @param params - Map of java parameters
     * @return - String of -D arguments
     */
    public static String toJavaParams(Map<String, String> params) {
        if (params == null) {
            return "";
        }

        String stringParams = "";
        for (String key : params.keySet()) {
            stringParams += String.format("-D%s=%s ", key, params.get(key));
        }
        return stringParams;
    }

    /**
     * Convert string arguments in a List to a String
     *
     * @param args - List of command line options with its arguments
     * @return - String of options with it's arguments
     */
    public static String toArguments(List<String> args) {
        if (args == null) {
            return "";
        }

        String stringArgs = "";
        for (String arg : args) {
            stringArgs += String.format("%s ", arg);
        }
        return stringArgs;
    }

    /**
     * Run command in another process and wait for it to finish.
     *
     * @param command - Command to be run in another process, e.g. "mvn clean install"
     * @param workingDir - Working directory in which to run the command.
     * @return exit value.
     * @throws Exception
     */
    public static Integer runCommandAndWait(String command, String workingDir) throws Exception {
        return runCommandAndWait(command, workingDir, false);
    }

    /**
     * Run command in another process and wait for it to finish. Throw IOException if command exits with non zero
     * exit value.
     *
     * @param command - Command to be run in another process, e.g. "mvn clean install"
     * @param workingDir - Working directory in which to run the command.
     * @param ignoreFailure - Weather or not to ignore non zero exit value, if false, IOException will be thrown when
     *                        exit value is not 0.
     * @return exit value.
     * @throws Exception
     */
    public static Integer runCommandAndWait(String command, String workingDir, Boolean ignoreFailure) throws Exception {
        Process proc = Runtime.getRuntime().exec(command, null, new File(workingDir));
        File buildlog = new File(workingDir + "/build.log");
        File builderrlog = new File(workingDir + "/builderr.log");

        InputStream stdout = new BufferedInputStream(proc.getInputStream());
        FileOutputStream stdoutFile = new FileOutputStream(buildlog);
        ByteStreams.copy(stdout, stdoutFile);

        InputStream stderr = new BufferedInputStream(proc.getErrorStream());
        FileOutputStream stderrFile = new FileOutputStream(builderrlog);
        ByteStreams.copy(stderr, stderrFile);

        stdout.close();
        stderr.close();
        stdoutFile.close();
        stderrFile.close();

        if (!ignoreFailure && proc.waitFor() != 0) {
            throw new IOException(
                    String.format("Process exited with an error, see files %s and %s", buildlog, builderrlog));
        }

        return proc.exitValue();
    }
}
