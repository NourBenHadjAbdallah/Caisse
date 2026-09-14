package com.caisse.util;

import com.caisse.model.ReceiptRow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.List;

/** Turns a list of ReceiptRow into the on-screen (and print-job) JavaFX node. */
public final class ReceiptViewBuilder {

    private ReceiptViewBuilder() {}

    public static VBox build(List<ReceiptRow> rows) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(18, 16, 20, 16));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white;");

        for (ReceiptRow row : rows) {
            switch (row.getType()) {
                case TITLE, TEXT -> box.getChildren().add(mono(row.getText(), row.getSize(), row.isBold(), false));
                case TEXT_MUTED -> box.getChildren().add(mono(row.getText(), row.getSize(), row.isBold(), true));
                case TWO_COL -> box.getChildren().add(twoColRow(row.getLeft(), row.getRight(), row.getSize(), row.isBold()));
                case DASHED -> box.getChildren().add(dashedLine());
                case SPACER -> box.getChildren().add(spacer(row.getHeight()));
                case BARCODE -> box.getChildren().add(buildBarcode(row.getBarcodeSeed()));
            }
        }

        return box;
    }

    private static HBox twoColRow(String left, String right, double size, boolean bold) {
        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);
        Text leftText = mono(left, size, bold, false);
        Text rightText = mono(right, size, bold, false);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(leftText, spacer, rightText);
        return row;
    }

    private static VBox buildBarcode(String seed) {
        HBox bars = new HBox(0);
        bars.setAlignment(Pos.CENTER);
        bars.setPrefHeight(45);

        int hash = Math.abs(seed.hashCode());
        for (int i = 0; i < 40; i++) {
            int bit = (hash >> (i % 30)) & 1;
            double width = bit == 1 ? 3.0 : 1.5;
            Rectangle bar = new Rectangle(width, 40);
            bar.setFill(bit == 1 ? Color.BLACK : Color.web("#d1d5db"));
            HBox.setMargin(bar, new Insets(0, 0.5, 0, 0.5));
            bars.getChildren().add(bar);
            hash = hash * 31 + i;
        }

        VBox wrapper = new VBox(bars);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private static Text mono(String content, double size, boolean bold, boolean muted) {
        Text t = new Text(content == null ? "" : content);
        t.setFont(bold ? Font.font("Consolas", FontWeight.BOLD, size) : Font.font("Consolas", size));
        t.setTextAlignment(TextAlignment.CENTER);
        t.setFill(muted ? Color.web("#6b7280") : Color.web("#111827"));
        return t;
    }

    private static Region spacer(double height) {
        Region r = new Region();
        r.setPrefHeight(height);
        return r;
    }

    private static Text dashedLine() {
        return mono("--------------------------------", 10, false, true);
    }
}