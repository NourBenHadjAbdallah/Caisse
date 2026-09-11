package com.caisse.model;

import java.math.BigDecimal;

public class Product {
    private String id;
    private String code;
    private String label;
    private int stockQuantity;
    private BigDecimal normalPrice;
    private BigDecimal discountPercent = BigDecimal.ZERO;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public BigDecimal getNormalPrice() { return normalPrice; }
    public void setNormalPrice(BigDecimal normalPrice) { this.normalPrice = normalPrice; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    /** Unit price after applying the discount percentage. */
    public BigDecimal finalPrice() {
        BigDecimal factor = BigDecimal.ONE.subtract(discountPercent.divide(new BigDecimal("100")));
        return normalPrice.multiply(factor).setScale(3, java.math.RoundingMode.HALF_UP);
    }
}
