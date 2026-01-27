-- Create lab_results table for storing uploaded test results
-- Supports multiple versions/uploads per lab order

CREATE TABLE lab_results (
    result_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_order_id UUID NOT NULL,

    -- File storage (Cloudinary)
    file_url VARCHAR(500) NOT NULL,
    cloudinary_public_id VARCHAR(255),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,

    -- Metadata
    version INTEGER NOT NULL DEFAULT 1,
    is_latest BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    -- Upload info
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_by_name VARCHAR(200),

    -- Audit
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_lab_result_order FOREIGN KEY (lab_order_id) REFERENCES lab_orders(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_lab_result_order_id ON lab_results(lab_order_id);
CREATE INDEX idx_lab_result_uploaded_by ON lab_results(uploaded_by);
CREATE INDEX idx_lab_result_latest ON lab_results(lab_order_id, is_latest) WHERE is_latest = TRUE AND deleted = FALSE;
CREATE INDEX idx_lab_result_version ON lab_results(lab_order_id, version) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE lab_results IS 'Lab test results with file uploads - supports versioning';
COMMENT ON COLUMN lab_results.version IS 'Version number for multiple uploads of same order';
COMMENT ON COLUMN lab_results.is_latest IS 'Flag indicating the most recent version';
COMMENT ON COLUMN lab_results.uploaded_by IS 'Phlebotomist admin user ID who uploaded the result';
