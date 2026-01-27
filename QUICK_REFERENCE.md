# 🚀 Cloudinary Lab Reports - Quick Reference

## Environment Setup (ONE-TIME)

```bash
# 1. Get Cloudinary account (free tier)
https://cloudinary.com/users/register/free

# 2. Copy credentials from Cloudinary Dashboard
Cloud Name: _______________
API Key: _______________
API Secret: _______________

# 3. Add to .env file
cd lab_service_medibridge
echo "CLOUDINARY_CLOUD_NAME=your_cloud_name" >> .env
echo "CLOUDINARY_API_KEY=your_api_key" >> .env
echo "CLOUDINARY_API_SECRET=your_api_secret" >> .env
```

---

## API Endpoints Summary

### ADMIN (All Operations)

**Upload Report** (multipart/form-data)
```bash
POST /api/v1/admin/lab/lab-orders/{orderId}/report/upload
Authorization: Bearer {ADMIN_TOKEN}
Body: file=@report.pdf
```

**Publish Report**
```bash
POST /api/v1/admin/lab/lab-orders/{orderId}/report/publish
Authorization: Bearer {ADMIN_TOKEN}
```

**Get Report** (any status)
```bash
GET /api/v1/admin/lab/lab-orders/{orderId}/report
Authorization: Bearer {ADMIN_TOKEN}
```

---

### PATIENT (View Own Published Reports)

**Get My Report**
```bash
GET /api/v1/lab-orders/{orderId}/report
Authorization: Bearer {PATIENT_TOKEN}
```

---

### DOCTOR (View Reports for Own Orders)

**Get Report for My Order**
```bash
GET /api/v1/doctors/lab-orders/{orderId}/report
Authorization: Bearer {DOCTOR_TOKEN}
```

---

## Quick Test

```bash
# 1. Upload as admin
curl -X POST http://localhost:8085/api/v1/admin/lab/lab-orders/123/report/upload \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -F "file=@test.pdf"

# 2. Publish as admin
curl -X POST http://localhost:8085/api/v1/admin/lab/lab-orders/123/report/publish \
  -H "Authorization: Bearer ADMIN_TOKEN"

# 3. Get as patient
curl -X GET http://localhost:8085/api/v1/lab-orders/123/report \
  -H "Authorization: Bearer PATIENT_TOKEN"
```

---

## Security Matrix

| Role | Upload | Publish | View Draft | View Published |
|------|--------|---------|------------|----------------|
| ADMIN | ✓ | ✓ | ✓ | ✓ |
| DOCTOR | ✗ | ✗ | ✗ | ✓ (own orders) |
| PATIENT | ✗ | ✗ | ✗ | ✓ (own orders) |

---

## File Constraints

- **Max Size**: 10MB (configurable via MAX_FILE_SIZE_MB)
- **Types**: PDF, PNG, JPEG, JPG only
- **Validation**: MIME type + extension + size

---

## Response Status Codes

- **201 Created** - Report uploaded successfully
- **200 OK** - Report published/retrieved successfully
- **400 Bad Request** - Invalid file (size/type)
- **403 Forbidden** - Access denied (not owner/creator)
- **404 Not Found** - Report/order not found
- **500 Internal Server Error** - Cloud storage error

---

## Troubleshooting

**"Cloudinary not configured"**
→ Add credentials to .env file

**"File size exceeds maximum"**
→ File > 10MB, compress or change MAX_FILE_SIZE_MB

**"File type not allowed"**
→ Only PDF, PNG, JPG supported

**"Report not yet published"**
→ Admin must publish first

**"Access denied"**
→ Check user is owner (patient) or creator (doctor)

---

## Files Modified

✅ 13 files created  
✅ 7 files modified  
✅ 0 compilation errors  
✅ Build successful  

---

## What Gets Stored

```
Cloudinary: Actual file (PDF/image)
Database: URL + metadata (checksum, size, type)
Kafka: Event when published
```

---

## Cloudinary Folder Structure

```
medibridge/lab-reports/
  └── 2026/
      └── 01/
          └── ORDER-123/
              └── lab_ORDER-123_1737633600000.pdf
```

---

## Dependencies Added

- cloudinary-http5:2.0.0
- cloudinary-taglib:2.0.0  
- dotenv-java:2.2.4

---

## Documentation

📖 **Full Guide**: `CLOUDINARY_IMPLEMENTATION.md`  
📋 **Summary**: `IMPLEMENTATION_SUMMARY.md`  
📝 **This File**: Quick reference

---

**Status**: ✅ READY TO USE  
**Date**: January 23, 2026  
**Port**: 8085
