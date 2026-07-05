package com.example.demo.exception;

/** Wird geworfen wenn Soll und Haben nicht ausgeglichen sind. */
public class SollHabenException extends RuntimeException {
    public SollHabenException(String message) { super(message); }
}
