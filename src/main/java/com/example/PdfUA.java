package com.example;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfUAConformance;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.pdfua.PdfUAConfig;
import com.itextpdf.pdfua.PdfUADocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PdfUA {

    private static final String DEFAULT_LANGUAGE = "en-US";
    private static final String DEFAULT_TITLE = "Book Cover";
    private static final String COVER_ALT_TEXT = "Cover";

    public static void main(String[] args) throws Exception {
        Path dest = Path.of("./target/sandbox/pdfua/pdf_ua.pdf");
        Path source = Path.of("./src/main/resources/pdf/oa9783839431597.pdf");

        Files.createDirectories(dest.getParent());

        new PdfUA().createCoverPdf(source, dest);
    }

    public void createCoverPdf(Path sourcePdfPath, Path destPath) throws IOException {
        createCoverPdf(sourcePdfPath, destPath, DEFAULT_TITLE, DEFAULT_LANGUAGE);
    }

    public void createCoverPdf(Path sourcePdfPath, Path destPath,
                               String title, String language) throws IOException {
        try (PdfDocument sourcePdf = new PdfDocument(new PdfReader(sourcePdfPath.toFile()))) {
            PdfPage firstPage = sourcePdf.getFirstPage();
            PageSize pageSize = new PageSize(firstPage.getPageSize());

            PdfImageXObject sourceImage = extractFirstImage(firstPage)
                    .orElseThrow(() -> new ImageExtractionException(
                            "No image found on the first page: " + sourcePdfPath));

            writeCoverPdf(destPath, pageSize, sourceImage, title, language);
        }
    }

    private void writeCoverPdf(Path destPath, PageSize pageSize,
                               PdfImageXObject sourceImage,
                               String title, String language) throws IOException {
        PdfUAConfig config = new PdfUAConfig(PdfUAConformance.PDF_UA_1, title, language);

        try (PdfDocument pdfDoc = new PdfUADocument(new PdfWriter(destPath.toFile()), config);
             Document document = new Document(pdfDoc, pageSize)) {

            document.setMargins(0, 0, 0, 0);

            // Copy the image to the new document
            PdfImageXObject copiedImage = copyImageToDocument(sourceImage, pdfDoc);

            Image coverImage = createFullPageImage(copiedImage, pageSize);
            document.add(coverImage);
        }
    }

    private PdfImageXObject copyImageToDocument(PdfImageXObject sourceImage, PdfDocument targetDoc) {
        PdfStream copiedStream = (PdfStream) sourceImage.getPdfObject().copyTo(targetDoc);
        return new PdfImageXObject(copiedStream);
    }

    private Image createFullPageImage(PdfImageXObject imageXObject, PageSize pageSize) {
        Image image = new Image(imageXObject);
        image.getAccessibilityProperties().setAlternateDescription(COVER_ALT_TEXT);
        image.scaleToFit(pageSize.getWidth(), pageSize.getHeight());
        image.setFixedPosition(0, 0);
        return image;
    }

    private Optional<PdfImageXObject> extractFirstImage(PdfPage page) {
        var xObjects = page.getResources().getResource(PdfName.XObject);
        if (xObjects == null) {
            return Optional.empty();
        }

        for (PdfName name : xObjects.keySet()) {
            PdfStream stream = xObjects.getAsStream(name);
            if (isImageStream(stream)) {
                return Optional.of(new PdfImageXObject(stream));
            }
        }
        return Optional.empty();
    }

    private boolean isImageStream(PdfStream stream) {
        return stream != null && PdfName.Image.equals(stream.getAsName(PdfName.Subtype));
    }

    public static class ImageExtractionException extends IOException {
        public ImageExtractionException(String message) {
            super(message);
        }
    }
}