package com.example;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
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

    public static void main(String[] args) throws Exception {
        File file = new File(DEST);
        file.getParentFile().mkdirs();

        new PdfUA().manipulatePdf(DEST);
    }

    public void manipulatePdf(String dest) throws IOException {
        PdfDocument pdfDoc = new PdfUADocument(new PdfWriter(dest),
                new PdfUAConfig(PdfUAConformance.PDF_UA_1, "Some title", "en-US"));
        Document document = new Document(pdfDoc, PageSize.A4);

        Image img = new Image(ImageDataFactory.create(COVER));
        img.getAccessibilityProperties().setAlternateDescription("Cover");
        document.add(img);

        document.close();
    }

}