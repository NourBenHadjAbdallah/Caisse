package com.caisse.util;

import com.caisse.model.Journey;
import com.caisse.model.JourneyClosingResult;
import com.caisse.model.ReceiptRow;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Builds the ordered list of receipt lines for each printable document. Same content feeds the screen preview, the printer, and the PDF export. */
public final class ReceiptContentFactory {

    private ReceiptContentFactory() {}

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Edit these to match your actual shop.
    private static final String STORE_NAME = "CAISSE";
    private static final String STORE_SUBTITLE = "POINT DE VENTE";
    private static final String STORE_ADDRESS = "Votre adresse ici";
    private static final String STORE_PHONE = "TEL : 00 000 000";

    private static final BigDecimal TAX_RATE = new BigDecimal("0.07");
    private static final String TAX_LABEL = "TVA";

    // =========================================================
    // ITEMIZED TICKET
    // =========================================================

    public static List<ReceiptRow> buildTicketRows(JSONObject ticket) {
        List<ReceiptRow> rows = new ArrayList<>();

        OffsetDateTime createdAt = OffsetDateTime.parse(ticket.getString("created_at"));
        JSONArray items = ticket.getJSONArray("items");
        BigDecimal total = ticket.optBigDecimal("total", BigDecimal.ZERO);
        String paymentMethod = ticket.optString("payment_method", "");
        String shortId = ticket.getString("id").substring(0, 8).toUpperCase();

        header(rows);

        rows.add(ReceiptRow.textMuted("Numéro : " + shortId, 10));
        rows.add(ReceiptRow.textMuted("Date : " + createdAt.format(DATE_FMT) + "   " + createdAt.format(TIME_FMT), 10));
        rows.add(ReceiptRow.textMuted("Journée : " + ticket.optString("journey_id", "-"), 10));
        rows.add(ReceiptRow.spacer(10));

        rows.add(ReceiptRow.barcode(ticket.getString("id")));
        rows.add(ReceiptRow.text("* " + shortId + " *", 10, false));
        rows.add(ReceiptRow.dashed());

        rows.add(ReceiptRow.twoCol("Désignation", "Montant TTC", 11, true));
        rows.add(ReceiptRow.dashed());

        int articleCount = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject product = item.optJSONObject("products");
            String code = product != null ? product.optString("code", "") : "";
            String label = product != null ? product.optString("label", "") : "";
            int qty = item.optInt("quantity", 0);
            articleCount += qty;
            BigDecimal lineTotal = item.optBigDecimal("line_total", BigDecimal.ZERO);

            rows.add(ReceiptRow.twoCol(qty + "x " + label, money(lineTotal), 11, false));
            if (code != null && !code.isBlank()) {
                rows.add(ReceiptRow.textMuted(code, 9));
            }
        }

        rows.add(ReceiptRow.spacer(10));
        rows.add(ReceiptRow.text("**** " + money(total) + " ****", 16, true));
        rows.add(ReceiptRow.text(articleCount + (articleCount > 1 ? " articles" : " article"), 10, false));
        rows.add(ReceiptRow.dashed());

        rows.add(ReceiptRow.text("Règlement", 12, true));
        String paymentLabel = "CASH".equalsIgnoreCase(paymentMethod) ? "Espèces" : "Carte bancaire";
        rows.add(ReceiptRow.twoCol(paymentLabel, money(total), 11, false));
        rows.add(ReceiptRow.dashed());

        addTaxBlock(rows, total);

        rows.add(ReceiptRow.spacer(14));
        rows.add(ReceiptRow.text("Merci pour votre visite !", 11, true));

