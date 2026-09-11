package com.caisse.service;

import com.caisse.model.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    /** delta is negative for a sale, positive for a return. */
    public void adjustStock(String productId, int delta) throws IOException, InterruptedException {
        JSONArray rows = client.select("products", "select=stock_quantity&id=eq." + productId);
        int current = rows.getJSONObject(0).getInt("stock_quantity");
        JSONObject changes = new JSONObject().put("stock_quantity", current + delta);
        client.update("products", "id=eq." + productId, changes);
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

    private String urlEnc(String s) {
        return s.replace(" ", "%20");
    }
}
