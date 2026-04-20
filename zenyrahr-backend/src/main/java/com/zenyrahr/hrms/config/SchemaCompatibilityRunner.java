package com.zenyrahr.hrms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaCompatibilityRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // Backward-compatible migration for older databases.
        // Ensures the org active-user quota column exists before entity reads.
        try {
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS max_active_users INTEGER");
            jdbcTemplate.execute("UPDATE organization SET max_active_users = 25 WHERE max_active_users IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN max_active_users SET DEFAULT 25");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN max_active_users SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS timesheet_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET timesheet_enabled = TRUE WHERE timesheet_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN timesheet_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN timesheet_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization DROP COLUMN IF EXISTS recruitment_enabled");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS employee_management_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET employee_management_enabled = TRUE WHERE employee_management_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN employee_management_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN employee_management_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS self_service_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET self_service_enabled = TRUE WHERE self_service_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN self_service_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN self_service_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS attendance_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET attendance_enabled = TRUE WHERE attendance_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN attendance_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN attendance_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS leave_management_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET leave_management_enabled = TRUE WHERE leave_management_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN leave_management_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN leave_management_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS holiday_management_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET holiday_management_enabled = TRUE WHERE holiday_management_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN holiday_management_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN holiday_management_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS payroll_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET payroll_enabled = TRUE WHERE payroll_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN payroll_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN payroll_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS travel_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET travel_enabled = TRUE WHERE travel_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN travel_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN travel_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS expense_enabled BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET expense_enabled = TRUE WHERE expense_enabled IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN expense_enabled SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN expense_enabled SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS employee_code_prefix VARCHAR(32)");
            jdbcTemplate.execute("UPDATE organization SET employee_code_prefix = 'EMP' WHERE employee_code_prefix IS NULL OR employee_code_prefix = ''");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN employee_code_prefix SET DEFAULT 'EMP'");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN employee_code_prefix SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS employee_code_padding INTEGER");
            jdbcTemplate.execute("UPDATE organization SET employee_code_padding = 4 WHERE employee_code_padding IS NULL OR employee_code_padding <= 0");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN employee_code_padding SET DEFAULT 4");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN employee_code_padding SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS next_employee_code_number INTEGER");
            jdbcTemplate.execute("UPDATE organization SET next_employee_code_number = 1 WHERE next_employee_code_number IS NULL OR next_employee_code_number <= 0");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN next_employee_code_number SET DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN next_employee_code_number SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE organization ADD COLUMN IF NOT EXISTS allow_manual_employee_code_override BOOLEAN");
            jdbcTemplate.execute("UPDATE organization SET allow_manual_employee_code_override = FALSE WHERE allow_manual_employee_code_override IS NULL");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN allow_manual_employee_code_override SET DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE organization ALTER COLUMN allow_manual_employee_code_override SET NOT NULL");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS approval_hierarchy_rule (" +
                            "id BIGSERIAL PRIMARY KEY," +
                            "organization_id BIGINT NOT NULL," +
                            "module VARCHAR(32) NOT NULL," +
                            "level_no INTEGER NOT NULL," +
                            "approver_type VARCHAR(32) NOT NULL," +
                            "approver_role VARCHAR(64)," +
                            "approver_user_id BIGINT," +
                            "active BOOLEAN NOT NULL DEFAULT TRUE," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "created_by VARCHAR(100)," +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "updated_by VARCHAR(100)," +
                            "CONSTRAINT uk_approval_rule_org_module_level UNIQUE (organization_id, module, level_no)" +
                            ")"
            );
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD COLUMN IF NOT EXISTS organization_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD COLUMN IF NOT EXISTS module VARCHAR(32)");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD COLUMN IF NOT EXISTS level_no INTEGER");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD COLUMN IF NOT EXISTS approver_type VARCHAR(32)");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD COLUMN IF NOT EXISTS approver_role VARCHAR(64)");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD COLUMN IF NOT EXISTS approver_user_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD COLUMN IF NOT EXISTS active BOOLEAN");
            jdbcTemplate.execute("UPDATE approval_hierarchy_rule SET active = TRUE WHERE active IS NULL");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ALTER COLUMN active SET DEFAULT TRUE");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_approval_rule_org_module ON approval_hierarchy_rule(organization_id, module)");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule DROP CONSTRAINT IF EXISTS fk_approval_rule_org");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD CONSTRAINT fk_approval_rule_org FOREIGN KEY (organization_id) REFERENCES organization(id)");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule DROP CONSTRAINT IF EXISTS fk_approval_rule_user");
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule ADD CONSTRAINT fk_approval_rule_user FOREIGN KEY (approver_user_id) REFERENCES employee(id)");
            // Older databases may still have a module CHECK constraint without TIMESHEET.
            // Recreate it to match current enum values and prevent save failures.
            jdbcTemplate.execute("ALTER TABLE approval_hierarchy_rule DROP CONSTRAINT IF EXISTS approval_hierarchy_rule_module_check");
            jdbcTemplate.execute(
                    "ALTER TABLE approval_hierarchy_rule " +
                            "ADD CONSTRAINT approval_hierarchy_rule_module_check " +
                            "CHECK (module IN ('LEAVE', 'TIMESHEET', 'TRAVEL', 'EXPENSE'))"
            );
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS announcement (" +
                            "id BIGSERIAL PRIMARY KEY," +
                            "code VARCHAR(255) UNIQUE NOT NULL," +
                            "active BOOLEAN NOT NULL DEFAULT TRUE," +
                            "deleted BOOLEAN NOT NULL DEFAULT FALSE," +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "title VARCHAR(160) NOT NULL," +
                            "message TEXT NOT NULL," +
                            "organization_id BIGINT NOT NULL," +
                            "posted_by_name VARCHAR(120) NOT NULL," +
                            "posted_by_role VARCHAR(50) NOT NULL" +
                            ")"
            );
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS code VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS active BOOLEAN");
            jdbcTemplate.execute("UPDATE announcement SET active = TRUE WHERE active IS NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN active SET DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN active SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS deleted BOOLEAN");
            jdbcTemplate.execute("UPDATE announcement SET deleted = FALSE WHERE deleted IS NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN deleted SET DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN deleted SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
            jdbcTemplate.execute("UPDATE announcement SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN created_at SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");
            jdbcTemplate.execute("UPDATE announcement SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS title VARCHAR(160)");
            jdbcTemplate.execute("UPDATE announcement SET title = 'Untitled' WHERE title IS NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN title SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS message TEXT");
            jdbcTemplate.execute("UPDATE announcement SET message = '' WHERE message IS NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN message SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS organization_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS posted_by_name VARCHAR(120)");
            jdbcTemplate.execute("UPDATE announcement SET posted_by_name = 'Unknown User' WHERE posted_by_name IS NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN posted_by_name SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN IF NOT EXISTS posted_by_role VARCHAR(50)");
            jdbcTemplate.execute("UPDATE announcement SET posted_by_role = 'UNKNOWN' WHERE posted_by_role IS NULL");
            jdbcTemplate.execute("ALTER TABLE announcement ALTER COLUMN posted_by_role SET NOT NULL");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_announcement_org_active ON announcement(organization_id, active, deleted)");
            jdbcTemplate.execute("ALTER TABLE announcement DROP CONSTRAINT IF EXISTS fk_announcement_org");
            jdbcTemplate.execute("ALTER TABLE announcement ADD CONSTRAINT fk_announcement_org FOREIGN KEY (organization_id) REFERENCES organization(id)");
            jdbcTemplate.execute("ALTER TABLE leave_request ADD COLUMN IF NOT EXISTS revocation_requested BOOLEAN");
            jdbcTemplate.execute("UPDATE leave_request SET revocation_requested = FALSE WHERE revocation_requested IS NULL");
            jdbcTemplate.execute("ALTER TABLE leave_request ALTER COLUMN revocation_requested SET DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE leave_request ALTER COLUMN revocation_requested SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE timesheet ADD COLUMN IF NOT EXISTS current_approval_level INTEGER");
            jdbcTemplate.execute("UPDATE timesheet SET current_approval_level = 1 WHERE current_approval_level IS NULL OR current_approval_level <= 0");
            jdbcTemplate.execute("ALTER TABLE timesheet ALTER COLUMN current_approval_level SET DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE timesheet ALTER COLUMN current_approval_level SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE timesheet ADD COLUMN IF NOT EXISTS max_approval_level INTEGER");
            jdbcTemplate.execute("UPDATE timesheet SET max_approval_level = 1 WHERE max_approval_level IS NULL OR max_approval_level <= 0");
            jdbcTemplate.execute("ALTER TABLE timesheet ALTER COLUMN max_approval_level SET DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE timesheet ALTER COLUMN max_approval_level SET NOT NULL");
            jdbcTemplate.execute("UPDATE timesheet SET max_approval_level = GREATEST(max_approval_level, current_approval_level)");
            jdbcTemplate.execute("ALTER TABLE project ADD COLUMN IF NOT EXISTS start_date DATE");
            jdbcTemplate.execute("ALTER TABLE project ADD COLUMN IF NOT EXISTS deadline DATE");
            jdbcTemplate.execute("ALTER TABLE project ADD COLUMN IF NOT EXISTS status VARCHAR(32)");
            jdbcTemplate.execute("UPDATE project SET status = 'ACTIVE' WHERE status IS NULL OR TRIM(status) = ''");
            jdbcTemplate.execute("ALTER TABLE project ALTER COLUMN status SET DEFAULT 'ACTIVE'");
            jdbcTemplate.execute("ALTER TABLE project ALTER COLUMN status SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE project ADD COLUMN IF NOT EXISTS organization_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE project ADD COLUMN IF NOT EXISTS description TEXT");
            jdbcTemplate.execute(
                    "DO $$ " +
                            "DECLARE v_type text; " +
                            "BEGIN " +
                            "  SELECT data_type INTO v_type " +
                            "  FROM information_schema.columns " +
                            "  WHERE table_name = 'project' AND column_name = 'description' " +
                            "  LIMIT 1; " +
                            "  IF v_type = 'oid' THEN " +
                            "    ALTER TABLE project ALTER COLUMN description TYPE TEXT " +
                            "    USING CASE " +
                            "      WHEN description IS NULL THEN NULL " +
                            "      ELSE convert_from(lo_get(description), 'UTF8') " +
                            "    END; " +
                            "  END IF; " +
                            "END $$;"
            );
            // Normalize legacy project.organization_id values that were stored as text in older schemas.
            jdbcTemplate.execute(
                    "ALTER TABLE project ALTER COLUMN organization_id TYPE BIGINT " +
                            "USING NULLIF(regexp_replace(organization_id::text, '[^0-9]', '', 'g'), '')::BIGINT"
            );
            jdbcTemplate.execute(
                    "UPDATE project p SET organization_id = e.organization_id " +
                            "FROM employee_projects ep " +
                            "JOIN employee e ON e.id = ep.employee_id " +
                            "WHERE p.id = ep.project_id AND p.organization_id IS NULL"
            );
            jdbcTemplate.execute(
                    "UPDATE project p SET start_date = CURRENT_DATE WHERE p.start_date IS NULL"
            );
            jdbcTemplate.execute(
                    "UPDATE project p SET deadline = COALESCE(p.start_date, CURRENT_DATE) WHERE p.deadline IS NULL"
            );
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_project_org ON project(organization_id)");
            jdbcTemplate.execute("ALTER TABLE project DROP CONSTRAINT IF EXISTS fk_project_org");
            jdbcTemplate.execute("ALTER TABLE project ADD CONSTRAINT fk_project_org FOREIGN KEY (organization_id) REFERENCES organization(id)");
            log.info("Schema compatibility check complete for organization menu and capacity columns");
        } catch (Exception ex) {
            log.warn("Schema compatibility migration skipped/failed: {}", ex.getMessage());
        }
    }
}
