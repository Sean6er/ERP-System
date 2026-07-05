package com.example.demo.exception;

/** Wird geworfen wenn das SAP-Belegprinzip verletzt wird (z.B. Löschversuch). */
public class BelegprinzipException extends RuntimeException {
    public BelegprinzipException(String message) { super(message); }
}
