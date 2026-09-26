package com.revisionassistant.service;

public class JsonImportException extends Exception {
    public JsonImportException(String message) { super(message); }
    public JsonImportException(String message, Throwable cause) { super(message, cause); }
}
