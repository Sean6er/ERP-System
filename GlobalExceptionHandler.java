package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> handleIllegalArg(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    @ExceptionHandler(BelegprinzipException.class)
    public ResponseEntity<Map<String,Object>> handleBelegprinzip(BelegprinzipException ex) {
        return error(HttpStatus.CONFLICT, "Belegprinzip-Verletzung: " + ex.getMessage());
    }
    @ExceptionHandler(SollHabenException.class)
    public ResponseEntity<Map<String,Object>> handleSollHaben(SollHabenException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "Soll/Haben-Fehler: " + ex.getMessage());
    }
    /** ÄNDERUNG: neuer Handler für die Kreditlimit-Prüfung des Kreditkontrollbereichs. */
    @ExceptionHandler(KreditlimitUeberschrittenException.class)
    public ResponseEntity<Map<String,Object>> handleKreditlimit(KreditlimitUeberschrittenException ex) {
        return error(HttpStatus.CONFLICT, "Kreditlimit-Verletzung: " + ex.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Validierungsfehler: " + msg);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleGeneral(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Fehler: " + ex.getMessage());
    }
    private ResponseEntity<Map<String,Object>> error(HttpStatus status, String msg) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("zeitstempel", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("fehler", status.getReasonPhrase());
        body.put("meldung", msg);
        return ResponseEntity.status(status).body(body);
    }
}
