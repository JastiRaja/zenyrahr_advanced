DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'job_posting'
    ) THEN
        ALTER TABLE job_posting ADD COLUMN IF NOT EXISTS description TEXT;
        ALTER TABLE job_posting ADD COLUMN IF NOT EXISTS source_channel VARCHAR(120);
        ALTER TABLE job_posting ADD COLUMN IF NOT EXISTS owner_employee_id BIGINT;
        ALTER TABLE job_posting ADD COLUMN IF NOT EXISTS opened_at TIMESTAMP;
        ALTER TABLE job_posting ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP;

        UPDATE job_posting
        SET opened_at = NOW()
        WHERE opened_at IS NULL;

        ALTER TABLE job_posting ALTER COLUMN opened_at SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'candidate'
    ) THEN
        ALTER TABLE candidate ADD COLUMN IF NOT EXISTS source VARCHAR(80);
        ALTER TABLE candidate ADD COLUMN IF NOT EXISTS owner_employee_id BIGINT;
        ALTER TABLE candidate ADD COLUMN IF NOT EXISTS applied_at TIMESTAMP;
        ALTER TABLE candidate ADD COLUMN IF NOT EXISTS stage_updated_at TIMESTAMP;
        ALTER TABLE candidate ADD COLUMN IF NOT EXISTS notes TEXT;
        ALTER TABLE candidate ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

        UPDATE candidate
        SET applied_at = NOW()
        WHERE applied_at IS NULL;

        UPDATE candidate
        SET stage_updated_at = NOW()
        WHERE stage_updated_at IS NULL;

        ALTER TABLE candidate ALTER COLUMN applied_at SET NOT NULL;
        ALTER TABLE candidate ALTER COLUMN stage_updated_at SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.job_posting') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_job_posting_org_status ON job_posting (organization_id, status);
        CREATE INDEX IF NOT EXISTS idx_job_posting_org_department ON job_posting (organization_id, department);
    END IF;

    IF to_regclass('public.candidate') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_candidate_org_stage ON candidate (organization_id, stage);
        CREATE INDEX IF NOT EXISTS idx_candidate_org_job ON candidate (organization_id, job_posting_id);
    END IF;
END $$;
