package com.caisse.service;

import com.caisse.model.Ticket;
import com.caisse.model.TicketItem;
import com.caisse.model.TicketSummary;
import com.caisse.state.AppState;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketService {

    private final SupabaseClient client = SupabaseClient.getInstance();
    private final ProductService productService = new ProductService();

    /**
     * Saves the ticket, its line items and its payment, then decrements stock
     * for every product sold. Every step re-checks AppState.hasOpenJourney()
     * so this can never run against a closed/absent journey even if the UI
     * state got stale.
     */
    public Ticket saveTicket(Ticket ticket) throws IOException, InterruptedException {
        if (!AppState.getInstance().hasOpenJourney()) {
            throw new IOException("Aucune journée ouverte. Impossible d'enregistrer un ticket.");
        }
        if (ticket.getItems().isEmpty()) {
            throw new IOException("Le ticket ne contient aucun article.");
        }

        String ticketId = UUID.randomUUID().toString();
        ticket.setId(ticketId);
        ticket.setCreatedAt(OffsetDateTime.now());

        JSONObject ticketRow = new JSONObject()
                .put("id", ticketId)
                .put("journey_id", ticket.getJourneyId())
                .put("cashier_id", ticket.getCashierId())
                .put("total", ticket.total())
                .put("payment_method", ticket.getPaymentMethod())
                .put("created_at", ticket.getCreatedAt().toString());
        client.insert("tickets", ticketRow);

        for (TicketItem item : ticket.getItems()) {
            JSONObject itemRow = new JSONObject()
                    .put("id", UUID.randomUUID().toString())
                    .put("ticket_id", ticketId)
                    .put("product_id", item.getProduct().getId())
                    .put("quantity", item.getQuantity())
                    .put("unit_price", item.getUnitPrice())
                    .put("discount_percent", item.getDiscountPercent())
                    .put("line_total", item.lineTotal());
            client.insert("ticket_items", itemRow);
            productService.adjustStock(item.getProduct().getId(), -item.getQuantity());
        }

        JSONObject paymentRow = new JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("ticket_id", ticketId)
                .put("method", ticket.getPaymentMethod())
                .put("amount", ticket.total());
        client.insert("payments", paymentRow);

        return ticket;
    }

    /** Used by the Returns screen to look up and display the original ticket. */
    public JSONObject findTicketWithItems(String ticketId) throws IOException, InterruptedException {
        JSONArray tickets = client.select("tickets", "select=*&id=eq." + ticketId);
        if (tickets.isEmpty()) return null;
        JSONObject ticket = tickets.getJSONObject(0);
        JSONArray items = client.select("ticket_items",
                "select=*,products(code,label)&ticket_id=eq." + ticketId);
        ticket.put("items", items);
        return ticket;
    }

    /**
     * Used by the ticket history screen to list a cashier's tickets within
     * a date range (inclusive). Pass the same date for "from" and "to" to
     * get a single day (e.g. today's tickets).
     */
    public List<TicketSummary> listTicketsForCashier(String cashierId, LocalDate from, LocalDate to)
            throws IOException, InterruptedException {

        StringBuilder filter = new StringBuilder("select=id,total,payment_method,created_at");
        filter.append("&created_at=gte.").append(from).append("T00:00:00");
        filter.append("&created_at=lte.").append(to).append("T23:59:59");
        filter.append("&order=created_at.desc");

        if (cashierId != null && !cashierId.isBlank()) {
            filter.append("&cashier_id=eq.").append(cashierId);
        }

        JSONArray rows = client.select("tickets", filter.toString());

        List<TicketSummary> out = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject t = rows.getJSONObject(i);
            out.add(new TicketSummary(
                    t.getString("id"),
                    OffsetDateTime.parse(t.getString("created_at")),
                    t.optBigDecimal("total", BigDecimal.ZERO),
                    t.optString("payment_method", "")
            ));
        }
        return out;
    }
}