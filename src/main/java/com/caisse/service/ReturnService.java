package com.caisse.service;

import com.caisse.model.ReturnItem;
import com.caisse.model.ReturnRecord;
import com.caisse.state.AppState;
import org.json.JSONObject;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ReturnService {

    private final SupabaseClient client = SupabaseClient.getInstance();
    private final ProductService productService = new ProductService();

    public ReturnRecord saveReturn(ReturnRecord ret) throws IOException, InterruptedException {
        if (!AppState.getInstance().hasOpenJourney()) {
            throw new IOException("Aucune journée ouverte. Impossible de traiter un retour.");
        }
        if (ret.getItems().isEmpty()) {
            throw new IOException("Sélectionnez au moins un article à retourner.");
        }

        String returnId = UUID.randomUUID().toString();
        ret.setId(returnId);
        ret.setCreatedAt(OffsetDateTime.now());

        JSONObject row = new JSONObject()
                .put("id", returnId)
                .put("original_ticket_id", ret.getOriginalTicketId())
                .put("journey_id", ret.getJourneyId())
                .put("total", ret.total())
                .put("created_at", ret.getCreatedAt().toString());
        client.insert("returns", row);

        for (ReturnItem item : ret.getItems()) {
            JSONObject itemRow = new JSONObject()
                    .put("id", UUID.randomUUID().toString())
                    .put("return_id", returnId)
                    .put("product_id", item.getProduct().getId())
                    .put("quantity", item.getQuantity())
                    .put("unit_price", item.getUnitPrice())
                    .put("line_total", item.lineTotal());
            client.insert("return_items", itemRow);
            productService.adjustStock(item.getProduct().getId(), item.getQuantity()); // stock increases
        }

        return ret;
    }
}
