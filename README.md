# OIT-App — Oral Immunotherapy Management Platform

A comprehensive OIT management platform for pediatric clinical use, consisting of three integrated components.

---

## Components

### 1. Backend API (`backend/`)
Node.js / Express REST API with MySQL.

**Setup:**
```bash
cd backend
cp .env.example .env          # fill in DB credentials and JWT_SECRET
npm install
# Import schema into MySQL:
mysql -u root -p < database/schema.sql
npm start                     # runs on port 3000 by default
```

### 2. Web Dashboard (`dashboard/`)
PHP pages for cPanel hosting — connects directly to MySQL via PDO.
Uses **Tabler UI** (CDN) for styling.

**Deployment to cPanel:**
1. Upload the `dashboard/` folder contents to your `public_html/dashboard/` directory.
2. Copy `config/db.example.php` → `config/db.php` on the server and set the real password.
3. The `config/db.php` file is `.gitignore`d and must **never** be committed.
4. Ensure PHP 7.4+ and the `pdo_mysql` extension are enabled (standard on cPanel).

**Database:** `voxelnodes_oit` (see `config/db.example.php` for connection settings)

### 3. Android Application (`android/`)
Android 5.1.1+ (minSdkVersion = 22), Java, Groovy Gradle — produces a standard APK.

**Build:**
```bash
cd android
./gradlew assembleDebug        # debug APK
./gradlew assembleRelease      # release APK
```
APK output: `android/app/build/outputs/apk/`

Configure the API base URL in `SessionManager.java` (default: `http://10.0.2.2:3000` for emulator).

---

## Database Schema
Full MySQL schema: `backend/database/schema.sql`
Tables: `users`, `patients`, `dose_plans`, `updose_decisions`, `cartons`, `powder_supplies`, `syringes`, `mar_entries`, `food_logs`, `symptom_logs`, `print_jobs`, `notifications`, `audit_logs`, `missions`, `badges`

---

## Security Notes
- `dashboard/config/db.php` contains live credentials — **gitignored**, never commit it.
- Backend uses JWT (HS256) + bcrypt password hashing.
- All PHP output escaped with `htmlspecialchars`; all SQL uses PDO prepared statements.
- Android clears session data on logout.