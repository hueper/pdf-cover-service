# PDF Cover Service

Extract accessible, PDF/UA-compliant cover pages from PDF files. Automatically generates alt text for cover images using OpenAI's vision API.

**Demo:** https://pdf-cover.onrender.com/

## Features

- Extracts first-page image from PDFs and creates a standalone cover
- PDF/UA-1 compliant output for accessibility
- AI-generated alt text via OpenAI GPT-4o (optional)
- REST API with health check, cover generation, and metadata extraction
- React frontend with drag & drop, batch processing

## Quick Start

### Backend (Java)

**Prerequisites:** Java 21, Maven 3.9+

```bash
cd backend
mvn compile exec:java
```

Server runs on `http://localhost:8080`

**With Docker:**

```bash
cd backend
docker build -t pdf-cover-service .
docker run -p 8080:8080 -e OPENAI_API_KEY=your-key pdf-cover-service
```

### Frontend (React)

**Prerequisites:** Node.js 18+, npm or Yarn

```bash
cd frontend
npm install
npm run dev
```

Opens at `http://localhost:5173`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| POST | `/cover` | Create cover PDF (multipart: `file`, optional `title`, `language`) |
| POST | `/metadata` | Extract PDF metadata |

**Example:**

```bash
curl -X POST -F "file=@book.pdf" http://localhost:8080/cover -o cover.pdf
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8080` |
| `OPENAI_API_KEY` | OpenAI API key for alt text generation | *(uses default alt text if unset)* |

## Deploy to Render

### Backend (Web Service)

1. Create a **Web Service** connected to your repo
2. Set **Root Directory:** `backend`
3. Set **Runtime:** Docker
4. Add environment variable: `OPENAI_API_KEY` (optional)

### Frontend (Static Site)

1. Create a **Static Site** connected to your repo
2. Set **Root Directory:** `frontend`
3. Set **Build Command:** `npm install && npm run build`
4. Set **Publish Directory:** `dist`
5. Update `DEFAULT_API_URL` in `frontend/src/App.jsx` to your backend URL

## Project Structure

```
pdf-cover-service/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/
│       ├── App.java          # Javalin HTTP server
│       └── PdfUA.java        # PDF processing & OpenAI integration
└── frontend/
    ├── package.json
    └── src/
        └── App.jsx           # React UI
```

## License

This project is dual-licensed:

- **Backend** (`/backend`): Licensed under **AGPL-3.0** due to its dependency on [iText Core](https://itextpdf.com/). For commercial use without AGPL obligations, contact iText for a commercial license.

- **Frontend** (`/frontend`): Licensed under **MIT**.
