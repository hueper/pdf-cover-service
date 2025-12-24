package com.example;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfUAConformance;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.pdfua.PdfUAConfig;
import com.itextpdf.pdfua.PdfUADocument;

import java.io.File;
import java.io.IOException;

public class PdfUA {
    public static final String DEST = "./target/sandbox/pdfua/pdf_ua.pdf";
    public static final String COVER = "./src/main/resources/img/image.png";
    public static final String SOURCE_PDF = "./src/main/resources/pdf/oa9783839431597.pdf";

    public static void main(String[] args) throws Exception {
        File file = new File(DEST);
        file.getParentFile().mkdirs();
        new PdfUA().manipulatePdf(DEST);
    }

    public void manipulatePdf(String dest) throws IOException {
        // Read page size from the source PDF
        PageSize pageSize = getPageSizeFromPdf(SOURCE_PDF);

        PdfDocument pdfDoc = new PdfUADocument(new PdfWriter(dest),
                new PdfUAConfig(PdfUAConformance.PDF_UA_1, "Some title", "en-US"));
        Document document = new Document(pdfDoc, pageSize);

        // Remove margins
        document.setMargins(0, 0, 0, 0);

        Image img = new Image(ImageDataFactory.create(COVER));
        img.getAccessibilityProperties().setAlternateDescription("Cover");

        // Scale image to fit the full page
        img.scaleToFit(pageSize.getWidth(), pageSize.getHeight());
        img.setFixedPosition(0, 0);

        document.add(img);

        document.close();
    }

    private PageSize getPageSizeFromPdf(String pdfPath) throws IOException {
        try (PdfDocument sourcePdf = new PdfDocument(new PdfReader(pdfPath))) {
            Rectangle rect = sourcePdf.getFirstPage().getPageSize();
            return new PageSize(rect);
        }
    }
}