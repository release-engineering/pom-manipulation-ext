package org.commonjava.maven.ext.rest.exception;

/**
 * @author vdedik@redhat.com
 */
public class RestException extends RuntimeException {
    public RestException(String msg) {
        super(msg);
    }
}
