package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ÄNDERUNG: @EnableScheduling ergänzt – wird von
 * RechnungService.ueberfaelligePruefenScheduled() benötigt, damit offene
 * Rechnungen mit überschrittener Fälligkeit täglich automatisch auf
 * UEBERFAELLIG gesetzt werden (zusätzlich zur on-demand-Prüfung bei jeder
 * Cashflow-Abfrage in ReportingService).
 */
@SpringBootApplication
@EnableScheduling
public class ErpSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpSystemApplication.class, args);
	}

}