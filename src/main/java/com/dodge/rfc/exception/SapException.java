package com.dodge.rfc.exception;

public class SapException extends RuntimeException {
    private final String sapCode;

    public SapException(String message, String sapCode) {
        super(message);
        this.sapCode = sapCode;
    }

    public String getSapCode() { return sapCode; }
}
