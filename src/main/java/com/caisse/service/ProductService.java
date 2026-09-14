package com.caisse.service;

import com.caisse.model.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductService {

    private final SupabaseClient client = SupabaseClient.getInstance();

    public Product findByCode(String code) throws IOException, InterruptedException {
        JSONArray rows = client.select("products", "select=*&code=eq." + urlEnc(code) + "&limit=1");
        if (rows.isEmpty()) return null;
        return fromJson(rows.getJSONObject(0));
    }

    public List<Product> search(String term) throws IOException, InterruptedException {
        // PostgREST "or" filter across code and label, case-insensitive
        String q = "select=*&or=(code.ilike.*" + urlEnc(term) + "*,label.ilike.*" + urlEnc(term) + "*)&limit=50";
        JSONArray rows = client.select("products", q);
        List<Product> out = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) out.add(fromJson(rows.getJSONObject(i)));
        return out;
    }

    public List<Product> listAll() throws IOException, InterruptedException {
        JSONArray rows = client.select("products", "select=*&order=label.asc");
        List<Product> out = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) out.add(fromJson(rows.getJSONObject(i)));
        return out;
    }

    /**
     * Adjusts stock atomically via the adjust_stock(uuid, integer) Postgres function
     * (see sql/migration_security_hardening.sql). This replaces the previous
     * select-then-update pattern, which had a race condition: two concurrent
     * sales of the last unit could both read the same stock value and both
     * succeed, resulting in negative/oversold stock. The database function
     * performs the increment and the "still non-negative" check as a single
     * atomic statement, and raises an error if the adjustment would go negative.
     *
     * delta is negative for a sale, positive for a return.
     */
    public void adjustStock(String productId, int delta) throws IOException, InterruptedException {
        client.rpc("adjust_stock", Map.of(
                "p_product_id", productId,
                "p_delta", delta
        ));
    }

    private Product fromJson(JSONObject o) {
        Product p = new Product();
        p.setId(o.getString("id"));
        p.setCode(o.getString("code"));
        p.setLabel(o.getString("label"));
        p.setStockQuantity(o.optInt("stock_quantity", 0));
        p.setNormalPrice(o.optBigDecimal("normal_price", BigDecimal.ZERO));
        p.setDiscountPercent(o.optBigDecimal("discount_percent", BigDecimal.ZERO));
        return p;
    }

    /**
     * Properly URL-encodes a search/filter term before it's interpolated into
     * a PostgREST query string. The previous implementation only replaced
     * spaces, which left characters meaningful to PostgREST's filter syntax
     * (",", "(", ")", ".", "*") unescaped — a crafted search term could alter
     * the intended filter (e.g. break out of the "or=(...)" clause or inject
     * additional conditions). Full URL-encoding closes that off.
     */
    private String urlEnc(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported; this branch is unreachable in practice.
            return s.replace(" ", "%20");
        }
    }
}