package org.commonjava.maven.ext.rest.exception;

/**
 * @author vdedik@redhat.com
 */
public class ServerException extends RestException {
    public ServerException(String msg) {
        super(msg);
    }
}
