package com.caisse.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReportService {

    private final SupabaseClient client = SupabaseClient.getInstance();

    public static class ReportResult {
        public BigDecimal totalSales = BigDecimal.ZERO;
        public int ticketCount = 0;
        public BigDecimal cashSales = BigDecimal.ZERO;
        public BigDecimal cardSales = BigDecimal.ZERO;
        public BigDecimal returnsTotal = BigDecimal.ZERO;
        public BigDecimal netSales = BigDecimal.ZERO;
        public int productsSold = 0;
        public int productsReturned = 0;
    }

    /**
     * Pulls tickets/payments/returns (including both open and closed journeys, as required)
     * within [from, to] and optionally filtered by cashier code or payment method.
     * Reports are read-only: this service never writes.
     */
    public ReportResult run(LocalDate from, LocalDate to, String cashierCodeOrNull, String paymentMethodOrNull)
            throws IOException, InterruptedException {

        StringBuilder ticketQuery = new StringBuilder("select=id,total,payment_method,journey_id,created_at,journeys(cashier_code)");
        ticketQuery.append("&created_at=gte.").append(from).append("T00:00:00");
        ticketQuery.append("&created_at=lte.").append(to).append("T23:59:59");
        if (paymentMethodOrNull != null && !paymentMethodOrNull.isBlank()) {
            ticketQuery.append("&payment_method=eq.").append(paymentMethodOrNull);
        }
        JSONArray tickets = client.select("tickets", ticketQuery.toString());

        ReportResult result = new ReportResult();
        for (int i = 0; i < tickets.length(); i++) {
            JSONObject t = tickets.getJSONObject(i);
            if (cashierCodeOrNull != null && !cashierCodeOrNull.isBlank()) {
                JSONObject journey = t.optJSONObject("journeys");
                String code = journey != null ? journey.optString("cashier_code", "") : "";
                if (!cashierCodeOrNull.equalsIgnoreCase(code)) continue;
            }
            BigDecimal total = t.optBigDecimal("total", BigDecimal.ZERO);
            result.totalSales = result.totalSales.add(total);
            result.ticketCount++;
            if ("CASH".equalsIgnoreCase(t.optString("payment_method", ""))) {
                result.cashSales = result.cashSales.add(total);
            } else {
                result.cardSales = result.cardSales.add(total);
            }
        }

        // products sold, from ticket_items joined to the filtered tickets' date range
        JSONArray items = client.select("ticket_items",
                "select=quantity,tickets!inner(created_at)&tickets.created_at=gte." + from + "T00:00:00" +
                        "&tickets.created_at=lte." + to + "T23:59:59");
        for (int i = 0; i < items.length(); i++) {
            result.productsSold += items.getJSONObject(i).optInt("quantity", 0);
        }

        JSONArray returns = client.select("returns",
                "select=total,created_at&created_at=gte." + from + "T00:00:00&created_at=lte." + to + "T23:59:59");
        for (int i = 0; i < returns.length(); i++) {
            result.returnsTotal = result.returnsTotal.add(returns.getJSONObject(i).optBigDecimal("total", BigDecimal.ZERO));
        }

        JSONArray returnItems = client.select("return_items",
                "select=quantity,returns!inner(created_at)&returns.created_at=gte." + from + "T00:00:00" +
                        "&returns.created_at=lte." + to + "T23:59:59");
        for (int i = 0; i < returnItems.length(); i++) {
            result.productsReturned += returnItems.getJSONObject(i).optInt("quantity", 0);
        }

        result.netSales = result.totalSales.subtract(result.returnsTotal);
        return result;
    }
}
