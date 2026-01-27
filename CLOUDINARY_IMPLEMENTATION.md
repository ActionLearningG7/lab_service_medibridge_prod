# Cloudinary Lab Report Storage - Implementation Guide

## Overview
This implementation adds secure file storage for lab reports using Cloudinary, with healthcare-grade security ensuring only authorized users can access reports.

## Features Implemented

### ✅ Cloudinary Integration
- **CloudinaryConfig**: Spring bean configuration with environment variable support
- **ReportStorageClient**: Abstraction layer for storage operations
- **CloudinaryReportStorageClient**: Cloudinary implementation with:
  - PDF and image upload support
  - SHA-256 checksum computation
  - Organized folder structure: `{base}/{year}/{month}/{orderNumber}/`
  - Deterministic file naming
  - Healthcare-grade security (authenticated access mode)

### ✅ Database Schema Updates
**LabReport Entity** now includes:
- `reportUrl`: Secure HTTPS URL from Cloudinary
- `cloudinaryPublicId`: For resource management
- `reportMimeType`: File content type
- `reportSizeBytes`: File size
- `checksum`: SHA-256 for integrity verification
- `originalFilename`: User-uploaded filename
- `resourceType`: Cloudinary resource type (raw/image)
- `format`: File format (pdf, png, jpg)

### ✅ API Endpoints

#### ADMIN Endpoints
1. **Upload Report**
   ```
   POST /api/v1/admin/lab/lab-orders/{orderId}/report/upload
   Content-Type: multipart/form-data
   Authorization: Bearer {ADMIN_TOKEN}
   ```
   - Uploads file to Cloudinary
   - Creates LabReport entity in DRAFT status
   - Returns metadata (no URL for DRAFT)

2. **Publish Report**
   ```
   POST /api/v1/admin/lab/lab-orders/{orderId}/report/publish
   Authorization: Bearer {ADMIN_TOKEN}
   ```
   - Changes status from DRAFT to PUBLISHED
   - Makes report accessible to patient/doctor
   - Sends Kafka event
   - Returns metadata with secure URL

3. **Get Report (Any Status)**
   ```
   GET /api/v1/admin/lab/lab-orders/{orderId}/report
   Authorization: Bearer {ADMIN_TOKEN}
   ```
   - Admin can view reports in any status

#### PATIENT Endpoints
4. **Get My Report**
   ```
   GET /api/v1/lab-orders/{orderId}/report
   Authorization: Bearer {PATIENT_TOKEN}
   ```
   - Only returns PUBLISHED reports
   - Only if patient owns the order
   - Returns secure URL for download

#### DOCTOR Endpoints
5. **Get Report for My Order**
   ```
   GET /api/v1/doctors/lab-orders/{orderId}/report
   Authorization: Bearer {DOCTOR_TOKEN}
   ```
   - Only returns PUBLISHED reports
   - Only if doctor created the order
   - Returns secure URL for download

### ✅ Security Implementation

#### Authorization Matrix
| Role | Upload | Publish | View Draft | View Published |
|------|--------|---------|------------|----------------|
| ADMIN | ✓ | ✓ | ✓ | ✓ |
| DOCTOR | ✗ | ✗ | ✗ | ✓ (own orders) |
| PATIENT | ✗ | ✗ | ✗ | ✓ (own orders) |

#### Security Features
- ✓ Environment variables only (no hardcoded secrets)
- ✓ Dotenv support for local development
- ✓ Cloudinary authenticated access mode
- ✓ Role-based access control (@PreAuthorize)
- ✓ Ownership verification for patients/doctors
- ✓ DRAFT reports not accessible to patients/doctors
- ✓ SHA-256 checksum for integrity
- ✓ Secure URL logging (never log query params)

### ✅ File Validation
- Maximum file size: 10MB (configurable)
- Allowed types: PDF, PNG, JPEG, JPG
- Extension validation
- MIME type validation
- Content integrity check (checksum)

### ✅ Error Handling
Custom exceptions with proper HTTP status codes:
- `InvalidFileException` → 400 Bad Request
- `CloudStorageException` → 500 Internal Server Error
- `AccessDeniedException` → 403 Forbidden
- `EntityNotFoundException` → 404 Not Found

## Environment Variables

### Required (Production)
```bash
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

### Optional (with defaults)
```bash
CLOUDINARY_FOLDER=medibridge/lab-reports
CLOUDINARY_RESOURCE_TYPE=auto
CLOUDINARY_SECURE=true
MAX_FILE_SIZE_MB=10
ALLOWED_MIME_TYPES=application/pdf,image/png,image/jpeg,image/jpg
```

## Local Development Setup

### 1. Create .env file
```bash
cd lab_service_medibridge
cp .env.example .env
```

### 2. Add Cloudinary Credentials
Edit `.env` and add your Cloudinary account credentials:
```bash
CLOUDINARY_CLOUD_NAME=your_actual_cloud_name
CLOUDINARY_API_KEY=your_actual_api_key
CLOUDINARY_API_SECRET=your_actual_api_secret
```

### 3. Get Cloudinary Credentials
- Sign up at https://cloudinary.com (free tier available)
- Go to Dashboard
- Copy Cloud Name, API Key, and API Secret

### 4. Run the Service
```bash
./mvnw spring-boot:run
```

The service will:
- Load .env file in dev profile
- Initialize Cloudinary bean
- Fail fast if credentials missing in production

## Testing

### Upload Report (Admin)
```bash
curl -X POST http://localhost:8085/api/v1/admin/lab/lab-orders/{orderId}/report/upload \
  -H "Authorization: Bearer {ADMIN_TOKEN}" \
  -F "file=@/path/to/report.pdf"
