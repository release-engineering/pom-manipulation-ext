package org.commonjava.maven.ext.manip;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.junit.Test;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;

import static org.junit.Assert.*;

public class CliIntegrationTest {

    @Test
    public void testSimpleNumeric() throws IOException, InterruptedException, ResourceException, ScriptException {
        String buildDir = System.getProperty("buildDirectory");
        File ncwd = new File(buildDir + "/it-cli/simple-numeric");
        File testPropsFile = new File(buildDir + "/it-cli/simple-numeric/test.properties");
        File localRepo = new File(buildDir + "/local-repo");

        Properties testProps = new Properties();
        FileInputStream fis = new FileInputStream(testPropsFile);
        testProps.load(fis);
        String params = "";
        for (Object rawKey : testProps.keySet()) {
            String key = (String) rawKey;
            params += String.format("-D%s=%s ", key, testProps.getProperty(key));
        }

        String command = String.format("java -jar %s/pom-manipulation-cli.jar %s", buildDir, params);
        runCommandAndWait(command, ncwd);

        String commandMaven = String.format("mvn install -Dmaven.repo.local=%s", localRepo);
        runCommandAndWait(commandMaven, ncwd);

        Binding binding = new Binding();
        binding.setVariable("basedir", ncwd.toString());
        GroovyScriptEngine engine = new GroovyScriptEngine(ncwd.toString());
        engine.run("verify.groovy", binding);
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static String runCommandAndWait(String command, File workingDir) throws IOException, InterruptedException {
        Process proc = Runtime.getRuntime().exec(command, null, workingDir);
        BufferedReader brInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader brError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        String line = null;
        StringBuilder stdOutput = new StringBuilder();
        while ((line = brInput.readLine()) != null) {
            stdOutput.append(line + "\n");
        }
        StringBuilder errOutput = new StringBuilder();
        while ((line = brError.readLine()) != null) {
            errOutput.append(line + "\n");
        }

        if (proc.waitFor() != 0) {
            throw new IllegalStateException(
                    String.format("Process exited with an error, standard output:\n%s\nerror output:\n %s",
                            stdOutput, errOutput));
        }

        return stdOutput.toString();
    }
    
}