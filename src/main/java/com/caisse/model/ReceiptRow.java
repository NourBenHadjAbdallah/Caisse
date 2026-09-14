package com.caisse.model;

/** One line of a printable/exportable receipt. Shared by the on-screen preview, the printer, and the PDF export. */
public class ReceiptRow {

    public enum Type { TITLE, TEXT, TEXT_MUTED, TWO_COL, DASHED, SPACER, BARCODE }

    private final Type type;
    private final String text;
    private final String left;
    private final String right;
    private final double size;
    private final boolean bold;
    private final double height;
    private final String barcodeSeed;

    private ReceiptRow(Type type, String text, String left, String right,
                        double size, boolean bold, double height, String barcodeSeed) {
        this.type = type;
        this.text = text;
        this.left = left;
        this.right = right;
        this.size = size;
        this.bold = bold;
        this.height = height;
        this.barcodeSeed = barcodeSeed;
    }

    public static ReceiptRow title(String text, double size) {
        return new ReceiptRow(Type.TITLE, text, null, null, size, true, 0, null);
    }

    public static ReceiptRow text(String text, double size, boolean bold) {
        return new ReceiptRow(Type.TEXT, text, null, null, size, bold, 0, null);
    }

    public static ReceiptRow textMuted(String text, double size) {
        return new ReceiptRow(Type.TEXT_MUTED, text, null, null, size, false, 0, null);
    }

    public static ReceiptRow twoCol(String left, String right, double size, boolean bold) {
        return new ReceiptRow(Type.TWO_COL, null, left, right, size, bold, 0, null);
    }

    public static ReceiptRow dashed() {
        return new ReceiptRow(Type.DASHED, null, null, null, 10, false, 0, null);
    }

    public static ReceiptRow spacer(double height) {
        return new ReceiptRow(Type.SPACER, null, null, null, 0, false, height, null);
    }

    public static ReceiptRow barcode(String seed) {
        return new ReceiptRow(Type.BARCODE, null, null, null, 0, false, 45, seed);
    }

    public Type getType() { return type; }
    public String getText() { return text; }
    public String getLeft() { return left; }
    public String getRight() { return right; }
    public double getSize() { return size; }
    public boolean isBold() { return bold; }
    public double getHeight() { return height; }
    public String getBarcodeSeed() { return barcodeSeed; }
}