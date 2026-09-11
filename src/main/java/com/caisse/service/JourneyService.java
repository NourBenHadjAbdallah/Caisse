package com.caisse.service;

import com.caisse.model.Journey;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class JourneyService {

    private final SupabaseClient client = SupabaseClient.getInstance();

    /** Looks up whether the given cashier already has an OPEN journey (source of truth = Supabase). */
    public Journey findOpenJourney(String userId) throws IOException, InterruptedException {
        JSONArray rows = client.select("journeys",
                "select=*&user_id=eq." + userId + "&status=eq.OPEN&order=opened_at.desc&limit=1");
        if (rows.isEmpty()) return null;
        return fromJson(rows.getJSONObject(0));
    }

    /**
     * Opens a new journey for the cashier. The "users" table's cashier_code is embedded
     * for display; the actual one-open-journey-per-cashier rule is enforced in Postgres
     * via a unique partial index (see sql/schema.sql), so a duplicate attempt raises a
     * 409/23505 error that we surface as a friendly message instead of relying only on
     * the Java-side check.
     */
    public Journey openJourney(String userId, String cashierCode) throws IOException, InterruptedException {
        String journeyId = generateJourneyId();
        JSONObject row = new JSONObject()
                .put("id", journeyId)
                .put("user_id", userId)
                .put("cashier_code", cashierCode)
                .put("opened_at", OffsetDateTime.now().toString())
                .put("status", "OPEN")
                .put("total_sales", 0)
                .put("total_cash", 0)
                .put("total_card", 0)
                .put("total_returns", 0)
                .put("net_total", 0);
        try {
            JSONArray inserted = client.insert("journeys", row);
            return fromJson(inserted.getJSONObject(0));
        } catch (IOException e) {
            if (e.getMessage() != null && (e.getMessage().contains("23505") || e.getMessage().toLowerCase().contains("duplicate"))) {
                throw new IOException("Une journée est déjà ouverte pour ce caissier.");
            }
            throw e;
        }
    }

    /**
     * Recomputes the journey's financial summary from tickets/payments/returns,
     * then marks it CLOSED. Call buildSummary(...) first to show the confirmation
     * screen, then closeJourney(...) once the cashier confirms.
     */
    public Journey closeJourney(Journey journey) throws IOException, InterruptedException {
        JSONObject changes = new JSONObject()
                .put("status", "CLOSED")
                .put("closed_at", OffsetDateTime.now().toString())
                .put("total_sales", journey.getTotalSales())
                .put("total_cash", journey.getTotalCash())
                .put("total_card", journey.getTotalCard())
                .put("total_returns", journey.getTotalReturns())
                .put("net_total", journey.getNetTotal());
        JSONArray updated = client.update("journeys", "id=eq." + journey.getId(), changes);
        return fromJson(updated.getJSONObject(0));
    }

    /** Pulls tickets/payments/returns totals for the journey to populate the closing summary screen. */
    public Journey buildSummary(Journey journey) throws IOException, InterruptedException {
        JSONArray tickets = client.select("tickets", "select=id,total&journey_id=eq." + journey.getId());
        JSONArray payments = client.select("payments",
                "select=method,amount&ticket_id=in.(" + ticketIdsCsv(tickets) + ")");
        JSONArray returns = client.select("returns", "select=id,total&journey_id=eq." + journey.getId());

        BigDecimal gross = BigDecimal.ZERO;
        for (int i = 0; i < tickets.length(); i++) gross = gross.add(tickets.getJSONObject(i).getBigDecimal("total"));

        BigDecimal cash = BigDecimal.ZERO, card = BigDecimal.ZERO;
        for (int i = 0; i < payments.length(); i++) {
            JSONObject p = payments.getJSONObject(i);
            BigDecimal amt = p.getBigDecimal("amount");
            if ("CASH".equalsIgnoreCase(p.getString("method"))) cash = cash.add(amt);
            else card = card.add(amt);
        }

        BigDecimal returnsTotal = BigDecimal.ZERO;
        for (int i = 0; i < returns.length(); i++) returnsTotal = returnsTotal.add(returns.getJSONObject(i).getBigDecimal("total"));

        journey.setTicketCount(tickets.length());
        journey.setTotalSales(gross);
        journey.setTotalCash(cash);
        journey.setTotalCard(card);
        journey.setTotalReturns(returnsTotal);
        journey.setNetTotal(gross.subtract(returnsTotal));
        return journey;
    }

    private String ticketIdsCsv(JSONArray tickets) {
        if (tickets.isEmpty()) return "00000000-0000-0000-0000-000000000000"; // no matches, valid empty filter
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tickets.length(); i++) {
            if (i > 0) sb.append(",");
            sb.append(tickets.getJSONObject(i).getString("id"));
        }
        return sb.toString();
    }

    private String generateJourneyId() {
        int year = OffsetDateTime.now().getYear();
        int seq = new Random().nextInt(9000) + 1000; // display id; the DB row id is the source of truth
        return "J-" + year + "-" + seq;
    }

    private Journey fromJson(JSONObject o) {
        Journey j = new Journey();
        j.setId(o.getString("id"));
        j.setUserId(o.getString("user_id"));
        j.setCashierCode(o.optString("cashier_code", ""));
        j.setStatus(o.getString("status"));
        j.setOpenedAt(OffsetDateTime.parse(o.getString("opened_at"), safeFormatter(o.getString("opened_at"))));
        if (!o.isNull("closed_at") && o.optString("closed_at", null) != null) {
            j.setClosedAt(OffsetDateTime.parse(o.getString("closed_at")));
        }
        j.setTotalSales(o.optBigDecimal("total_sales", BigDecimal.ZERO));
        j.setTotalCash(o.optBigDecimal("total_cash", BigDecimal.ZERO));
        j.setTotalCard(o.optBigDecimal("total_card", BigDecimal.ZERO));
        j.setTotalReturns(o.optBigDecimal("total_returns", BigDecimal.ZERO));
        j.setNetTotal(o.optBigDecimal("net_total", BigDecimal.ZERO));
        return j;
    }

    private DateTimeFormatter safeFormatter(String sample) {
        return DateTimeFormatter.ISO_DATE_TIME;
    }
}
