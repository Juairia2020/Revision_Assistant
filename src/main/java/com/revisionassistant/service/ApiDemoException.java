package com.revisionassistant.service;

/** User-friendly exception for failures in the public API demonstration. */
public class ApiDemoException extends Exception {
    public ApiDemoException(String message) {
        super(message);
    }

    public ApiDemoException(String message, Throwable cause) {
        super(message, cause);
    }
}
