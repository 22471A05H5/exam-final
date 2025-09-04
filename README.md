# Exam Management System

A comprehensive Spring Boot application for managing exams with role-based access control.

## Features

- **Role-based Access**: SuperAdmin, Principal, HOD, Assistant, Faculty, Student
- **Exam Management**: Create, take, and grade exams
- **AI Question Generation**: Powered by Google Gemini API
- **Real-time Chat**: Communication between students and admins
- **Results & Analytics**: Detailed performance tracking

## Tech Stack

- Spring Boot 3.5.5
- MySQL (local) / PostgreSQL (production)
- Spring Security
- Thymeleaf Templates
- Google Gemini AI API

## Deployment

Configured for Render deployment with PostgreSQL database.

### Environment Variables Required

```
DATABASE_URL=your_postgresql_connection_string
PGUSER=your_db_username
PGPASSWORD=your_db_password
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
GEMINI_API_KEY=your_gemini_api_key
```

## Local Development

Uses MySQL database with default configuration in `application.properties`.
