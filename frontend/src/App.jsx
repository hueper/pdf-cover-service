import { useState, useCallback } from 'react'
import {
  ThemeProvider,
  createTheme,
  CssBaseline,
  Container,
  Box,
  Typography,
  Button,
  Paper,
  LinearProgress,
  Alert,
  IconButton,
  Chip,
  TextField,
  Collapse,
  Link,
} from '@mui/material'
import {
  CloudUpload,
  Download,
  Delete,
  PictureAsPdf,
  CheckCircle,
  Error as ErrorIcon,
  Settings,
  ExpandMore,
  ExpandLess,
  GitHub
} from '@mui/icons-material'

// Dark theme with custom palette
const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#e94560',
      light: '#ff6b8a',
      dark: '#b8304a',
    },
    secondary: {
      main: '#0f3460',
      light: '#1a4a80',
      dark: '#0a2040',
    },
    background: {
      default: '#0a0a0f',
      paper: '#1a1a25',
    },
    text: {
      primary: '#f5f5f7',
      secondary: '#a0a0b0',
    },
  },
  typography: {
    fontFamily: '"DM Sans", sans-serif',
    h1: {
      fontFamily: '"Fraunces", serif',
      fontWeight: 700,
    },
    h2: {
      fontFamily: '"Fraunces", serif',
      fontWeight: 600,
    },
    h3: {
      fontFamily: '"Fraunces", serif',
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          padding: '12px 24px',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
  },
})

// Default API URL - change this or use environment variable
const DEFAULT_API_URL = 'https://pdf-cover-service.onrender.com'

