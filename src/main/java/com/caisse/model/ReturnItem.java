package com.caisse.model;

import java.math.BigDecimal;

public class ReturnItem {
    private Product product;
    private int quantity;
    private BigDecimal unitPrice;

    public ReturnItem(Product product, int quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(3, java.math.RoundingMode.HALF_UP);
    }
}
