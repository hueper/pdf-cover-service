package com.example;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.ByteArrayOutputStream;

public class App {

    private static final int DEFAULT_PORT = 8080;
    private final PdfUA pdfUA;
    private final Javalin app;

    public App() {
        this.pdfUA = new PdfUA();
        this.app = createApp();
    }

    public App(String openAiApiKey) {
        this.pdfUA = new PdfUA(openAiApiKey);
        this.app = createApp();
    }

    private Javalin createApp() {
        return Javalin.create(config -> {
            config.http.maxRequestSize = 50_000_000L; // 50MB max upload
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        })
        .get("/", this::handleHealth)
        .get("/health", this::handleHealth)
        .post("/cover", this::handleCreateCover)
        .post("/metadata", this::handleExtractMetadata)
        .exception(PdfUA.ImageExtractionException.class, this::handleImageExtractionError)
        .exception(Exception.class, this::handleGenericError);
    }

    // ========== Endpoint Handlers ==========

    private void handleHealth(Context ctx) {
        ctx.json(new HealthResponse("ok", "PDF Cover Service is running"));
    }

    private void handleCreateCover(Context ctx) throws Exception {
        UploadedFile uploadedFile = ctx.uploadedFile("file");
        
        if (uploadedFile == null) {
            ctx.status(400).json(new ErrorResponse("error", "No file uploaded. Use multipart/form-data with field name 'file'"));
            return;
        }

        if (!uploadedFile.contentType().equals("application/pdf")) {
            ctx.status(400).json(new ErrorResponse("error", "File must be a PDF"));
            return;
        }

        // Optional parameters
        String title = ctx.formParam("title");
        String language = ctx.formParam("language");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        if (title != null || language != null) {
            pdfUA.createCoverPdf(uploadedFile.content(), outputStream, title, language);
        } else {
            pdfUA.createCoverPdf(uploadedFile.content(), outputStream);
        }

        String outputFilename = generateOutputFilename(uploadedFile.filename());
        
        ctx.contentType(ContentType.APPLICATION_PDF)
           .header("Content-Disposition", "attachment; filename=\"" + outputFilename + "\"")
           .result(outputStream.toByteArray());
    }

    private void handleExtractMetadata(Context ctx) throws Exception {
        UploadedFile uploadedFile = ctx.uploadedFile("file");
        
        if (uploadedFile == null) {
            ctx.status(400).json(new ErrorResponse("error", "No file uploaded. Use multipart/form-data with field name 'file'"));
            return;
        }

        byte[] pdfBytes = uploadedFile.content().readAllBytes();
        PdfUA.PdfMetadata metadata = pdfUA.extractMetadata(pdfBytes);
        
        ctx.json(new MetadataResponse(
                "ok",
                metadata.title(),
                metadata.language(),
                metadata.pageCount()
        ));
    }

    // ========== Error Handlers ==========

    private void handleImageExtractionError(PdfUA.ImageExtractionException e, Context ctx) {
        ctx.status(422).json(new ErrorResponse("error", e.getMessage()));
    }

    private void handleGenericError(Exception e, Context ctx) {
        ctx.status(500).json(new ErrorResponse("error", "Internal server error: " + e.getMessage()));
    }

    // ========== Utilities ==========

    private String generateOutputFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "cover.pdf";
        }
        if (originalFilename.toLowerCase().endsWith(".pdf")) {
            return originalFilename.substring(0, originalFilename.length() - 4) + "_cover.pdf";
        }
        return originalFilename + "_cover.pdf";
    }

    // ========== Server Lifecycle ==========

    public Javalin start(int port) {
        return app.start(port);
    }

    public void stop() {
        app.stop();
    }

    // ========== Main ==========

    public static void main(String[] args) {
        int port = getPort();
        
        App application = new App();
        application.start(port);

        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║       PDF Cover Service Started                   ║");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.println("║  Endpoints:                                       ║");
        System.out.println("║    GET  /health    - Health check                 ║");
        System.out.println("║    POST /cover     - Create cover PDF             ║");
        System.out.println("║    POST /metadata  - Extract PDF metadata         ║");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.printf("║  Server running on: http://localhost:%-14d║%n", port);
        System.out.println("╚═══════════════════════════════════════════════════╝");
        
        if (System.getenv("OPENAI_API_KEY") == null) {
            System.out.println("⚠  Warning: OPENAI_API_KEY not set - using default alt text");
        }
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable, using default: " + DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }

    // ========== Response DTOs ==========

    record HealthResponse(String status, String message) {}
    record ErrorResponse(String status, String error) {}
    record MetadataResponse(String status, String title, String language, int pageCount) {}
}