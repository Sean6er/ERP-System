package com.example.demo.service;

import com.example.demo.entity.Konto;
import com.example.demo.repository.KontoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClaudeNlpService – ruft die Anthropic Claude API auf, um aus einem
 * deutschen Freitext einen Buchungsvorschlag (Soll/Haben) zu erzeugen.
 *
 * Wichtig: Claude bekommt den TATSÄCHLICHEN Kontenplan aus der Datenbank
 * als Kontext mitgeteilt und wird per "Structured Outputs" (JSON-Schema)
 * gezwungen, ausschließlich in einem festen, parsbaren Format zu antworten.
 * Damit erfindet das Modell keine Kontonummern und die Antwort ist immer
 * valides JSON – kein fragiles String-Parsing nötig.
 *
 * Konfiguration (application.properties):
 *   anthropic.api.key=${ANTHROPIC_API_KEY:}
 *   anthropic.api.model=claude-sonnet-5
 *   anthropic.api.base-url=https://api.anthropic.com/v1/messages
 *
 * WICHTIG: Den API-Key niemals hart codieren oder ins Repo committen.
 * Immer über Umgebungsvariable ANTHROPIC_API_KEY setzen.
 */
@Service
public class ClaudeNlpService {

    @Value("${anthropic.api.key:}")
    private String apiKey;

    @Value("${anthropic.api.model:claude-sonnet-5}")
    private String model;

    @Value("${anthropic.api.base-url:https://api.anthropic.com/v1/messages}")
    private String baseUrl;

    @Value("${anthropic.api.version:2023-06-01}")
    private String apiVersion;

    private final KontoRepository kontoRepository;
    private final HttpClient httpClient;
    private final JsonMapper mapper;

