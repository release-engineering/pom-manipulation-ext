package org.commonjava.maven.ext.manip.rest.exception;

/**
 * @author vdedik@redhat.com
 */
public class ClientException extends RestException {
    public ClientException(String msg) {
        super(msg);
    }
}
