/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package test;

import org.junit.Test;

public class HelloWorldwithJUnit
{
    public static void main (String [] args)
    {
        System.out.println("hello");
    }

    @Test
    public void test()
    {
        // Just a dummy method to verify that we can compile again JUnit 4
    }
}
