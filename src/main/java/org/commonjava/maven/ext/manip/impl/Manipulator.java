package org.commonjava.maven.ext.manip.impl;

import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.VersioningState;

/**
 * Represents one way that a POM may be manipulated/modified during pre-processing. State is kept separately for each {@link Manipulator}
 * (see {@link VersioningState}, associated with the {@link ProjectVersioningManipulator} implementation of this interface). State is stored in the
 * {@link ManipulationSession} instance. State consists of both configuration (normally detected from the user properties, or -D options on the command
 * line), and also changes detected in the scan() method invocation that will be applied later.
 * 
 * @author jdcasey
 */
public interface Manipulator
{

    /**
     * Initialize any state for the manipulator
     */
    void init( ManipulationSession session )
        throws ManipulationException;

    void scan( final List<MavenProject> projects, ManipulationSession session )
        throws ManipulationException;

    boolean applyChanges( Model model, ManipulationSession session )
        throws ManipulationException;

    void applyChanges( List<MavenProject> projects, ManipulationSession session )
        throws ManipulationException;

}