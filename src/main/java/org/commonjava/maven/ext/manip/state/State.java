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
package org.commonjava.maven.ext.manip.state;

import org.commonjava.maven.ext.manip.impl.Manipulator;

/**
 * Basic list of methods that state collections related to different {@link Manipulator}'s should implement. This is also a marker interface to 
 * help ensure the validity of content stored in the session.
 * 
 * A State implementation can contain a mixture of configuration (parsed/configured from command-line properties or other sources) and state output
 * from the {@link Manipulator#scan(java.util.List, ManipulationSession)} invocation.
 * 
 * @author jdcasey
 */
public interface State
{
    /**
     * Return true if this State is enabled.
     */
    public abstract boolean isEnabled();
}
