package org.commonjava.maven.ext.manip.rest.exception;

/**
 * @author vdedik@redhat.com
 */
public class ServerException extends RestException {
    public ServerException(String msg) {
        super(msg);
    }
}
