package com.example;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfUAConformance;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.pdfua.PdfUAConfig;
import com.itextpdf.pdfua.PdfUADocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PdfUA {

    private static final String DEFAULT_LANGUAGE = "en-US";
    private static final String DEFAULT_TITLE = "Book Cover";
    private static final String DEFAULT_ALT_TEXT = "Book cover image";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o";

    private final HttpClient httpClient;
    private final Gson gson;
    private final String openAiApiKey;

    public PdfUA() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.openAiApiKey = System.getenv("OPENAI_API_KEY");
    }

    public PdfUA(String openAiApiKey) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.openAiApiKey = openAiApiKey;
    }

    public static void main(String[] args) throws Exception {
        Path sourceDir = Path.of("./src/main/resources/pdf");
        Path destDir = Path.of("./target/sandbox/pdfua");

        Files.createDirectories(destDir);

        PdfUA pdfUA = new PdfUA();

        if (pdfUA.openAiApiKey == null || pdfUA.openAiApiKey.isBlank()) {
            System.err.println("Warning: OPENAI_API_KEY environment variable not set. Using default alt text.");
        }

        List<Path> pdfFiles = pdfUA.findPdfFiles(sourceDir);

        System.out.println("Found " + pdfFiles.size() + " PDF files to process");

        int successCount = 0;
        int failCount = 0;

        for (Path sourcePdf : pdfFiles) {
            String fileName = sourcePdf.getFileName().toString();
            String outputFileName = fileName.replace(".pdf", "_cover.pdf");
            Path destPath = destDir.resolve(outputFileName);

            try {
                pdfUA.createCoverPdf(sourcePdf, destPath);
                System.out.println("✓ Processed: " + fileName + " -> " + outputFileName);
                successCount++;
            } catch (Exception e) {
                System.err.println("✗ Failed to process: " + fileName + " - " + e.getMessage());
                failCount++;
            }
        }

        System.out.println("\nProcessing complete: " + successCount + " succeeded, " + failCount + " failed");
    }

    public List<Path> findPdfFiles(Path directory) throws IOException {
        List<Path> pdfFiles = new ArrayList<>();

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IOException("Directory does not exist or is not a directory: " + directory);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.pdf")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    pdfFiles.add(entry);
                }
            }
        }

        return pdfFiles;
    }

    public void createCoverPdf(Path sourcePdfPath, Path destPath) throws IOException {
        try (PdfDocument sourcePdf = new PdfDocument(new PdfReader(sourcePdfPath.toFile()))) {
            String title = extractTitle(sourcePdf).orElse(DEFAULT_TITLE);
            String language = extractLanguage(sourcePdf).orElse(DEFAULT_LANGUAGE);

            createCoverPdf(sourcePdf, sourcePdfPath, destPath, title, language);
        }
    }

    public void createCoverPdf(Path sourcePdfPath, Path destPath,
                               String title, String language) throws IOException {
        try (PdfDocument sourcePdf = new PdfDocument(new PdfReader(sourcePdfPath.toFile()))) {
            createCoverPdf(sourcePdf, sourcePdfPath, destPath, title, language);
        }
    }

    private void createCoverPdf(PdfDocument sourcePdf, Path sourcePdfPath, Path destPath,
                                String title, String language) throws IOException {
        PdfPage firstPage = sourcePdf.getFirstPage();
        PageSize pageSize = new PageSize(firstPage.getPageSize());

        PdfImageXObject sourceImage = extractFirstImage(firstPage)
                .orElseThrow(() -> new ImageExtractionException(
                        "No image found on the first page: " + sourcePdfPath));

        // Generate alt text using OpenAI API
        String altText = generateAltText(sourceImage, title);

        writeCoverPdf(destPath, pageSize, sourceImage, title, language, altText);
    }

    private String generateAltText(PdfImageXObject image, String title) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return DEFAULT_ALT_TEXT;
        }

        try {
            // Convert image to PNG format for OpenAI compatibility
            byte[] pngBytes = convertToPng(image);
            String base64Image = Base64.getEncoder().encodeToString(pngBytes);

            return callOpenAiApi(base64Image, "image/png", title);
        } catch (Exception e) {
            System.err.println("Warning: Failed to generate alt text via OpenAI: " + e.getMessage());
            return DEFAULT_ALT_TEXT;
        }
    }

    /**
     * Converts a PDF image to PNG format.
     * This ensures compatibility with OpenAI's vision API which only supports
     * PNG, JPEG, GIF, and WebP formats.
     */
    private byte[] convertToPng(PdfImageXObject pdfImage) throws IOException {
        // Get the rendered BufferedImage from the PDF image
        BufferedImage bufferedImage = pdfImage.getBufferedImage();

        if (bufferedImage == null) {
            throw new IOException("Could not extract BufferedImage from PDF image");
        }

        // Convert to PNG
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean written = ImageIO.write(bufferedImage, "PNG", outputStream);

        if (!written) {
            throw new IOException("Failed to write image as PNG - no appropriate writer found");
        }

        return outputStream.toByteArray();
    }

    private String callOpenAiApi(String base64Image, String mimeType, String title) throws IOException, InterruptedException {
        JsonObject requestBody = buildOpenAiRequest(base64Image, mimeType, title);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API returned status " + response.statusCode() + ": " + response.body());
        }

        return parseOpenAiResponse(response.body());
    }

    private JsonObject buildOpenAiRequest(String base64Image, String mimeType, String title) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", OPENAI_MODEL);
        requestBody.addProperty("max_tokens", 300);

        JsonArray messages = new JsonArray();

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        JsonArray content = new JsonArray();

        // Text part with instructions
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", buildPrompt(title));
        content.add(textPart);

        // Image part
        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");
        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", "data:" + mimeType + ";base64," + base64Image);
        imageUrl.addProperty("detail", "low");
        imagePart.add("image_url", imageUrl);
        content.add(imagePart);

        userMessage.add("content", content);
        messages.add(userMessage);

        requestBody.add("messages", messages);

        return requestBody;
    }

    private String buildPrompt(String title) {
        return "Generate a concise alternative text description for this book cover image. " +
                "The book title is: \"" + title + "\". " +
                "The alt text should be suitable for screen readers and accessibility purposes. " +
                "Describe the key visual elements, colors, and any text visible on the cover. " +
                "Keep the description under 150 characters if possible. " +
                "Respond with only the alt text, no additional explanation.";
    }

    private String parseOpenAiResponse(String responseBody) {
        JsonObject response = gson.fromJson(responseBody, JsonObject.class);
        JsonArray choices = response.getAsJsonArray("choices");

        if (choices != null && choices.size() > 0) {
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            if (message != null) {
                String content = message.get("content").getAsString();
                return content.trim();
            }
        }

        return DEFAULT_ALT_TEXT;
    }

    private Optional<String> extractTitle(PdfDocument pdfDocument) {
        PdfDocumentInfo info = pdfDocument.getDocumentInfo();
        String title = info.getTitle();
        if (title != null && !title.isBlank()) {
            return Optional.of(title);
        }
        return Optional.empty();
    }

    private Optional<String> extractLanguage(PdfDocument pdfDocument) {
        // Try to get language from the catalog's Lang entry
        PdfString langString = pdfDocument.getCatalog().getPdfObject().getAsString(PdfName.Lang);
        if (langString != null) {
            String lang = langString.getValue();
            if (lang != null && !lang.isBlank()) {
                return Optional.of(lang);
            }
        }
        return Optional.empty();
    }

    private void writeCoverPdf(Path destPath, PageSize pageSize,
                               PdfImageXObject sourceImage,
                               String title, String language, String altText) throws IOException {
        PdfUAConfig config = new PdfUAConfig(PdfUAConformance.PDF_UA_1, title, language);

        try (PdfDocument pdfDoc = new PdfUADocument(new PdfWriter(destPath.toFile()), config);
             Document document = new Document(pdfDoc, pageSize)) {

            document.setMargins(0, 0, 0, 0);

            // Copy the image to the new document
            PdfImageXObject copiedImage = copyImageToDocument(sourceImage, pdfDoc);

            Image coverImage = createFullPageImage(copiedImage, pageSize, altText);
            document.add(coverImage);
        }
    }

    private PdfImageXObject copyImageToDocument(PdfImageXObject sourceImage, PdfDocument targetDoc) {
        PdfStream copiedStream = (PdfStream) sourceImage.getPdfObject().copyTo(targetDoc);
        return new PdfImageXObject(copiedStream);
    }

    private Image createFullPageImage(PdfImageXObject imageXObject, PageSize pageSize, String altText) {
        Image image = new Image(imageXObject);
        image.getAccessibilityProperties().setAlternateDescription(altText);
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