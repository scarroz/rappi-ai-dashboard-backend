package com.rappi.dashboard.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

public final class PdfExportUtil {

    public static final BaseColor RAPPI_RED = new BaseColor(255, 68, 31);
    public static final BaseColor RAPPI_LIGHT = new BaseColor(248, 247, 244);
    public static final BaseColor RAPPI_DARK = new BaseColor(26, 26, 26);
    public static final BaseColor RAPPI_GRAY = new BaseColor(102, 102, 102);
    public static final BaseColor RAPPI_BORDER = new BaseColor(230, 230, 230);

    public static final Font TITLE_FONT =
            new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, RAPPI_RED);

    public static final Font SUBTITLE_FONT =
            new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, RAPPI_GRAY);

    public static final Font SECTION_FONT =
            new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, RAPPI_DARK);

    public static final Font LABEL_FONT =
            new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, RAPPI_DARK);

    public static final Font BODY_FONT =
            new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, RAPPI_DARK);

    public static final Font SMALL_FONT =
            new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, RAPPI_GRAY);

    public static final Font TABLE_HEADER_FONT =
            new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

    public static final Font TABLE_CELL_FONT =
            new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, RAPPI_DARK);

    private PdfExportUtil() {
    }

    public static Document createDocument() {
        return new Document(PageSize.A4, 36, 36, 36, 36);
    }

    public static void addTitle(Document document, String title, String subtitle) throws DocumentException {
        Paragraph pTitle = new Paragraph(title, TITLE_FONT);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        pTitle.setSpacingAfter(5f);
        document.add(pTitle);

        Paragraph pSub = new Paragraph(subtitle, SUBTITLE_FONT);
        pSub.setAlignment(Element.ALIGN_CENTER);
        pSub.setSpacingAfter(16f);
        document.add(pSub);
    }

    public static void addSection(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION_FONT);
        p.setSpacingBefore(8f);
        p.setSpacingAfter(8f);
        document.add(p);
    }

    public static void addText(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, BODY_FONT);
        p.setLeading(15f);
        p.setSpacingAfter(6f);
        document.add(p);
    }

    public static void addBullet(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setLeading(15f);
        p.setSpacingAfter(4f);
        p.add(new Chunk("• ", LABEL_FONT));
        p.add(new Chunk(text, BODY_FONT));
        document.add(p);
    }

    public static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(RAPPI_RED);
        cell.setBorderColor(RAPPI_RED);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        return cell;
    }

    public static PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_CELL_FONT));
        cell.setBackgroundColor(RAPPI_LIGHT);
        cell.setBorderColor(RAPPI_BORDER);
        cell.setPadding(7f);
        return cell;
    }

    public static PdfPCell keyMetricCell(String title, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(title + "\n", LABEL_FONT));
        p.add(new Chunk(value, TITLE_FONT));

        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(RAPPI_LIGHT);
        cell.setBorderColor(RAPPI_BORDER);
        cell.setBorderWidth(1f);
        cell.setPadding(12f);
        cell.setMinimumHeight(70f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    public static PdfPTable createTable(float... widths) throws DocumentException {
        PdfPTable table = new PdfPTable(widths.length);
        table.setWidthPercentage(100f);
        table.setWidths(widths);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(12f);
        return table;
    }

    public static Rectangle createCardBorder() {
        Rectangle rectangle = new Rectangle(36, 36, 559, 806);
        rectangle.setBorder(Rectangle.BOX);
        rectangle.setBorderWidth(1f);
        rectangle.setBorderColor(RAPPI_BORDER);
        return rectangle;
    }
}