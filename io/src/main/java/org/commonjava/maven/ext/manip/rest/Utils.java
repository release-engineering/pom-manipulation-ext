package org.commonjava.maven.ext.manip.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.Scanner;

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

    public static ProjectVersionRef fromString(String gav) {
        String[] projectGavSplit = gav.split(":");
        ProjectVersionRef result =
                new ProjectVersionRef(projectGavSplit[0], projectGavSplit[1], projectGavSplit[2]);
        return result;
    }
}
