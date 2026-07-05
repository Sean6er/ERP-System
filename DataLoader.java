package com.example.demo;


import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.BuchungsService;
import com.example.demo.service.FiMmIntegrationService;
import com.example.demo.service.FiSdIntegrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ÄNDERUNG (Kontengruppen-Feature): Der Kontenplan bleibt weiterhin EIN
 * einziger Kontenplan (GL00) für den gesamten Konzern (SAP-Standard: ein
 * Buchungskreis → genau ein Kontenplan). NEU werden zusätzlich sechs
 * Kontengruppen (GL00–GL05) angelegt, die die Sachkonten INNERHALB dieses
 * einen Kontenplans über einen Nummernkreis klassifizieren – siehe
 * Kontengruppe.java-Javadoc. Jedes per anlegen() erstellte Konto bekommt
 * automatisch die zu seiner Kontonummer passende Kontengruppe zugewiesen.
 *
 * WICHTIG (persistente H2-Datenbank): Der Idempotenz-Guard unten
 * (`if (mandantRepo.findByMandantNr("GB00").isPresent()) return;`)
 * verhindert ein erneutes Seeden, SOBALD einmal Testdaten angelegt wurden.
 * Änderungen an dieser Klasse (wie die Kontengruppen hier) wirken sich auf
 * eine bereits gefüllte H2-Datei NICHT aus! Falls die Kontenübersicht nach
 * einem Neustart weiterhin die alte Struktur zeigt: die H2-Datei (Pfad
 * siehe application.properties, z.B. ./data/erpdb.mv.db) löschen und neu
 * starten, damit DataLoader wieder von vorne seedet.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final KontoRepository kontoRepo;
    private final BuchungsService buchungsService;
    private final MandantRepository mandantRepo;
    private final GeschaeftsbereichRepository gbRepo;
    private final KreditkontrollbereichRepository kkbRepo;
    private final KontenplanRepository kpRepo;
    private final KontengruppeRepository kgRepo;
    private final BuchungskreisRepository bkRepo;
    private final KundeRepository kundeRepo;
    private final LieferantRepository lieferantRepo;
    private final GeschaeftsfallRepository geschaeftsfallRepo;
    private final RechnungRepository rechnungRepo;
    private final FiSdIntegrationService fiSdService;
    private final FiMmIntegrationService fiMmService;

    public DataLoader(KontoRepository kontoRepo, BuchungsService buchungsService,
                      MandantRepository mandantRepo, GeschaeftsbereichRepository gbRepo,
                      KreditkontrollbereichRepository kkbRepo, KontenplanRepository kpRepo,
                      KontengruppeRepository kgRepo,
                      BuchungskreisRepository bkRepo, KundeRepository kundeRepo,
                      LieferantRepository lieferantRepo, GeschaeftsfallRepository geschaeftsfallRepo,
                      RechnungRepository rechnungRepo, FiSdIntegrationService fiSdService,
                      FiMmIntegrationService fiMmService) {
        this.kontoRepo = kontoRepo;
        this.buchungsService = buchungsService;
        this.mandantRepo = mandantRepo;
        this.gbRepo = gbRepo;
        this.kkbRepo = kkbRepo;
        this.kpRepo = kpRepo;
        this.kgRepo = kgRepo;
        this.bkRepo = bkRepo;
        this.kundeRepo = kundeRepo;
        this.lieferantRepo = lieferantRepo;
        this.geschaeftsfallRepo = geschaeftsfallRepo;
        this.rechnungRepo = rechnungRepo;
        this.fiSdService = fiSdService;
        this.fiMmService = fiMmService;
    }

    // ── Kontonummern (Klassenschema – siehe Kontengruppen-Javadoc oben) ─────
    // Klasse 0-1 / Kontengruppe GL00: Anlage- und Umlaufvermögen (AKTIV)
    private static final int K_MASCHINEN        = 1000;
    private static final int K_FUHRPARK         = 10000;
    private static final int K_BANK             = 100000;
    private static final int K_BANK_US00        = 100005;
    private static final int K_BANK_DE00        = 100006;
    private static final int K_KASSE            = 100010;
    private static final int K_FORDERUNGEN      = 120000;
    private static final int K_VORRAETE         = 140000;
    // Klasse 2 / Kontengruppe GL01: Eigenkapital und Rückstellungen (PASSIV)
    private static final int K_EIGENKAPITAL     = 200000;
    private static final int K_GEWINNRUECKLAGE  = 210000;
    private static final int K_RUECKSTELLUNGEN  = 260000;
    // Klasse 3 / Kontengruppe GL02: Verbindlichkeiten (PASSIV)
    private static final int K_VERB_ALUL        = 300000;
    private static final int K_VERB_SONSTIGE    = 310000;
    private static final int K_VERB_INLAND      = 320000;
    // Klasse 4-7 / Kontengruppe GL03: Betriebliche Aufwendungen (AUFWAND)
    private static final int K_MATERIALAUFWAND  = 400000;
    private static final int K_PERSONALAUFWAND  = 420000;
    private static final int K_MIETAUFWAND      = 600000;
    private static final int K_VERSICHERUNG     = 620000;
    private static final int K_SONST_AUFWAND    = 650000;
    private static final int K_ABSCHREIBUNGEN   = 700000;
    // Klasse 8 / Kontengruppe GL04: Erlöse (ERTRAG)
    private static final int K_UMSATZERLOESE    = 800000;
    private static final int K_SONST_ERTRAEGE   = 810000;
    // Klasse 9 / Kontengruppe GL05: Vortrags- und statistische Konten
    private static final int K_SALDENVORTRAG    = 900000;

    @Override
    public void run(String... args) {
        // Idempotenz-Guard: bei Hot-Restart mit persistenter H2-Datei nicht doppelt seeden
        if (mandantRepo.findByMandantNr("GB00").isPresent()) {
            System.out.println("ℹ Testdaten bereits vorhanden – DataLoader übersprungen. " +
                    "Falls die Kontenstruktur veraltet wirkt: H2-Datei löschen und neu starten.");
            return;
        }

        // 1. Mandant anlegen
        Mandant gb = mandantRepo.save(new Mandant("GB00", "Global Bike", "EUR"));

        // 2. EIN Kontenplan GL00 für den gesamten Konzern (SAP-Standard)
        Kontenplan gl00 = kpRepo.save(new Kontenplan("GL00", "Global Bike Kontenplan"));

        // 2a. Kontengruppen anlegen – klassifizieren die Sachkonten INNERHALB
        //     von GL00 über einen Nummernkreis (siehe Kontengruppe.java).
        kgRepo.save(new Kontengruppe("GL00", "Anlage- und Umlaufvermögen", 0, 199999));
        kgRepo.save(new Kontengruppe("GL01", "Eigenkapital und Rückstellungen", 200000, 299999));
        kgRepo.save(new Kontengruppe("GL02", "Verbindlichkeiten", 300000, 399999));
        kgRepo.save(new Kontengruppe("GL03", "Betriebliche Aufwendungen", 400000, 799999));
        kgRepo.save(new Kontengruppe("GL04", "Erlöse", 800000, 899999));
        kgRepo.save(new Kontengruppe("GL05", "Vortrags- und statistische Konten", 900000, 999999));

        // 3. Kreditkontrollbereich MIT Kreditlimit anlegen
        Kreditkontrollbereich kkb = kkbRepo.save(
                new Kreditkontrollbereich("GB00", "Global Bike KKB", "EUR", new BigDecimal("50000.00")));

        // 4. Buchungskreise anlegen
        Buchungskreis de00 = bkRepo.save(new Buchungskreis("DE00", "Global Bike Germany GmbH", "Deutschland", "EUR", gb));
        de00.setKontenplan(gl00);
        de00.setKreditkontrollbereich(kkb);
        de00 = bkRepo.save(de00);

        Buchungskreis us00 = bkRepo.save(new Buchungskreis("US00", "Global Bike US Inc.", "USA", "EUR", gb));
        us00.setKontenplan(gl00);
        us00.setKreditkontrollbereich(kkb);
        us00 = bkRepo.save(us00);

        // 5. Konten anlegen – klassenbasiert, alle unter Kontenplan GL00,
        //    jeweils mit automatisch zugeordneter Kontengruppe (siehe anlegen()).

        // Kontengruppe GL00: Anlage- und Umlaufvermögen (AKTIV)
        anlegen(K_MASCHINEN,   "Maschinen und technische Anlagen",  Konto.Kontotyp.AKTIV, gl00);
        anlegen(K_FUHRPARK,    "Fuhrpark",                          Konto.Kontotyp.AKTIV, gl00);
        anlegen(K_BANK,        "Bank Hauptkonto (GlobalBike)",      Konto.Kontotyp.AKTIV, gl00);
        anlegen(K_BANK_US00,   "Bank Zahlungsausgang (US00)",       Konto.Kontotyp.AKTIV, gl00);
        anlegen(K_BANK_DE00,   "Bank Deutschland (DE00)",           Konto.Kontotyp.AKTIV, gl00);
        anlegen(K_KASSE,       "Kasse / Barbestand",                Konto.Kontotyp.AKTIV, gl00);
        anlegen(K_FORDERUNGEN, "Forderungen aLuL",                  Konto.Kontotyp.AKTIV, gl00);
        anlegen(K_VORRAETE,    "Roh-, Hilfs- und Betriebsstoffe",   Konto.Kontotyp.AKTIV, gl00);

        // Kontengruppe GL01: Eigenkapital und Rückstellungen (PASSIV)
        anlegen(K_EIGENKAPITAL,    "Gezeichnetes Kapital",   Konto.Kontotyp.PASSIV, gl00);
        anlegen(K_GEWINNRUECKLAGE, "Gewinnrücklagen",        Konto.Kontotyp.PASSIV, gl00);
        anlegen(K_RUECKSTELLUNGEN, "Rückstellungen",         Konto.Kontotyp.PASSIV, gl00);

        // Kontengruppe GL02: Verbindlichkeiten (PASSIV)
        anlegen(K_VERB_ALUL,     "Verbindlichkeiten aLuL",              Konto.Kontotyp.PASSIV, gl00);
        anlegen(K_VERB_SONSTIGE, "sonstige Verbindlichkeiten",          Konto.Kontotyp.PASSIV, gl00);
        anlegen(K_VERB_INLAND,   "sonstige Verbindlichkeiten Inland",   Konto.Kontotyp.PASSIV, gl00);

        // Kontengruppe GL03: Betriebliche Aufwendungen (AUFWAND)
        anlegen(K_MATERIALAUFWAND, "Materialaufwand",                       Konto.Kontotyp.AUFWAND, gl00);
        anlegen(K_PERSONALAUFWAND, "Personalaufwand",                       Konto.Kontotyp.AUFWAND, gl00);
        anlegen(K_MIETAUFWAND,     "Mietaufwand",                           Konto.Kontotyp.AUFWAND, gl00);
        anlegen(K_VERSICHERUNG,    "Versicherungsaufwand",                  Konto.Kontotyp.AUFWAND, gl00);
        anlegen(K_SONST_AUFWAND,   "Sonstige betriebliche Aufwendungen",    Konto.Kontotyp.AUFWAND, gl00);
        anlegen(K_ABSCHREIBUNGEN,  "Abschreibungen auf Sachanlagen",       Konto.Kontotyp.AUFWAND, gl00);

        // Kontengruppe GL04: Erlöse (ERTRAG)
        anlegen(K_UMSATZERLOESE,  "Umsatzerlöse",                    Konto.Kontotyp.ERTRAG, gl00);
        anlegen(K_SONST_ERTRAEGE, "Sonstige betriebliche Erträge",   Konto.Kontotyp.ERTRAG, gl00);

        // Kontengruppe GL05: Vortrags- und statistische Konten (wird nicht bebucht)
        anlegen(K_SALDENVORTRAG, "Saldenvortrag / Eröffnungsbilanzkonto", Konto.Kontotyp.AKTIV, gl00);

        // 6. Geschäftsbereiche
        gbRepo.save(new Geschaeftsbereich("BIKE", "Fahrräder"));
        gbRepo.save(new Geschaeftsbereich("ZUB", "Zubehör"));

        // 7. Reine Hauptbuch-Buchungen (Klassen 0-1 / 2 / 4-7, kein Nebenbuch beteiligt)
        buchungsService.bucheSollHaben(K_BANK, K_EIGENKAPITAL,
                new BigDecimal("100000.00"), "Eigenkapitaleinlage Gründung",
                LocalDate.now(), "DataLoader", null, "DE00");

        buchungsService.bucheSollHaben(K_MASCHINEN, K_BANK,
                new BigDecimal("25000.00"), "Kauf Produktionsmaschine",
                LocalDate.now(), "DataLoader", null, "DE00");

        buchungsService.bucheSollHaben(K_FUHRPARK, K_BANK,
                new BigDecimal("18000.00"), "Kauf Firmenfahrzeug",
                LocalDate.now(), "DataLoader", null, "DE00");

        buchungsService.bucheSollHaben(K_MIETAUFWAND, K_BANK,
                new BigDecimal("1500.00"), "Miete März",
                LocalDate.now(), "DataLoader", null, "DE00");

        buchungsService.bucheSollHaben(K_PERSONALAUFWAND, K_BANK,
                new BigDecimal("3200.00"), "Gehälter März",
                LocalDate.now(), "DataLoader", null, "DE00");

        buchungsService.bucheSollHaben(K_VERSICHERUNG, K_BANK,
                new BigDecimal("450.00"), "KFZ-Versicherung Q1",
                LocalDate.now(), "DataLoader", null, "DE00");

        buchungsService.bucheSollHaben(K_SONST_AUFWAND, K_RUECKSTELLUNGEN,
                new BigDecimal("3000.00"), "Rückstellung Gewährleistung",
                LocalDate.now(), "DataLoader", null, "DE00");

        buchungsService.bucheSollHaben(K_ABSCHREIBUNGEN, K_MASCHINEN,
                new BigDecimal("2000.00"), "Abschreibung Maschinen (AfA) März",
                LocalDate.now(), "DataLoader", null, "DE00");

        // 8. Stammdaten: Debitoren (Kunden)
        Kunde rockyMountain = new Kunde("Rocky Mountain Bikes", "Alex Rider",
                "info@rockymountain.example", "Denver, USA");
        rockyMountain.setAbstimmkonto(K_FORDERUNGEN);
        rockyMountain.setKreditkontrollbereich(kkb);
        rockyMountain = kundeRepo.save(rockyMountain);

        Kunde trekDeutschland = new Kunde("Trek Deutschland GmbH", "Sabine Huber",
                "buchhaltung@trek-de.example", "München, Deutschland");
        trekDeutschland.setAbstimmkonto(K_FORDERUNGEN);
        trekDeutschland = kundeRepo.save(trekDeutschland);

        // 9. Stammdaten: Kreditoren (Lieferanten)
        Lieferant vermieter = new Lieferant("Cardinal Properties",
                "billing@cardinalproperties.example", "+1 555 0100", "US00 – Buchungskreis-Region");
        vermieter.setAbstimmkonto(K_VERB_SONSTIGE);
        vermieter = lieferantRepo.save(vermieter);

        Lieferant canyon = new Lieferant("Canyon Supplies GmbH",
                "einkauf@canyon-supplies.example", "+49 30 5550101", "Koblenz, Deutschland");
        canyon.setAbstimmkonto(K_VERB_ALUL);
        canyon = lieferantRepo.save(canyon);

        // 10. Echte Geschäftsvorfälle über FI-SD/FI-MM buchen

        fiSdService.vollstaendigerVerkaufsprozess(
                rockyMountain.getFirmenname(), new BigDecimal("12000.00"), "DataLoader", "DE00");

        fiSdService.vollstaendigerVerkaufsprozess(
                rockyMountain.getFirmenname(), new BigDecimal("3000.00"), "DataLoader", "US00");

        Geschaeftsfall verkaufTrek = new Geschaeftsfall(LocalDate.now(),
                Geschaeftsfall.GeschaeftsfallTyp.VERKAUF, trekDeutschland, null);
        verkaufTrek.setBuchungskreis(de00);
        verkaufTrek = geschaeftsfallRepo.save(verkaufTrek);
        fiSdService.bucheFaktura(verkaufTrek.getGeschaeftsfallId(),
                new BigDecimal("8000.00"), trekDeutschland.getFirmenname(), "DataLoader");

        fiMmService.vollstaendigerEinkaufsprozess(
                canyon.getFirmenname(), new BigDecimal("4200.00"), "DataLoader", "DE00");

        Geschaeftsfall einkaufCardinal = new Geschaeftsfall(LocalDate.now(),
                Geschaeftsfall.GeschaeftsfallTyp.EINKAUF, null, null);
        einkaufCardinal.setBuchungskreis(de00);
        einkaufCardinal.setLieferant(vermieter);
        einkaufCardinal = geschaeftsfallRepo.save(einkaufCardinal);
        Map<String, Object> wareneingang = fiMmService.bucheWareneingang(
                einkaufCardinal.getGeschaeftsfallId(), new BigDecimal("2600.00"),
                vermieter.getFirmenname(), "DataLoader");

        Long ueberfaelligeRechnungId = ((Number) wareneingang.get("rechnungId")).longValue();
        rechnungRepo.findById(ueberfaelligeRechnungId).ifPresent(r -> {
            r.setFaelligkeit(LocalDate.now().minusDays(5));
            rechnungRepo.save(r);
        });

        System.out.println("✓ ERP-FI Testdaten geladen (Kontenplan GL00 mit Kontengruppen GL00–GL05, " +
                "Buchungskreise DE00/US00, Kreditlimit 50.000 EUR, offene + überfällige Posten) – " +
                "http://localhost:8080/api/reporting/bilanz");
    }

    /**
     * Legt ein Konto an und ordnet automatisch die zum Nummernkreis passende
     * Kontengruppe zu (siehe KontengruppeRepository.findPassendeGruppe()).
     */
    private void anlegen(int nummer, String bezeichnung, Konto.Kontotyp typ, Kontenplan kp) {
        Konto k = new Konto(nummer, bezeichnung, typ);
        k.setKontenplan(kp);
        List<Kontengruppe> treffer = kgRepo.findPassendeGruppe(nummer);
        if (!treffer.isEmpty()) {
            k.setKontengruppe(treffer.get(0));
        }
        kontoRepo.save(k);
    }
}