package com.caisse.util;

import com.caisse.model.ReceiptRow;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Renders the same ReceiptRow list used on screen into a real PDF file. */
public final class ReceiptPdfBuilder {

    private ReceiptPdfBuilder() {}

    private static final float MARGIN = 40f;
    private static final float LINE_GAP = 4f;

    public static void save(List<ReceiptRow> rows, File file) throws IOException {
        try (PDDocument document = new PDDocument()) {

            PDFont regular = PDType1Font.COURIER;
            PDFont bold = PDType1Font.COURIER_BOLD;

            PDPage page = new PDPage(PDRectangle.A5);
            document.addPage(page);

            PDPageContentStream stream = new PDPageContentStream(document, page);
            float pageWidth = page.getMediaBox().getWidth();
            float y = page.getMediaBox().getHeight() - MARGIN;

            for (ReceiptRow row : rows) {

                if (y < MARGIN + 20) {
                    stream.close();
                    page = new PDPage(PDRectangle.A5);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - MARGIN;
                }

                switch (row.getType()) {

                    case TITLE, TEXT -> {
                        PDFont font = row.isBold() ? bold : regular;
                        float size = (float) row.getSize();
                        String text = row.getText() == null ? "" : row.getText();
                        float textWidth = font.getStringWidth(text) / 1000f * size;
                        float x = (pageWidth - textWidth) / 2f;

                        stream.setNonStrokingColor(0.07f, 0.09f, 0.15f);
                        stream.beginText();
                        stream.setFont(font, size);
                        stream.newLineAtOffset(x, y);
                        stream.showText(text);
                        stream.endText();
                        y -= size + LINE_GAP;
                    }

                    case TEXT_MUTED -> {
                        float size = (float) row.getSize();
                        String text = row.getText() == null ? "" : row.getText();
                        float textWidth = regular.getStringWidth(text) / 1000f * size;
                        float x = (pageWidth - textWidth) / 2f;

                        stream.setNonStrokingColor(0.42f, 0.45f, 0.5f);
                        stream.beginText();
                        stream.setFont(regular, size);
                        stream.newLineAtOffset(x, y);
                        stream.showText(text);
                        stream.endText();
                        stream.setNonStrokingColor(0.07f, 0.09f, 0.15f);
                        y -= size + LINE_GAP;
                    }

                    case TWO_COL -> {
                        PDFont font = row.isBold() ? bold : regular;
                        float size = (float) row.getSize();
                        String left = row.getLeft() == null ? "" : row.getLeft();
                        String right = row.getRight() == null ? "" : row.getRight();

                        stream.setNonStrokingColor(0.07f, 0.09f, 0.15f);

                        stream.beginText();
                        stream.setFont(font, size);
                        stream.newLineAtOffset(MARGIN, y);
                        stream.showText(left);
                        stream.endText();

                        float rightWidth = font.getStringWidth(right) / 1000f * size;
                        stream.beginText();
                        stream.setFont(font, size);
                        stream.newLineAtOffset(pageWidth - MARGIN - rightWidth, y);
                        stream.showText(right);
                        stream.endText();

                        y -= size + LINE_GAP;
                    }

                    case DASHED -> {
                        float size = 10f;
                        String text = "--------------------------------";
                        float textWidth = regular.getStringWidth(text) / 1000f * size;
                        float x = (pageWidth - textWidth) / 2f;

                        stream.setNonStrokingColor(0.6f, 0.63f, 0.68f);
                        stream.beginText();
                        stream.setFont(regular, size);
                        stream.newLineAtOffset(x, y);
                        stream.showText(text);
                        stream.endText();
                        stream.setNonStrokingColor(0.07f, 0.09f, 0.15f);
                        y -= size + LINE_GAP;
                    }

                    case SPACER -> y -= row.getHeight();

                    case BARCODE -> {
                        int hash = Math.abs(row.getBarcodeSeed().hashCode());
                        float barHeight = 30f;
                        float x = pageWidth / 2f - 70f;

                        stream.setNonStrokingColor(0f, 0f, 0f);
                        for (int i = 0; i < 40; i++) {
                            int bit = (hash >> (i % 30)) & 1;
                            float w = bit == 1 ? 2.2f : 1.1f;
                            if (bit == 1) {
                                stream.addRect(x, y - barHeight, w, barHeight);
                                stream.fill();
                            }
                            x += w + 0.8f;
                            hash = hash * 31 + i;
                        }
                        y -= barHeight + LINE_GAP;
                    }
                }
            }

            stream.close();
            document.save(file);
        }
    }
}