        return rows;
    }

    // =========================================================
    // TICKET X — read-only mid-journey snapshot
    // =========================================================

    public static List<ReceiptRow> buildTicketXRows(Journey journey) {
        List<ReceiptRow> rows = new ArrayList<>();

        header(rows);

        rows.add(ReceiptRow.text("TICKET X", 14, true));
        rows.add(ReceiptRow.textMuted("Lecture seule — journée toujours ouverte", 10));
        rows.add(ReceiptRow.spacer(6));

        rows.add(ReceiptRow.textMuted("Journée : " + journey.getId(), 10));
        rows.add(ReceiptRow.textMuted("Caissier : " + journey.getCashierCode(), 10));
        OffsetDateTime now = OffsetDateTime.now();
        rows.add(ReceiptRow.textMuted("Édité le : " + now.format(DATE_FMT) + "   " + now.format(TIME_FMT), 10));
        rows.add(ReceiptRow.spacer(10));

        rows.add(ReceiptRow.barcode(journey.getId()));
        rows.add(ReceiptRow.dashed());

        addJourneySummaryBlock(rows, journey);

        rows.add(ReceiptRow.spacer(14));
        rows.add(ReceiptRow.text("Journée toujours ouverte.", 11, true));

        return rows;
    }

    // =========================================================
    // TICKET Z — closing + liste des règlements
    // =========================================================

    public static List<ReceiptRow> buildTicketZRows(JourneyClosingResult result) {
        List<ReceiptRow> rows = new ArrayList<>();
        Journey journey = result.getJourney();

        header(rows);

        rows.add(ReceiptRow.text("TICKET Z", 14, true));
        rows.add(ReceiptRow.textMuted("Fermeture de journée", 10));
        rows.add(ReceiptRow.spacer(6));

        rows.add(ReceiptRow.textMuted("Journée : " + journey.getId(), 10));
        rows.add(ReceiptRow.textMuted("Caissier : " + journey.getCashierCode(), 10));
        rows.add(ReceiptRow.textMuted("Fermée le : " +
                (journey.getClosedAt() != null
                        ? journey.getClosedAt().format(DATE_FMT) + "   " + journey.getClosedAt().format(TIME_FMT)
                        : "-"), 10));
        rows.add(ReceiptRow.spacer(10));

        rows.add(ReceiptRow.barcode(journey.getId()));
        rows.add(ReceiptRow.dashed());

        addJourneySummaryBlock(rows, journey);

        rows.add(ReceiptRow.spacer(10));
        rows.add(ReceiptRow.text("LISTE DES RÈGLEMENTS", 12, true));
        rows.add(ReceiptRow.dashed());
        rows.add(ReceiptRow.twoCol("Espèces (comptées)", money(result.getCashCounted()), 11, false));
        rows.add(ReceiptRow.twoCol("Carte (comptée)", money(result.getCardCounted()), 11, false));
        rows.add(ReceiptRow.dashed());

        rows.add(ReceiptRow.spacer(14));
        rows.add(ReceiptRow.text("Journée fermée définitivement.", 11, true));

        return rows;
    }

    // =========================================================
    // SHARED HELPERS
    // =========================================================

    private static void header(List<ReceiptRow> rows) {
        rows.add(ReceiptRow.title(STORE_NAME, 20));
        rows.add(ReceiptRow.text(STORE_SUBTITLE, 10, true));
        rows.add(ReceiptRow.spacer(6));
        rows.add(ReceiptRow.textMuted(STORE_ADDRESS, 10));
        rows.add(ReceiptRow.textMuted(STORE_PHONE, 10));
        rows.add(ReceiptRow.spacer(8));
    }

    private static void addJourneySummaryBlock(List<ReceiptRow> rows, Journey journey) {
        rows.add(ReceiptRow.twoCol("Tickets", String.valueOf(journey.getTicketCount()), 11, false));
        rows.add(ReceiptRow.twoCol("Ventes brutes", money(journey.getTotalSales()), 11, false));
        rows.add(ReceiptRow.twoCol("Espèces", money(journey.getTotalCash()), 11, false));
        rows.add(ReceiptRow.twoCol("Carte", money(journey.getTotalCard()), 11, false));
        rows.add(ReceiptRow.twoCol("Retours", money(journey.getTotalReturns()), 11, false));
        rows.add(ReceiptRow.dashed());
        rows.add(ReceiptRow.twoCol("VENTES NETTES", money(journey.getNetTotal()), 13, true));
    }

    private static void addTaxBlock(List<ReceiptRow> rows, BigDecimal total) {
        BigDecimal baseHT = total.divide(BigDecimal.ONE.add(TAX_RATE), 3, RoundingMode.HALF_UP);
        BigDecimal taxAmount = total.subtract(baseHT).setScale(3, RoundingMode.HALF_UP);

        rows.add(ReceiptRow.text("Taxe    Montant   Taux    Base HT", 10, true));
        rows.add(ReceiptRow.dashed());
        rows.add(ReceiptRow.text(
                TAX_LABEL + "     " + money(taxAmount) + "   " +
                        TAX_RATE.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%   " +
                        money(baseHT), 10, false));
    }

    private static String money(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return v.setScale(3, RoundingMode.HALF_UP) + " DT";
    }
}