    // JsonMapper wird von Spring Boot 4 (Jackson 3) automatisch als Bean bereitgestellt.
    public ClaudeNlpService(KontoRepository kontoRepository, JsonMapper mapper) {
        this.kontoRepository = kontoRepository;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Prüft, ob ein API-Key konfiguriert ist (für Fallback-Entscheidung im aufrufenden Service). */
    public boolean isVerfuegbar() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Analysiert Freitext mittels Claude API und liefert einen Buchungsvorschlag
     * im selben Map-Format wie der bisherige regelbasierte Parser, damit
     * NlpController/Frontend unverändert bleiben können.
     */
    public Map<String, Object> analysiere(String freitext) {
        if (!isVerfuegbar()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY ist nicht konfiguriert");
        }

        List<Konto> konten = kontoRepository.findAllSorted();
        String kontenplanText = konten.isEmpty()
                ? "(Kontenplan ist leer)"
                : konten.stream()
                    .map(k -> k.getKontonummer() + " | " + k.getKontobezeichnung() + " | " + k.getKontotyp())
                    .collect(Collectors.joining("\n"));

        String systemPrompt = """
                Du bist ein Buchhaltungsassistent für ein SAP-inspiriertes ERP-System \
                (Global Bike GmbH, Buchungskreis DE00, Kontenplan GL00).
                Deine einzige Aufgabe: Wandle einen deutschen Freitext-Geschäftsvorfall \
                in einen korrekten Soll/Haben-Buchungssatz um.

                Regeln der doppelten Buchführung:
                - AKTIV- und AUFWAND-Konten werden im SOLL erhöht.
                - PASSIV- und ERTRAG-Konten werden im HABEN erhöht.
                - Wähle IMMER ein existierendes Konto aus der folgenden Liste \
                (Format: Kontonummer | Bezeichnung | Typ). Erfinde NIEMALS eine \
                Kontonummer, die nicht in der Liste steht.
                - Wenn kein passendes Konto existiert oder der Freitext keinen \
                eindeutigen Geschäftsvorfall beschreibt, setze erfolg=false und \
                erkläre den Grund knapp im Feld "fehler".
                - Erkenne den Betrag (Dezimaltrennzeichen kann Komma oder Punkt sein) \
                und die Währung (Standard: EUR).
                - Wenn kein Datum im Text genannt wird, verwende das heutige Datum: %s

                Verfügbarer Kontenplan (GL00):
                %s
                """.formatted(LocalDate.now(), kontenplanText);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", "Freitext: " + freitext
        )));

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("schema", buildSchema());
        Map<String, Object> outputConfig = new LinkedHashMap<>();
        outputConfig.put("format", format);
        body.put("output_config", outputConfig);

        try {
            String jsonBody = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", apiVersion)
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Claude API Fehler (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode contentArray = root.path("content");
            if (!contentArray.isArray() || contentArray.isEmpty()) {
                throw new IllegalStateException("Unerwartete Claude-API-Antwort ohne content: " + response.body());
            }
            String text = contentArray.get(0).path("text").asText();
            JsonNode result = mapper.readTree(text);

            return zuVorschlag(freitext, result);

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Fehler bei der Claude-NLP-Analyse: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> zuVorschlag(String freitext, JsonNode result) {
        Map<String, Object> vorschlag = new LinkedHashMap<>();
        vorschlag.put("originalText", freitext);
        vorschlag.put("analysiert", LocalDate.now().toString());
        vorschlag.put("quelle", "claude-api:" + model);

        boolean erfolg = result.path("erfolg").asBoolean(false);
        vorschlag.put("erfolg", erfolg);

        if (!erfolg) {
            vorschlag.put("fehler", result.path("fehler").asText("Freitext konnte nicht eindeutig zugeordnet werden."));
            return vorschlag;
        }

        BigDecimal betrag = new BigDecimal(result.path("betrag").asText("0"));
        if (betrag.compareTo(BigDecimal.ZERO) <= 0) {
            vorschlag.put("erfolg", false);
            vorschlag.put("fehler", "Kein gültiger Betrag erkannt.");
            return vorschlag;
        }

        vorschlag.put("betragErkannt", betrag);
        vorschlag.put("betrag", betrag);
        vorschlag.put("waehrung", result.path("waehrung").asText("EUR"));
        vorschlag.put("sollKontoNr", result.path("sollKontoNr").asInt());
        vorschlag.put("sollKontoBezeichnung", result.path("sollKontoBezeichnung").asText());
        vorschlag.put("habenKontoNr", result.path("habenKontoNr").asInt());
        vorschlag.put("habenKontoBezeichnung", result.path("habenKontoBezeichnung").asText());
        vorschlag.put("buchungstext",
                result.path("buchungstext").asText(freitext));
        vorschlag.put("buchungsdatum",
                result.path("buchungsdatum").asText(LocalDate.now().toString()));
        vorschlag.put("konfidenz", result.path("konfidenz").asDouble(1.0));
        vorschlag.put("erfolg", true);
        vorschlag.put("hinweis", "KI-Vorschlag (Claude) prüfen und mit POST /api/buchungen/buchen bestätigen.");

        return vorschlag;
    }

    /** JSON-Schema, das die Claude-Antwort strikt erzwingt (Structured Outputs). */
    private Map<String, Object> buildSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("erfolg", Map.of(
                "type", "boolean",
                "description", "true, wenn ein eindeutiger Buchungssatz ermittelt werden konnte"));
        properties.put("fehler", Map.of(
                "type", "string",
                "description", "Fehlerbeschreibung falls erfolg=false, sonst leerer String"));
        properties.put("betrag", Map.of(
                "type", "string",
                "description", "Erkannter Betrag als Dezimalzahl mit Punkt als Trennzeichen, z.B. '1500.00'"));
        properties.put("waehrung", Map.of("type", "string", "description", "Währungscode, z.B. EUR"));
        properties.put("sollKontoNr", Map.of(
                "type", "integer",
                "description", "Kontonummer des Sollkontos – MUSS aus dem übergebenen Kontenplan stammen"));
        properties.put("sollKontoBezeichnung", Map.of("type", "string"));
        properties.put("habenKontoNr", Map.of(
                "type", "integer",
                "description", "Kontonummer des Habenkontos – MUSS aus dem übergebenen Kontenplan stammen"));
        properties.put("habenKontoBezeichnung", Map.of("type", "string"));
        properties.put("buchungstext", Map.of(
                "type", "string",
                "description", "Kurzer, normalisierter Buchungstext für den Beleg"));
        properties.put("buchungsdatum", Map.of(
                "type", "string",
                "description", "Buchungsdatum im Format YYYY-MM-DD"));
        properties.put("konfidenz", Map.of(
                "type", "number",
                "description", "Konfidenz der Zuordnung zwischen 0.0 und 1.0"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(
                "erfolg", "fehler", "betrag", "waehrung",
                "sollKontoNr", "sollKontoBezeichnung", "habenKontoNr", "habenKontoBezeichnung",
                "buchungstext", "buchungsdatum", "konfidenz"));
        schema.put("additionalProperties", false);
        return schema;
    }
}