# Expense Management System

An enterprise-grade **Expense Management System** built with **Spring Boot 3.3**, **Spring Data JPA (MySQL)**, **Spring Security**, and **Thymeleaf**. Supports Corporate expense workflows and Personal budget tracking.

---

## 🚀 Features

### Core
- ✅ **Dual-Mode**: Corporate expense submission & approval + Personal budget tracking
- ✅ **Authentication**: Session-based login with BCrypt password hashing
- ✅ **Role-Based Access**: Employee, Manager, Admin, Individual user roles
- ✅ **Expense Lifecycle**: DRAFT → SUBMITTED → APPROVED/REJECTED → REIMBURSED

### Beyond CRUD
1. 🔍 **Advanced Search & Filtering** — Filter by keyword, category, status, type, date range, amount
2. 📎 **File Uploads** — Receipt upload (JPEG/PNG/PDF, max 5MB) with server-side validation
3. 📧 **Email Notifications** — On submission, approval, rejection, budget warnings (log-only mock, swap SMTP to activate)
4. 🗑️ **Soft Deletes + Audit Trail** — `deletedAt`, `createdBy`, `updatedBy` on all entities
5. 📊 **Export (CSV/PDF)** — Download filtered expense lists in CSV or PDF format
6. ⏰ **Scheduling** — Weekly draft reminders + daily budget alert checks
7. ⚡ **Caching** — Caffeine cache on dashboard stats and manager lists

---

## 🛠️ Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 21+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |

---

## ⚙️ Setup

### 1. Create the Database

```sql
CREATE DATABASE expense_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure Database Credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/expense_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root    # ← change to your MySQL password
```

### 3. (Optional) Enable Real Email

```properties
spring.mail.username=your-gmail@gmail.com
spring.mail.password=your-app-password     # Gmail App Password
app.mail.enabled=true
```

---

## ▶️ Run the Application

```bash
# Navigate to project folder
cd expense-management

# Run with Maven
mvn spring-boot:run
```

The app will start at **http://localhost:8080**

---

## 🔑 Demo Accounts (Auto-Seeded)

| Role | Email | Password |
|---|---|---|
| Admin | admin@expense.com | admin123 |
| Manager | manager@expense.com | manager123 |
| Employee | employee@expense.com | employee123 |
| Individual | individual@expense.com | individual123 |

---

## 📚 Application Routes

| URL | Access | Description |
|---|---|---|
| `/login` | Public | Login page |
| `/register` | Public | Create account |
| `/dashboard` | All | Role-adaptive dashboard |
| `/expenses` | All | List + search/filter expenses |
| `/expenses/new` | All | Create new expense |
| `/expenses/{id}` | All | View expense detail |
| `/expenses/{id}/edit` | Owner | Edit DRAFT expense |
| `/expenses/{id}/submit` | Owner | Submit for approval |
| `/expenses/{id}/approve` | Manager/Admin | Approve expense |
| `/expenses/{id}/reject` | Manager/Admin | Reject expense |
| `/expenses/{id}/receipt` | Owner/Manager | Download/view receipt |
| `/reports` | Manager/Admin | Expense reports |
| `/budget` | Employee/Individual | Budget management |
| `/admin/users` | Admin | User management |
| `/admin/audit` | Admin | Soft-deleted records |
| `/notifications` | All | Notification list |
| `/export/csv` | Manager/Admin | Export to CSV |
| `/export/pdf` | Manager/Admin | Export to PDF |
| `/swagger-ui.html` | All | API Documentation |

---

## 📁 Project Structure

```
expense-management/
├── src/main/java/com/expensemanager/
│   ├── config/          # Security, Cache, Swagger, Audit
│   ├── controller/      # HTTP request handlers
│   ├── dto/             # Data transfer objects
│   ├── exception/       # Custom exceptions + global handler
│   ├── model/           # JPA entities + enums
│   ├── repository/      # Spring Data JPA repositories
│   ├── scheduler/       # @Scheduled tasks
│   └── service/         # Business logic + implementations
├── src/main/resources/
│   ├── templates/       # Thymeleaf HTML templates
│   │   ├── auth/        # Login, Register
│   │   ├── dashboard/   # Role-adaptive dashboard
│   │   ├── expenses/    # List, Form, Detail
│   │   ├── reports/     # Reports management
│   │   ├── budget/      # Budget management
│   │   ├── admin/       # User mgmt, Audit trail
│   │   ├── notifications/
│   │   ├── error/       # 400, 403, 404, 500
│   │   └── layout/      # Base layout fragment
│   └── static/
│       ├── css/style.css
│       └── js/app.js
└── src/test/            # JUnit 5 + Mockito unit tests
```

---

## 🧪 Running Tests

```bash
mvn test
```

Tests use an **H2 in-memory database** (via `application-test.properties`) — no MySQL needed for tests.

---

## 📖 API Documentation

Swagger UI available at: **http://localhost:8080/swagger-ui.html**

OpenAPI JSON: **http://localhost:8080/v3/api-docs**

---

## 🔒 Security Notes

- All passwords hashed with **BCrypt (strength 12)**
- CSRF protection enabled on all POST/PUT/DELETE forms
- Session cookies: `HttpOnly`, `SameSite=Strict`
- Financial records immutable once approved
- Personal expenses never visible to corporate admins
- Role-based access enforced at both route and method level

---

## 🏗️ Technology Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3 |
| Security | Spring Security 6 |
| ORM | Spring Data JPA + Hibernate |
| Database | MySQL 8 (H2 for tests) |
| Frontend | Thymeleaf + Bootstrap 5 |
| Caching | Caffeine |
| Export | OpenPDF + Apache Commons CSV |
| API Docs | SpringDoc OpenAPI 3 |
| Build | Maven |
| Testing | JUnit 5 + Mockito |

---

## 📝 License

MIT License — Educational project based on Expense Management System SRS.
