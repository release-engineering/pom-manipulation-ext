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

// Test we can compile against JUnit 3.

import junit.swingui.TestRunner;

public class HelloWorldwithJUnit
{
    public static void main (String [] args)
    {
        System.out.println("hello");
        new TestRunner();
    }
}
