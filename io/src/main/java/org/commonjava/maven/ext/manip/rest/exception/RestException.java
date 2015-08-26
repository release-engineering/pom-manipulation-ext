package org.commonjava.maven.ext.manip.rest.exception;

/**
 * @author vdedik@redhat.com
 */
public class RestException extends RuntimeException {
    public RestException(String msg) {
        super(msg);
    }
}