function App() {
  const [files, setFiles] = useState([])
  const [apiUrl, setApiUrl] = useState(DEFAULT_API_URL)
  const [showSettings, setShowSettings] = useState(false)
  const [dragActive, setDragActive] = useState(false)

  const handleDrag = useCallback((e) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }, [])

  const processFiles = useCallback((fileList) => {
    const pdfFiles = Array.from(fileList).filter(
      (file) => file.type === 'application/pdf'
    )
    const newFiles = pdfFiles.map((file) => ({
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      file,
      name: file.name,
      size: file.size,
      status: 'pending', // pending, uploading, success, error
      progress: 0,
      result: null,
      error: null,
    }))
    setFiles((prev) => [...prev, ...newFiles])
  }, [])

  const handleDrop = useCallback(
    (e) => {
      e.preventDefault()
      e.stopPropagation()
      setDragActive(false)
      if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
        processFiles(e.dataTransfer.files)
      }
    },
    [processFiles]
  )

  const handleFileInput = useCallback(
    (e) => {
      if (e.target.files && e.target.files.length > 0) {
        processFiles(e.target.files)
      }
    },
    [processFiles]
  )

  const uploadFile = useCallback(
    async (fileItem) => {
      setFiles((prev) =>
        prev.map((f) =>
          f.id === fileItem.id ? { ...f, status: 'uploading', progress: 0 } : f
        )
      )

      try {
        const formData = new FormData()
        formData.append('file', fileItem.file)

        const response = await fetch(`${apiUrl}/cover`, {
          method: 'POST',
          body: formData,
        })

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({}))
          throw new Error(errorData.error || `HTTP ${response.status}`)
        }

        const blob = await response.blob()
        const url = URL.createObjectURL(blob)
        const outputName = fileItem.name.replace('.pdf', '_cover.pdf')

        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileItem.id
              ? {
                  ...f,
                  status: 'success',
                  progress: 100,
                  result: { url, name: outputName },
                }
              : f
          )
        )
      } catch (error) {
        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileItem.id
              ? { ...f, status: 'error', error: error.message }
              : f
          )
        )
      }
    },
    [apiUrl]
  )

  const uploadAll = useCallback(() => {
    files
      .filter((f) => f.status === 'pending')
      .forEach((f) => uploadFile(f))
  }, [files, uploadFile])

  const downloadFile = useCallback((result) => {
    const a = document.createElement('a')
    a.href = result.url
    a.download = result.name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  }, [])

  const downloadAll = useCallback(() => {
    files
      .filter((f) => f.status === 'success' && f.result)
      .forEach((f) => downloadFile(f.result))
  }, [files, downloadFile])

  const removeFile = useCallback((id) => {
    setFiles((prev) => {
      const file = prev.find((f) => f.id === id)
      if (file?.result?.url) {
        URL.revokeObjectURL(file.result.url)
      }
      return prev.filter((f) => f.id !== id)
    })
  }, [])

  const clearAll = useCallback(() => {
    files.forEach((f) => {
      if (f.result?.url) {
        URL.revokeObjectURL(f.result.url)
      }
    })
    setFiles([])
  }, [files])

  const formatSize = (bytes) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  const pendingCount = files.filter((f) => f.status === 'pending').length
  const successCount = files.filter((f) => f.status === 'success').length
  const uploadingCount = files.filter((f) => f.status === 'uploading').length

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box
        sx={{
          minHeight: '100vh',
          background: `
            radial-gradient(ellipse at 20% 0%, rgba(233, 69, 96, 0.15) 0%, transparent 50%),
            radial-gradient(ellipse at 80% 100%, rgba(15, 52, 96, 0.2) 0%, transparent 50%),
            #0a0a0f
          `,
          py: 6,
        }}
      >
        <Container maxWidth="md">
          {/* Header */}
          <Box sx={{ textAlign: 'center', mb: 6 }} className="fade-in-up">
            <Typography
              variant="h2"
              component="h1"
              sx={{
                mb: 2,
                background: 'linear-gradient(135deg, #f5f5f7 0%, #a0a0b0 100%)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}
            >
              PDF Cover Extractor
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              Upload PDF files to extract accessible cover pages with PDF/UA compliance
            </Typography>

            {/* Settings Toggle */}
            <Button
              size="small"
              startIcon={<Settings />}
              endIcon={showSettings ? <ExpandLess /> : <ExpandMore />}
              onClick={() => setShowSettings(!showSettings)}
              sx={{ color: 'text.secondary' }}
            >
              API Settings
            </Button>
            <Collapse in={showSettings}>
              <Box sx={{ mt: 2, maxWidth: 500, mx: 'auto' }}>
                <TextField
                  fullWidth
                  size="small"
                  label="API URL"
                  value={apiUrl}
                  onChange={(e) => setApiUrl(e.target.value)}
                  placeholder="https://your-service.onrender.com"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      backgroundColor: 'rgba(255,255,255,0.03)',
                    },
                  }}
                />
              </Box>
            </Collapse>
          </Box>

          {/* Drop Zone */}
          <Paper
            className="fade-in-up stagger-1"
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            sx={{
              p: 6,
              mb: 4,
              textAlign: 'center',
              border: '2px dashed',
              borderColor: dragActive ? 'primary.main' : 'divider',
              backgroundColor: dragActive
                ? 'rgba(233, 69, 96, 0.08)'
                : 'rgba(255, 255, 255, 0.02)',
              transition: 'all 0.3s ease',
              cursor: 'pointer',
              '&:hover': {
                borderColor: 'primary.light',
                backgroundColor: 'rgba(233, 69, 96, 0.05)',
              },
            }}
            onClick={() => document.getElementById('file-input').click()}
          >
            <input
              id="file-input"
              type="file"
              accept="application/pdf"
              multiple
              onChange={handleFileInput}
              style={{ display: 'none' }}
            />
            <CloudUpload
              sx={{
                fontSize: 64,
                color: dragActive ? 'primary.main' : 'text.secondary',
                mb: 2,
                transition: 'color 0.3s ease',
              }}
            />
            <Typography variant="h6" gutterBottom>
              {dragActive ? 'Drop your PDFs here' : 'Drag & drop PDF files here'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              or click to browse • Multiple files supported
            </Typography>
          </Paper>

          {/* Action Buttons */}
          {files.length > 0 && (
            <Box
              className="fade-in-up stagger-2"
              sx={{
                display: 'flex',
                gap: 2,
                mb: 4,
                flexWrap: 'wrap',
                justifyContent: 'center',
              }}
            >
              <Button
                variant="contained"
                onClick={uploadAll}
                disabled={pendingCount === 0 || uploadingCount > 0}
                startIcon={<CloudUpload />}
              >
                Process {pendingCount > 0 ? `(${pendingCount})` : 'All'}
              </Button>
              <Button
                variant="outlined"
                onClick={downloadAll}
                disabled={successCount === 0}
                startIcon={<Download />}
              >
                Download All ({successCount})
              </Button>
              <Button
                variant="outlined"
                color="error"
                onClick={clearAll}
                startIcon={<Delete />}
              >
                Clear All
              </Button>
            </Box>
          )}

          {/* File List */}
          <Box className="fade-in-up stagger-3">
            {files.map((fileItem) => (
              <Paper
                key={fileItem.id}
                sx={{
                  p: 2,
                  mb: 2,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 2,
                  backgroundColor: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid',
                  borderColor:
                    fileItem.status === 'success'
                      ? 'success.dark'
                      : fileItem.status === 'error'
                      ? 'error.dark'
                      : 'divider',
                }}
              >
                <PictureAsPdf
                  sx={{
                    fontSize: 40,
                    color:
                      fileItem.status === 'success'
                        ? 'success.main'
                        : fileItem.status === 'error'
                        ? 'error.main'
                        : 'primary.main',
                  }}
                />

                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography
                    variant="body1"
                    sx={{
                      fontWeight: 500,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {fileItem.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {formatSize(fileItem.size)}
                  </Typography>

                  {fileItem.status === 'uploading' && (
                    <LinearProgress
                      sx={{ mt: 1, borderRadius: 1 }}
                      color="primary"
                    />
                  )}

                  {fileItem.status === 'error' && (
                    <Alert severity="error" sx={{ mt: 1, py: 0 }}>
                      {fileItem.error}
                    </Alert>
                  )}
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  {fileItem.status === 'pending' && (
                    <Chip label="Pending" size="small" variant="outlined" />
                  )}
                  {fileItem.status === 'uploading' && (
                    <Chip
                      label="Processing..."
                      size="small"
                      color="primary"
                      variant="outlined"
                    />
                  )}
                  {fileItem.status === 'success' && (
                    <>
                      <CheckCircle color="success" />
                      <IconButton
                        color="primary"
                        onClick={() => downloadFile(fileItem.result)}
                        title="Download"
                      >
                        <Download />
                      </IconButton>
                    </>
                  )}
                  {fileItem.status === 'error' && <ErrorIcon color="error" />}

                  <IconButton
                    onClick={() => removeFile(fileItem.id)}
                    title="Remove"
                    sx={{ color: 'text.secondary' }}
                  >
                    <Delete />
                  </IconButton>
                </Box>
              </Paper>
            ))}
          </Box>

          {/* Footer */}
          <Box sx={{ textAlign: 'center', mt: 6, color: 'text.secondary' }}>
            <Link
              href="https://github.com/hueper/pdf-cover-service"
              target="_blank"
              rel="noopener"
              sx={{ 
                color: 'text.secondary',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 1,
                textDecoration: 'none',
                '&:hover': {
                  color: 'text.primary',
                },
              }}
            >
              <GitHub />
              <Typography variant="body2" component="span">
                View on GitHub
              </Typography>
            </Link>
          </Box>
        </Container>
      </Box>
    </ThemeProvider>
  )
}

export default App
