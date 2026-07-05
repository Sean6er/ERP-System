package com.example.demo.dto;
import jakarta.validation.constraints.NotBlank;
/** Request für NLP-Buchungsassistent */
public class NlpBuchungsRequest {
    @NotBlank(message = "Freitext darf nicht leer sein")
    private String freitext;
    private String erfasstVon = "NLP-Assistent";
    private boolean direktBuchen = false;
    public String getFreitext() { return freitext; }
    public void setFreitext(String freitext) { this.freitext = freitext; }
    public String getErfasstVon() { return erfasstVon; }
    public void setErfasstVon(String erfasstVon) { this.erfasstVon = erfasstVon; }
    public boolean isDirektBuchen() { return direktBuchen; }
    public void setDirektBuchen(boolean direktBuchen) { this.direktBuchen = direktBuchen; }
}