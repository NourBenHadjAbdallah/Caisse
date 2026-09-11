package com.caisse.model;

import java.math.BigDecimal;

public class TicketItem {
    private Product product;
    private int quantity;
    private BigDecimal unitPrice;      // final (discounted) unit price at time of sale
    private BigDecimal discountPercent;

    public TicketItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.discountPercent = product.getDiscountPercent();
        this.unitPrice = product.finalPrice();
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(3, java.math.RoundingMode.HALF_UP);
    }
}