```

### Publish Report (Admin)
```bash
curl -X POST http://localhost:8085/api/v1/admin/lab/lab-orders/{orderId}/report/publish \
  -H "Authorization: Bearer {ADMIN_TOKEN}"
```

### Get Report (Patient)
```bash
curl -X GET http://localhost:8085/api/v1/lab-orders/{orderId}/report \
  -H "Authorization: Bearer {PATIENT_TOKEN}"
```

### Get Report (Doctor)
```bash
curl -X GET http://localhost:8085/api/v1/doctors/lab-orders/{orderId}/report \
  -H "Authorization: Bearer {DOCTOR_TOKEN}"
```

## Database Migration

Run this SQL to update existing tables:
```sql
ALTER TABLE lab_reports 
  ADD COLUMN cloudinary_public_id VARCHAR(255),
  ADD COLUMN report_mime_type VARCHAR(100),
  ADD COLUMN report_size_bytes BIGINT,
  ADD COLUMN original_filename VARCHAR(255),
  ADD COLUMN resource_type VARCHAR(20),
  ADD COLUMN format VARCHAR(20),
  MODIFY COLUMN report_url VARCHAR(500),
  MODIFY COLUMN checksum VARCHAR(64);

-- Add index for better query performance
CREATE INDEX idx_report_status ON lab_reports(status);
```

Or let Hibernate auto-update (ddl-auto: update is enabled).

## Folder Structure on Cloudinary
```
medibridge/lab-reports/
  └── 2026/
      └── 01/
          └── ORDER-123/
              ├── lab_ORDER-123_1737633600000.pdf
              └── lab_ORDER-123_1737637200000.png
```

## Response Examples

### Upload Response (DRAFT)
```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440000",
  "labOrderId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "DRAFT",
  "reportUrl": null,
  "mimeType": "application/pdf",
  "sizeBytes": 1024000,
  "format": "pdf",
  "originalFilename": "blood_test_results.pdf",
  "checksum": "a3b2c1d4e5f6...",
  "createdAt": "2026-01-23T10:00:00"
}
```

### Publish Response (PUBLISHED)
```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440000",
  "labOrderId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "PUBLISHED",
  "reportUrl": "https://res.cloudinary.com/your-cloud/raw/upload/v1234567890/medibridge/lab-reports/2026/01/ORDER-123/lab_ORDER-123_1737633600000.pdf",
  "mimeType": "application/pdf",
  "sizeBytes": 1024000,
  "format": "pdf",
  "originalFilename": "blood_test_results.pdf",
  "checksum": "a3b2c1d4e5f6...",
  "publishedAt": "2026-01-23T10:05:00",
  "createdAt": "2026-01-23T10:00:00"
}
```

## Kafka Events

When a report is published, a Kafka event is sent:
```json
{
  "reportId": "550e8400-e29b-41d4-a716-446655440000",
  "labOrderId": "550e8400-e29b-41d4-a716-446655440001",
  "patientId": "patient-uuid",
  "doctorId": "doctor-uuid",
  "publishedAt": "2026-01-23T10:05:00"
}
```

Topic: `lab-report-published`

## Production Deployment

### Railway / Heroku / AWS
Set environment variables in platform dashboard:
```
CLOUDINARY_CLOUD_NAME=prod_cloud_name
CLOUDINARY_API_KEY=prod_api_key
CLOUDINARY_API_SECRET=prod_api_secret
```

### Docker
```bash
docker run -e CLOUDINARY_CLOUD_NAME=xxx \
  -e CLOUDINARY_API_KEY=xxx \
  -e CLOUDINARY_API_SECRET=xxx \
  lab-service:latest
```

### Kubernetes Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: cloudinary-credentials
type: Opaque
data:
  cloud-name: <base64-encoded>
  api-key: <base64-encoded>
  api-secret: <base64-encoded>
```

## Troubleshooting

### Issue: Cloudinary credentials not configured
**Error**: "Cloudinary credentials not fully configured in dev"
**Solution**: Create .env file with valid credentials

### Issue: File size too large
**Error**: "File size exceeds maximum allowed 10 MB"
**Solution**: Increase MAX_FILE_SIZE_MB or compress file

### Issue: Invalid file type
**Error**: "File type not allowed"
**Solution**: Only PDF, PNG, JPEG, JPG are supported

### Issue: Report not found
**Error**: "Report not found for lab order"
**Solution**: Upload report first before trying to publish/retrieve

### Issue: Access denied
**Error**: "You don't have permission to access this report"
**Solution**: Ensure user owns the order (patient) or created it (doctor)

## Next Steps / Future Enhancements

1. **Signed URLs**: Generate time-limited signed URLs for extra security
2. **Thumbnails**: Auto-generate thumbnails for image reports
3. **OCR**: Extract text from PDFs for search
4. **Version Control**: Support multiple versions of same report
5. **Bulk Upload**: Upload multiple reports at once
6. **Report Templates**: Pre-defined report templates
7. **Watermarking**: Add patient/doctor info watermark
8. **Encryption**: Encrypt files before upload (client-side)

## Support

For issues or questions:
- Check logs: `tail -f logs/lab-service.log`
- Verify Cloudinary dashboard for uploaded files
- Test with small PDF file first
- Ensure JWT token is valid and has correct role

---

**Implementation completed**: January 23, 2026  
**Version**: 1.0.0  
**Stack**: Spring Boot 3.4.1, Java 21, Cloudinary SDK 2.0.0
