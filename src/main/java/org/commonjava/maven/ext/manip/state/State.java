package org.commonjava.maven.ext.manip.state;

public interface State
{
    /**
     * Return true if this State is enabled.
     */
    public abstract boolean isEnabled();
}