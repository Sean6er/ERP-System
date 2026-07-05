package com.example.demo.exception;

/** Wird geworfen, wenn eine Faktura das Kreditlimit des Kunden-Kreditkontrollbereichs überschreiten würde. */
public class KreditlimitUeberschrittenException extends RuntimeException {
    public KreditlimitUeberschrittenException(String message) { super(message); }
}
