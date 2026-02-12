# Sara Elevator Backend API

Spring Boot REST API for Sara Elevator Management System. Common backend for Web Admin Panel and Mobile Application.

## 🛠️ Technologies

- **Java 17**
- **Spring Boot 3.2**
- **PostgreSQL** - Production database
- **Flyway** - Database migrations
- **JPA/Hibernate** - ORM
- **JWT** - Authentication (Access + Refresh Token)
- **Apache PDFBox** - PDF generation
- **OpenAPI/Swagger** - API documentation
- **Docker & Docker Compose** - Local development

## 📋 Features

### ✅ Authentication & Authorization
- JWT-based authentication
- Access Token (1 hour)
- Refresh Token (7 days)
- Role-based authorization (PATRON, PERSONEL)

### ✅ Entity Management
- **Users**: User management (PATRON only)
- **Elevators**: Elevator CRUD operations
- **Maintenances**: Maintenance records
- **Parts**: Parts and stock management
- **Offers**: Offer management
- **File Attachments**: File attachments
- **Audit Logs**: Audit records

### ✅ Business Logic
- Periodic control system (EXPIRED/WARNING/OK)
- Automatic expiry date calculation (inspectionDate + 12 months)
- Payment tracking (income/debt calculation)
- Dashboard summary information

### ✅ API Endpoints
- Auth: `/auth/login`, `/auth/refresh`, `/auth/logout`
- Elevators: CRUD + status check + warnings
- Maintenances: CRUD + mark paid
- Dashboard: Summary statistics
- OpenAPI docs: `/swagger-ui.html`

## 🚀 Quick Start

### Requirements

- Java 17+
- Maven 3.6+
- Docker & Docker Compose (optional)

### Running with Docker (Recommended)

```bash
cd backend
docker-compose up -d
```

This command:
- Starts PostgreSQL database
- Runs Flyway migrations
- Starts the application

API: `http://localhost:8080/api`

### Manual Setup

1. Start PostgreSQL:
```bash
docker-compose up -d postgres
```

2. Run the application:
```bash
mvn spring-boot:run
```

Or build JAR:
```bash
mvn clean package
java -jar target/sara-asansor-api-1.0.0.jar
```

## 📚 API Documentation

After the application starts:

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/api-docs

## 🔐 Default Users

**Automatically created in development environment:**

- **PATRON (Admin)**:
  - Username: `patron`
  - Password: `password`

> ⚠️ **Security**: Delete these users or change their passwords in production!

## 📡 API Examples

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "patron",
    "password": "password"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "patron",
    "role": "PATRON"
  }
}
```

### Elevator List (Auth required)

```bash
curl -X GET http://localhost:8080/api/elevators \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Dashboard Summary

```bash
curl -X GET http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 🔧 Configuration

Main configuration file: `src/main/resources/application.yml`

Important settings:
- Database URL: `spring.datasource.url`
- JWT Secret: `app.jwt.secret` (change this in production!)
- File Storage: `app.file-storage.type` (local or s3)

## 📦 Database

- **Migration Files**: `src/main/resources/db/migration/`
- **V1__init_schema.sql**: Table schema
- **V2__seed_data.sql**: Seed data (dev only)

## 🧪 Test

```bash
mvn test
```

## 📝 API Response Format

All API responses follow this format:

```json
{
  "success": true/false,
  "message": "Operation message",
  "data": { ... },
  "errors": { ... }
}
```

## 🔒 Security

- JWT tokens are stateless
- Passwords are hashed with BCrypt
- Role-based access control
- CORS configuration

## 🚧 Development

### Adding New Entity

1. Create model class (`model/`)
2. Create repository interface (`repository/`)
3. Create service class (`service/`)
4. Create controller (`controller/`)
5. Create Flyway migration file

### Adding New Endpoint

Add new method to controller, Spring Security automatically protects it.

## 📞 Contact

**Company**: Sara Elevator  
**Location**: Central Iğdır

---

**Sara Elevator Backend API v1.0.0**  
*Spring Boot REST API*
