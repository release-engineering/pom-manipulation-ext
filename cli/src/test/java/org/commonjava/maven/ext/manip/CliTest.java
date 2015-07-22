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

import ch.qos.logback.classic.Level;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

public class CliTest
{
    @Before
    public void before()
    {
        final ch.qos.logback.classic.Logger root =
                        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );
        root.setLevel( Level.OFF);
    }

    @Test
    public void checkTargetMatches () throws Exception
    {
        Cli c = new Cli();
        File pom1 = new File("/tmp/foobar");

        executeMethod( c, "createSession", new Object[] { pom1 } );

        ManipulationSession session = (ManipulationSession) getField( c, "session" );

        assertTrue( "Session file should match", pom1.equals( session.getPom() ));
	}

    @Test
    public void checkTargetMatchesWithRun () throws Exception
    {
        Cli c = new Cli();
        File pom1 = new File("/tmp/foobar");

		executeMethod( c, "run", new Object[] { new String[] { "-f", pom1.toString() } } );

        ManipulationSession session = (ManipulationSession) getField( c, "session" );

        assertTrue( "Session file should match", pom1.equals( session.getPom() ));
    }


    @Test
	public void checkTargetDefaultMatches () throws Exception
	{
		Cli c = new Cli();

		executeMethod( c, "run", new Object[] { new String[] {} } );

		ManipulationSession session = (ManipulationSession) getField( c, "session" );
		File defaultTarget = (File) getField( c, "target" );

		assertTrue( "Session file should match", defaultTarget.equals( session.getPom() ) );
	}

	/**
	 * Gets the field value from an instance.  The field we wish to retrieve is
	 * specified by passing the name.  The value will be returned, even if the
	 * field would have private or protected access.
	 */
	private Object getField( Object instance, String name ) throws Exception
	{
		Class c = instance.getClass();

		// Retrieve the field with the specified name
		Field f = c.getDeclaredField( name );

		f.setAccessible( true );

		// Return the value of the field for the instance
		return f.get( instance );
	}

	/**
	 * Executes a method on an object instance.  The name and parameters of
	 * the method are specified.  The method will be executed and the value
	 * of it returned, even if the method would have private or protected access.
	 */
	private Object executeMethod( Object instance, String name, Object[] params ) throws Exception
	{
		Class c = instance.getClass();

		// Fetch the Class types of all method parameters
		Class[] types = new Class[params.length];

		for ( int i = 0; i < params.length; i++ )
			types[i] = params[i].getClass();

		Method m = c.getDeclaredMethod( name, types );
		m.setAccessible( true );

		return m.invoke( instance, params );
	}
}