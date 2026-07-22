CREATE DATABASE IF NOT EXISTS oit_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oit_db;

DROP TABLE IF EXISTS badges;
DROP TABLE IF EXISTS missions;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS print_jobs;
DROP TABLE IF EXISTS symptom_logs;
DROP TABLE IF EXISTS food_logs;
DROP TABLE IF EXISTS mar_entries;
DROP TABLE IF EXISTS syringes;
DROP TABLE IF EXISTS powder_supplies;
DROP TABLE IF EXISTS cartons;
DROP TABLE IF EXISTS updose_decisions;
DROP TABLE IF EXISTS dose_plans;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('admin', 'clinician') NOT NULL DEFAULT 'clinician',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email),
  KEY idx_users_role (role)
) ENGINE=InnoDB;

CREATE TABLE patients (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  mrn VARCHAR(100) NOT NULL,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  date_of_birth DATE NOT NULL,
  allergen VARCHAR(255) NOT NULL,
  oit_protocol VARCHAR(255) NOT NULL,
  current_dose_mcg DECIMAL(10,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_patients_mrn (mrn),
  KEY idx_patients_name (last_name, first_name),
  KEY idx_patients_dob (date_of_birth)
) ENGINE=InnoDB;

CREATE TABLE dose_plans (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  scheduled_date DATE NOT NULL,
  dose_mcg DECIMAL(10,2) NOT NULL,
  dose_form ENUM('milk', 'powder') NOT NULL,
  status ENUM('scheduled', 'completed', 'skipped') NOT NULL DEFAULT 'scheduled',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_dose_plans_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  KEY idx_dose_plans_patient_date (patient_id, scheduled_date),
  KEY idx_dose_plans_status (status),
  KEY idx_dose_plans_patient_status_date (patient_id, status, scheduled_date)
) ENGINE=InnoDB;

CREATE TABLE updose_decisions (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  previous_dose_mcg DECIMAL(10,2) NOT NULL,
  new_dose_mcg DECIMAL(10,2) NOT NULL,
  decision_date DATETIME NOT NULL,
  decision_type ENUM('continue', 'advance', 'custom') NOT NULL,
  notes TEXT NULL,
  decided_by_user_id INT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_updose_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  CONSTRAINT fk_updose_user FOREIGN KEY (decided_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
  KEY idx_updose_patient_date (patient_id, decision_date),
  KEY idx_updose_decider (decided_by_user_id)
) ENGINE=InnoDB;

CREATE TABLE cartons (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  lot_number VARCHAR(150) NOT NULL,
  expiration_date DATETIME NOT NULL,
  date_opened DATETIME NULL,
  status ENUM('active', 'discarded', 'expired') NOT NULL DEFAULT 'active',
  barcode VARCHAR(150) NOT NULL,
  notes TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_cartons_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  UNIQUE KEY uq_cartons_barcode (barcode),
  KEY idx_cartons_patient_status (patient_id, status),
  KEY idx_cartons_expiration (expiration_date),
  KEY idx_cartons_lot (lot_number)
) ENGINE=InnoDB;

CREATE TABLE powder_supplies (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  lot_number VARCHAR(150) NOT NULL,
  expiration_date DATETIME NOT NULL,
  date_opened DATETIME NULL,
  status ENUM('active', 'discarded', 'expired') NOT NULL DEFAULT 'active',
  barcode VARCHAR(150) NOT NULL,
  notes TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_powder_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  UNIQUE KEY uq_powder_barcode (barcode),
  KEY idx_powder_patient_status (patient_id, status),
  KEY idx_powder_expiration (expiration_date),
  KEY idx_powder_lot (lot_number)
) ENGINE=InnoDB;

CREATE TABLE syringes (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  carton_id INT UNSIGNED NULL,
  powder_supply_id INT UNSIGNED NULL,
  dose_mcg DECIMAL(10,2) NOT NULL,
  dose_form ENUM('milk', 'powder') NOT NULL,
  barcode VARCHAR(200) NOT NULL,
  date_drawn DATETIME NOT NULL,
  scheduled_dose_date DATE NULL,
  shelf_life_hours INT UNSIGNED NOT NULL,
  calculated_expiry DATETIME NOT NULL,
  final_expiry DATETIME NOT NULL,
  seal_type VARCHAR(100) NOT NULL,
  status ENUM('ready', 'used', 'expired', 'discarded') NOT NULL DEFAULT 'ready',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_syringes_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  CONSTRAINT fk_syringes_carton FOREIGN KEY (carton_id) REFERENCES cartons(id) ON DELETE SET NULL,
  CONSTRAINT fk_syringes_powder FOREIGN KEY (powder_supply_id) REFERENCES powder_supplies(id) ON DELETE SET NULL,
  CONSTRAINT chk_syringes_source CHECK (
    (carton_id IS NOT NULL AND powder_supply_id IS NULL AND dose_form = 'milk')
    OR (carton_id IS NULL AND powder_supply_id IS NOT NULL AND dose_form = 'powder')
  ),
  UNIQUE KEY uq_syringes_barcode (barcode),
  KEY idx_syringes_patient_status (patient_id, status),
  KEY idx_syringes_expiry (final_expiry),
  KEY idx_syringes_scheduled_date (scheduled_dose_date)
) ENGINE=InnoDB;

CREATE TABLE mar_entries (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  syringe_id INT UNSIGNED NOT NULL,
  dose_mcg DECIMAL(10,2) NOT NULL,
  dose_form ENUM('milk', 'powder') NOT NULL,
  administered_at DATETIME NOT NULL,
  administered_by_user_id INT UNSIGNED NOT NULL,
  outcome ENUM('completed', 'reaction', 'missed') NOT NULL,
  notes TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_mar_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  CONSTRAINT fk_mar_syringe FOREIGN KEY (syringe_id) REFERENCES syringes(id) ON DELETE RESTRICT,
  CONSTRAINT fk_mar_user FOREIGN KEY (administered_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
  KEY idx_mar_patient_administered (patient_id, administered_at),
  KEY idx_mar_syringe (syringe_id),
  KEY idx_mar_outcome (outcome)
) ENGINE=InnoDB;

CREATE TABLE food_logs (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  logged_at DATETIME NOT NULL,
  food_description TEXT NOT NULL,
  timing_ok BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_food_logs_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  KEY idx_food_logs_patient_logged (patient_id, logged_at)
) ENGINE=InnoDB;

CREATE TABLE symptom_logs (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  mar_entry_id INT UNSIGNED NULL,
  logged_at DATETIME NOT NULL,
  severity TINYINT UNSIGNED NOT NULL,
  symptoms JSON NOT NULL,
  notes TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_symptom_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  CONSTRAINT fk_symptom_mar FOREIGN KEY (mar_entry_id) REFERENCES mar_entries(id) ON DELETE SET NULL,
  KEY idx_symptom_patient_logged (patient_id, logged_at),
  KEY idx_symptom_mar (mar_entry_id),
  KEY idx_symptom_severity (severity)
) ENGINE=InnoDB;

CREATE TABLE print_jobs (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  syringe_id INT UNSIGNED NOT NULL,
  printed_at DATETIME NOT NULL,
  printer_id VARCHAR(100) NOT NULL,
  printer_type VARCHAR(100) NOT NULL,
  status ENUM('success', 'failed') NOT NULL DEFAULT 'success',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_print_jobs_syringe FOREIGN KEY (syringe_id) REFERENCES syringes(id) ON DELETE CASCADE,
  KEY idx_print_jobs_syringe_printed (syringe_id, printed_at),
  KEY idx_print_jobs_status (status)
) ENGINE=InnoDB;

CREATE TABLE notifications (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  type VARCHAR(100) NOT NULL,
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  KEY idx_notifications_patient_read (patient_id, is_read),
  KEY idx_notifications_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id INT UNSIGNED NULL,
  patient_id INT UNSIGNED NULL,
  action VARCHAR(100) NOT NULL,
  entity_type VARCHAR(100) NOT NULL,
  entity_id VARCHAR(100) NULL,
  details JSON NULL,
  ip_address VARCHAR(64) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_audit_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL,
  KEY idx_audit_patient_created (patient_id, created_at),
  KEY idx_audit_entity (entity_type, entity_id),
  KEY idx_audit_action_created (action, created_at),
  KEY idx_audit_user_created (user_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE missions (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  mission_date DATE NOT NULL,
  mission_type ENUM('log_food', 'complete_dose', 'record_symptoms', 'daily_checklist') NOT NULL,
  status ENUM('pending', 'completed') NOT NULL DEFAULT 'pending',
  completed_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_missions_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  UNIQUE KEY uq_missions_patient_day_type (patient_id, mission_date, mission_type),
  KEY idx_missions_patient_status_date (patient_id, status, mission_date)
) ENGINE=InnoDB;

CREATE TABLE badges (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  patient_id INT UNSIGNED NOT NULL,
  badge_type VARCHAR(100) NOT NULL,
  earned_at DATETIME NOT NULL,
  metadata JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_badges_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  KEY idx_badges_patient_earned (patient_id, earned_at),
  KEY idx_badges_type (badge_type)
) ENGINE=InnoDB;
