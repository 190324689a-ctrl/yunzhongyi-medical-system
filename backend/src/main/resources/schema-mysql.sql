-- ============================================================
-- 云中医医务系统 · MySQL 建表脚本
-- 与 schema.sql 保持同构
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100),
    real_name VARCHAR(50),
    phone VARCHAR(20),
    dept_id BIGINT,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(100),
    type INT DEFAULT 2,
    path VARCHAR(200),
    component VARCHAR(200),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    type VARCHAR(50),
    director VARCHAR(50),
    phone VARCHAR(20),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_code VARCHAR(50) NOT NULL,
    dict_value VARCHAR(100) NOT NULL,
    label VARCHAR(100) NOT NULL,
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    module VARCHAR(50),
    operation VARCHAR(50),
    method VARCHAR(200),
    params VARCHAR(2000),
    result INT DEFAULT 1,
    cost_ms BIGINT,
    error_msg VARCHAR(1000),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS physician (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(2),
    id_card VARCHAR(20),
    phone VARCHAR(20),
    dept_id BIGINT,
    dept_name VARCHAR(100),
    title VARCHAR(50),
    education VARCHAR(50),
    graduate_school VARCHAR(100),
    major VARCHAR(100),
    practice_no VARCHAR(50),
    practice_scope VARCHAR(200),
    practice_category VARCHAR(50),
    staff_type VARCHAR(50),
    hire_start_date DATE,
    hire_end_date DATE,
    certificates VARCHAR(500),
    photo_url VARCHAR(500),
    training VARCHAR(1000),
    surgery_level INT,
    surgery_auth INT,
    antitumor_rx INT,
    anesthesia_rx INT,
    psychotropic_rx INT,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS leave_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT NOT NULL,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    apply_type VARCHAR(50) NOT NULL,
    start_date DATE,
    end_date DATE,
    days INT,
    reason VARCHAR(1000),
    destination VARCHAR(200),
    surgery_level INT,
    status INT DEFAULT 0,
    current_approver VARCHAR(50),
    approval_opinion VARCHAR(500),
    approval_history VARCHAR(2000),
    apply_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS key_authorization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT NOT NULL,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    auth_type VARCHAR(50) NOT NULL,
    surgery_level INT,
    valid_from DATE,
    valid_to DATE,
    reason VARCHAR(1000),
    status INT DEFAULT 0,
    current_approver VARCHAR(50),
    approval_opinion VARCHAR(500),
    approval_history VARCHAR(2000),
    apply_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS assessment_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    assess_type VARCHAR(50) NOT NULL,
    cycle VARCHAR(50),
    score_items VARCHAR(2000),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS physician_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT NOT NULL,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    template_id BIGINT,
    template_name VARCHAR(100),
    assess_type VARCHAR(50) NOT NULL,
    assess_year INT,
    cycle VARCHAR(50),
    item_scores VARCHAR(2000),
    total_score DECIMAL(5,2),
    grade VARCHAR(20),
    result VARCHAR(100),
    assessor VARCHAR(50),
    assess_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS schedule_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    schedule_type VARCHAR(50) NOT NULL,
    shift_pattern VARCHAR(2000),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS duty_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT NOT NULL,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    schedule_date DATE NOT NULL,
    schedule_type VARCHAR(50) NOT NULL,
    shift_code VARCHAR(50) NOT NULL,
    start_time TIME,
    end_time TIME,
    template_id BIGINT,
    template_name VARCHAR(100),
    change_type VARCHAR(20),
    change_status INT DEFAULT 0,
    target_date DATE,
    target_shift_code VARCHAR(50),
    target_start_time TIME,
    target_end_time TIME,
    target_physician_id BIGINT,
    target_physician_name VARCHAR(50),
    change_reason VARCHAR(500),
    approver VARCHAR(50),
    approval_opinion VARCHAR(500),
    approval_history VARCHAR(2000),
    apply_date DATE,
    his_sync_status INT DEFAULT 0,
    his_sync_msg VARCHAR(500),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mdt_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_name VARCHAR(50) NOT NULL,
    inpatient_no VARCHAR(50),
    mdt_type VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    apply_physician_id BIGINT,
    apply_physician_name VARCHAR(50) NOT NULL,
    condition_summary VARCHAR(2000),
    invited_depts VARCHAR(500),
    invited_experts VARCHAR(500),
    expected_time TIMESTAMP NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    status INT DEFAULT 0,
    approver VARCHAR(50),
    approval_opinion VARCHAR(500),
    approval_history VARCHAR(2000),
    apply_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mdt_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mdt_id BIGINT NOT NULL,
    attendees VARCHAR(500) NOT NULL,
    meeting_time TIMESTAMP NOT NULL,
    meeting_place VARCHAR(100),
    opinion VARCHAR(2000) NOT NULL,
    suggestion VARCHAR(2000),
    recorder VARCHAR(50),
    record_date DATE,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quality_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    inspect_type VARCHAR(50) NOT NULL,
    check_items VARCHAR(4000),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS template_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    template_name VARCHAR(100),
    version_no VARCHAR(20) NOT NULL,
    check_items VARCHAR(4000),
    change_remark VARCHAR(500),
    status INT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quality_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT,
    template_name VARCHAR(100),
    inspect_type VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100) NOT NULL,
    inspector VARCHAR(200) NOT NULL,
    plan_date DATE NOT NULL,
    status INT DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quality_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    template_id BIGINT,
    template_name VARCHAR(100),
    inspect_type VARCHAR(50),
    dept_id BIGINT,
    dept_name VARCHAR(100),
    item_scores VARCHAR(2000) NOT NULL,
    total_score DECIMAL(10, 2),
    grade VARCHAR(20),
    result VARCHAR(500),
    inspector VARCHAR(200) NOT NULL,
    check_date DATE NOT NULL,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quality_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    plan_id BIGINT,
    description VARCHAR(2000) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    resp_dept_name VARCHAR(100),
    resp_person VARCHAR(50),
    found_date DATE,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quality_rectification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    plan_id BIGINT,
    record_id BIGINT,
    measure VARCHAR(2000) NOT NULL,
    deadline DATE NOT NULL,
    resp_person VARCHAR(50) NOT NULL,
    status INT DEFAULT 0,
    rectify_date DATE,
    reviewer VARCHAR(50),
    review_opinion VARCHAR(500),
    review_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS newtech_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(200) NOT NULL,
    project_type VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    applicant_id BIGINT,
    applicant_name VARCHAR(50) NOT NULL,
    project_leader VARCHAR(50),
    team_members VARCHAR(500),
    background VARCHAR(2000),
    objective VARCHAR(2000),
    tech_plan VARCHAR(4000),
    expected_benefit VARCHAR(2000),
    budget DECIMAL(12, 2),
    start_date DATE,
    end_date DATE,
    ethics_required BOOLEAN DEFAULT FALSE,
    ethics_status INT DEFAULT 0,
    ethics_reviewer VARCHAR(50),
    ethics_opinion VARCHAR(500),
    ethics_review_date DATE,
    status INT DEFAULT 0,
    approver VARCHAR(50),
    approval_opinion VARCHAR(500),
    approval_history VARCHAR(2000),
    apply_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS newtech_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    record_type VARCHAR(50) NOT NULL,
    record_date DATE,
    content VARCHAR(4000) NOT NULL,
    conclusion VARCHAR(2000),
    recorder VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS personnel_change (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT,
    physician_name VARCHAR(50) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    from_dept_id BIGINT,
    from_dept_name VARCHAR(100),
    to_dept_id BIGINT,
    to_dept_name VARCHAR(100),
    change_date DATE NOT NULL,
    apply_date DATE,
    reason VARCHAR(2000),
    status INT DEFAULT 0,
    approver VARCHAR(50),
    approval_opinion VARCHAR(500),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS workload_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    stat_month VARCHAR(7) NOT NULL,
    outpatient_count INT DEFAULT 0,
    inpatient_count INT DEFAULT 0,
    surgery_count INT DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quality_indicator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL,
    indicator_name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    standard_level VARCHAR(20) NOT NULL,
    standard_name VARCHAR(200),
    target_value DECIMAL(12, 2),
    actual_value DECIMAL(12, 2),
    unit VARCHAR(20),
    direction VARCHAR(20) DEFAULT 'HIGHER_BETTER',
    stat_period VARCHAR(20),
    dept_name VARCHAR(100),
    data_source VARCHAR(500),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS radiation_personnel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(2),
    id_card VARCHAR(20),
    phone VARCHAR(20),
    dept_id BIGINT,
    dept_name VARCHAR(100),
    title VARCHAR(50),
    work_type VARCHAR(50) NOT NULL,
    license_no VARCHAR(50),
    permit_no VARCHAR(50),
    permit_expiry DATE,
    entry_date DATE,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS radiation_qualification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    personnel_id BIGINT NOT NULL,
    personnel_name VARCHAR(50) NOT NULL,
    cert_type VARCHAR(50) NOT NULL,
    cert_name VARCHAR(100) NOT NULL,
    cert_no VARCHAR(50),
    issue_date DATE,
    expiry_date DATE,
    status INT DEFAULT 0,
    approver VARCHAR(50),
    approval_opinion VARCHAR(500),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS radiation_training (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    personnel_id BIGINT NOT NULL,
    personnel_name VARCHAR(50) NOT NULL,
    training_name VARCHAR(200) NOT NULL,
    training_type VARCHAR(50) NOT NULL,
    org VARCHAR(200),
    training_date DATE,
    hours INT,
    result VARCHAR(20),
    certificate_no VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS radiation_health (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    personnel_id BIGINT NOT NULL,
    personnel_name VARCHAR(50) NOT NULL,
    exam_type VARCHAR(50) NOT NULL,
    exam_date DATE,
    exam_org VARCHAR(200),
    dose DECIMAL(10, 2),
    result VARCHAR(20),
    conclusion VARCHAR(2000),
    next_exam_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS committee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    chairperson VARCHAR(50),
    vice_chairperson VARCHAR(50),
    secretary VARCHAR(50),
    member_count INT,
    term_no INT,
    term_start_date DATE,
    term_end_date DATE,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS committee_meeting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    committee_id BIGINT NOT NULL,
    committee_name VARCHAR(100),
    title VARCHAR(200) NOT NULL,
    meeting_date DATE,
    location VARCHAR(200),
    host VARCHAR(50),
    recorder VARCHAR(50),
    attendee_count INT,
    attendees VARCHAR(1000),
    content VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS committee_resolution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    committee_id BIGINT,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(2000),
    responsible_person VARCHAR(50),
    deadline DATE,
    status INT DEFAULT 0,
    complete_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    doc_no VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    version VARCHAR(20) DEFAULT 'V1.0',
    content TEXT,
    department VARCHAR(100),
    drafter VARCHAR(50),
    approver VARCHAR(50),
    publish_date DATE,
    effective_date DATE,
    expiry_date DATE,
    status INT DEFAULT 0,
    abolish_reason VARCHAR(500),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    policy_no VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    level VARCHAR(50) NOT NULL,
    issuing_authority VARCHAR(100),
    version VARCHAR(20) DEFAULT 'V1.0',
    content TEXT,
    applicable_dept VARCHAR(500),
    applicable_personnel VARCHAR(200),
    department VARCHAR(100),
    drafter VARCHAR(50),
    approver VARCHAR(50),
    publish_date DATE,
    effective_date DATE,
    expiry_date DATE,
    status INT DEFAULT 0,
    abolish_reason VARCHAR(500),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS policy_reading (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    policy_title VARCHAR(200),
    reader_name VARCHAR(50) NOT NULL,
    dept_name VARCHAR(100),
    read_date DATE,
    learned INT DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS core_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    system_no VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    level VARCHAR(50) NOT NULL,
    issuing_authority VARCHAR(100),
    version VARCHAR(20) DEFAULT 'V1.0',
    content TEXT,
    applicable_dept VARCHAR(500),
    applicable_personnel VARCHAR(200),
    department VARCHAR(100),
    drafter VARCHAR(50),
    approver VARCHAR(50),
    publish_date DATE,
    effective_date DATE,
    expiry_date DATE,
    status INT DEFAULT 0,
    abolish_reason VARCHAR(500),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS core_system_reading (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    core_system_id BIGINT NOT NULL,
    core_system_title VARCHAR(200),
    reader_name VARCHAR(50) NOT NULL,
    dept_name VARCHAR(100),
    read_date DATE,
    learned INT DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ethics_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_no VARCHAR(50) NOT NULL,
    physician_id BIGINT NOT NULL,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    title VARCHAR(50),
    establish_date DATE,
    status INT DEFAULT 0,
    last_grade VARCHAR(20),
    last_evaluate_year INT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ethics_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    score_items VARCHAR(2000),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ethics_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT NOT NULL,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    template_id BIGINT,
    template_name VARCHAR(100),
    assess_year INT,
    cycle VARCHAR(50),
    item_scores VARCHAR(2000),
    total_score DECIMAL(10,2),
    grade VARCHAR(20),
    result VARCHAR(50),
    assessor VARCHAR(50),
    assess_date DATE,
    status INT DEFAULT 0,
    approver VARCHAR(50),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ethics_complaint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_no VARCHAR(50) NOT NULL,
    complainant_name VARCHAR(50) NOT NULL,
    complainant_phone VARCHAR(20),
    physician_id BIGINT,
    physician_name VARCHAR(50),
    dept_name VARCHAR(100),
    complaint_type VARCHAR(20) NOT NULL,
    complaint_date DATE,
    deadline_date DATE,
    content VARCHAR(2000),
    status INT DEFAULT 0,
    handler VARCHAR(50),
    handle_result VARCHAR(2000),
    handle_date DATE,
    feedback_content VARCHAR(2000),
    feedback_date DATE,
    reject_reason VARCHAR(500),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ethics_commendation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT NOT NULL,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    commend_type VARCHAR(20) NOT NULL,
    commend_level VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(2000),
    granted_by VARCHAR(100),
    commend_date DATE,
    status INT DEFAULT 0,
    approver VARCHAR(50),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rectification_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_period VARCHAR(20) NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    dept_name VARCHAR(100),
    inspect_type VARCHAR(50),
    plan_count INT DEFAULT 0,
    record_count INT DEFAULT 0,
    avg_score DECIMAL(10, 2) DEFAULT 0,
    issue_count INT DEFAULT 0,
    major_count INT DEFAULT 0,
    minor_count INT DEFAULT 0,
    mild_count INT DEFAULT 0,
    rectification_total INT DEFAULT 0,
    rectification_pending INT DEFAULT 0,
    rectification_done INT DEFAULT 0,
    rectification_reviewed INT DEFAULT 0,
    rectification_rate DECIMAL(10, 2) DEFAULT 0,
    overdue_count INT DEFAULT 0,
    generated_by VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS medical_tech (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tech_no VARCHAR(100) NOT NULL,
    tech_name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    tech_level INT NOT NULL,
    access_type VARCHAR(50) NOT NULL,
    dept_name VARCHAR(500),
    risk_level VARCHAR(50),
    ethics_required INT DEFAULT 0,
    status INT DEFAULT 0,
    admission_date DATE,
    expiry_date DATE,
    description TEXT,
    approver VARCHAR(50),
    approval_history VARCHAR(2000),
    suspend_reason VARCHAR(500),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tech_admission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tech_id BIGINT,
    tech_no VARCHAR(100),
    tech_name VARCHAR(200) NOT NULL,
    apply_type VARCHAR(50) NOT NULL,
    apply_dept VARCHAR(100),
    applicant VARCHAR(50) NOT NULL,
    apply_date DATE,
    reason VARCHAR(1000),
    new_level INT,
    new_dept VARCHAR(500),
    new_expiry_date DATE,
    status INT DEFAULT 0,
    approver VARCHAR(50),
    approval_history VARCHAR(2000),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tech_authorization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id BIGINT,
    physician_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    dept_name VARCHAR(100),
    tech_id BIGINT,
    tech_no VARCHAR(100),
    tech_name VARCHAR(200) NOT NULL,
    tech_level INT,
    auth_level INT,
    start_date DATE,
    end_date DATE,
    status INT DEFAULT 0,
    approver VARCHAR(50),
    approval_history VARCHAR(2000),
    revoke_reason VARCHAR(500),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 19 检查维护：检查项目库
CREATE TABLE IF NOT EXISTS inspection_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_no VARCHAR(100) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    check_method VARCHAR(50),
    standard_id BIGINT,
    standard_no VARCHAR(100),
    standard_text VARCHAR(1000),
    max_score INT DEFAULT 100,
    weight DECIMAL(10,2),
    applicable_type VARCHAR(50),
    status INT DEFAULT 1,
    sort_order INT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 19 检查维护：检查标准库
CREATE TABLE IF NOT EXISTS inspection_standard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    standard_no VARCHAR(100) NOT NULL,
    standard_name VARCHAR(200) NOT NULL,
    level VARCHAR(50) NOT NULL,
    source_doc VARCHAR(500),
    content TEXT,
    applicable_scope VARCHAR(200),
    effective_date DATE,
    expiry_date DATE,
    status INT DEFAULT 1,
    abolish_reason VARCHAR(500),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 19 检查维护：模板检查项配置（模板与检查项目的组装关系）
CREATE TABLE IF NOT EXISTS inspection_template_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT,
    template_name VARCHAR(200),
    item_id BIGINT,
    item_no VARCHAR(100),
    item_name VARCHAR(200),
    weight DECIMAL(10,2),
    sort_order INT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 20 检查分类：检查分类库（两级分类；检查项目按分类编码归类）
CREATE TABLE IF NOT EXISTS inspection_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(100) NOT NULL,
    category_name VARCHAR(200) NOT NULL,
    parent_id BIGINT,
    level INT DEFAULT 1,
    applicable_type VARCHAR(50),
    description VARCHAR(1000),
    status INT DEFAULT 1,
    sort_order INT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS grading_standard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    standard_code VARCHAR(50) NOT NULL,
    standard_name VARCHAR(100) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    excellent_min DECIMAL(5,2) DEFAULT 90,
    good_min DECIMAL(5,2) DEFAULT 80,
    pass_min DECIMAL(5,2) DEFAULT 60,
    valid_from DATE,
    valid_to DATE,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS grading_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    standard_id BIGINT NOT NULL,
    standard_code VARCHAR(50),
    standard_name VARCHAR(100),
    dept_id BIGINT,
    dept_name VARCHAR(100) NOT NULL,
    period VARCHAR(10) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    grade VARCHAR(10),
    status INT DEFAULT 0,
    assessor VARCHAR(50),
    assess_date DATE,
    approver VARCHAR(50),
    approve_opinion VARCHAR(500),
    approve_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 27 医务系统完成统计：需求清单各模块的完成台账（编号 1-31 唯一；接口数/测试数为空表示计入其他模块）
CREATE TABLE IF NOT EXISTS module_completion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_no INT NOT NULL,
    module_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    status INT DEFAULT 0,
    progress INT DEFAULT 0,
    interface_count INT,
    test_count INT,
    completed_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 28 医务系统设置：用户-角色 / 角色-权限关联（底座补强）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);

-- 登录鉴权增强：角色-科室绑定（角色按医院科室授权数据范围；菜单可见性仍由角色-权限决定）
CREATE TABLE IF NOT EXISTS sys_role_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL
);

-- 模块 28 医务系统设置：系统参数（键唯一；内置参数键不可改、不可删）
CREATE TABLE IF NOT EXISTS sys_param (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    param_key VARCHAR(100) NOT NULL,
    param_name VARCHAR(100) NOT NULL,
    param_value VARCHAR(1000),
    param_group VARCHAR(50),
    value_type VARCHAR(20),
    status INT DEFAULT 1,
    is_system INT DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 28 医务系统设置：数据备份记录
CREATE TABLE IF NOT EXISTS sys_backup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    backup_name VARCHAR(100) NOT NULL,
    backup_type VARCHAR(20),
    backup_scope VARCHAR(50),
    record_count BIGINT,
    file_name VARCHAR(200),
    file_size BIGINT,
    status INT DEFAULT 0,
    created_by VARCHAR(50),
    restore_count INT DEFAULT 0,
    last_restore_time TIMESTAMP,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 28 医务系统设置：系统接口配置
CREATE TABLE IF NOT EXISTS sys_interface (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interface_code VARCHAR(50) NOT NULL,
    interface_name VARCHAR(100) NOT NULL,
    type VARCHAR(20),
    url VARCHAR(500) NOT NULL,
    auth_type VARCHAR(20),
    auth_token VARCHAR(500),
    status INT DEFAULT 1,
    timeout_seconds INT DEFAULT 5,
    last_test_time TIMESTAMP,
    last_test_result VARCHAR(20),
    last_test_msg VARCHAR(500),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 28 医务系统设置：消息提醒设置
CREATE TABLE IF NOT EXISTS sys_notice_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    channel VARCHAR(20),
    trigger_event VARCHAR(200),
    template_content VARCHAR(1000),
    receiver_scope VARCHAR(20),
    receiver_value VARCHAR(100),
    enabled INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 28 医务系统设置：知识库（支持 3000+ 条：分页 + 关键字检索 + 按分类筛选）
CREATE TABLE IF NOT EXISTS sys_knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_code VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    content VARCHAR(4000),
    keywords VARCHAR(500),
    status INT DEFAULT 1,
    view_count BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 29 消息提醒：站内消息
CREATE TABLE IF NOT EXISTS notify_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(2000),
    category VARCHAR(20),
    sender_name VARCHAR(100),
    receiver_scope VARCHAR(20),
    receiver_value VARCHAR(100),
    receiver_name VARCHAR(100),
    read_status INT DEFAULT 0,
    read_time TIMESTAMP,
    send_time TIMESTAMP,
    related_type VARCHAR(50),
    related_id BIGINT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 29 消息提醒：待办提醒
CREATE TABLE IF NOT EXISTS notify_todo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(2000),
    todo_type VARCHAR(20),
    related_type VARCHAR(50),
    related_id BIGINT,
    assignee VARCHAR(100),
    assignee_name VARCHAR(100),
    dept_name VARCHAR(100),
    due_date DATE,
    status INT DEFAULT 0,
    complete_time TIMESTAMP,
    source VARCHAR(20),
    setting_key VARCHAR(100),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 29 消息提醒：消息推送记录
CREATE TABLE IF NOT EXISTS notify_push (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(2000),
    channel VARCHAR(20),
    receiver_scope VARCHAR(20),
    receiver_value VARCHAR(100),
    receiver_name VARCHAR(100),
    send_status INT DEFAULT 0,
    send_time TIMESTAMP,
    fail_reason VARCHAR(500),
    related_message_id BIGINT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 29 消息提醒：短信提醒记录
CREATE TABLE IF NOT EXISTS notify_sms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    content VARCHAR(1000),
    receiver_name VARCHAR(100),
    template_code VARCHAR(100),
    send_status INT DEFAULT 0,
    send_time TIMESTAMP,
    fail_reason VARCHAR(500),
    related_type VARCHAR(50),
    related_id BIGINT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 30 医疗工作简报：简报模板
CREATE TABLE IF NOT EXISTS briefing_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20),
    sections VARCHAR(2000),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 30 医疗工作简报：简报
CREATE TABLE IF NOT EXISTS briefing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_no VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    template_id BIGINT,
    template_name VARCHAR(100),
    category VARCHAR(20),
    period VARCHAR(10),
    content VARCHAR(8000),
    status INT DEFAULT 0,
    author_name VARCHAR(100),
    publish_date DATE,
    approver VARCHAR(100),
    approval_opinion VARCHAR(500),
    approval_history VARCHAR(2000),
    approve_time TIMESTAMP,
    reject_reason VARCHAR(500),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 31 医务管理移动端：移动端设备登记
CREATE TABLE IF NOT EXISTS mobile_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(50) NOT NULL,
    username VARCHAR(50) NOT NULL,
    user_name VARCHAR(50),
    platform VARCHAR(20),
    device_model VARCHAR(100),
    app_version VARCHAR(20),
    push_token VARCHAR(200),
    status INT DEFAULT 1,
    last_active_time TIMESTAMP,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 模块 31 医务管理移动端：移动端审批操作记录
CREATE TABLE IF NOT EXISTS mobile_approval_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_type VARCHAR(20),
    related_id BIGINT,
    related_title VARCHAR(200),
    action VARCHAR(20),
    operator VARCHAR(50),
    device_code VARCHAR(50),
    opinion VARCHAR(500),
    result VARCHAR(20),
    fail_reason VARCHAR(500),
    action_time TIMESTAMP,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);
