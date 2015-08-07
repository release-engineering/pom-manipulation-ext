package org.commonjava.maven.ext.rest;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * @author vdedik@redhat.com
 */
public class Utils {
    public static String readFileFromClasspath(String filename) {
        StringBuilder fileContents = new StringBuilder();
        Scanner scanner = new Scanner(Utils.class.getResourceAsStream(filename));
        String lineSeparator = System.getProperty("line.separator");

        try {
            while(scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }

    public static Pattern getProjectMatcher(String gav) {
        return Pattern.compile(".*\"project\":\\w*\"" + gav + "\".*");
    }
}
