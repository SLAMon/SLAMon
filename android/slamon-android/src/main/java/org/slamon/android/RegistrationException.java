package org.slamon.android;

/**
 * An exception thrown in push notification registration process
 */
public class RegistrationException extends Exception {
    public RegistrationException(String message) {
        super(message);
    }
}
