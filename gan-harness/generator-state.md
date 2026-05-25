# Generator State — Iteration 001

## What Was Built

### Backend (Spring Boot 3.2 + MyBatis-Plus + JWT)
- Complete Maven project with all dependencies (Spring Boot, MyBatis-Plus, JWT, Knife4j, Hutool, Lombok)
- MySQL/H2 dual-profile configuration (dev uses H2 in-memory, prod uses MySQL)
- Database schema and seed data (5 developers, 15 properties, 3 customers, 5 suggestions)
- Entity classes: Developer, Customer, Property, Suggestion, ActivityLog, Report
- Unified API response format `Result<T>` with `{code, message, data}`
- JWT authentication filter and security configuration (role-based: ROLE_DEVELOPER, ROLE_CUSTOMER)
- CORS configuration for cross-origin access from frontend
- Global exception handler with proper HTTP status codes
- Service implementations with full business logic (auto vacancy rate calculation, profile management)
- RESTful controllers: Auth, Developer, Property, Customer, Suggestion
- Property multi-condition query with pagination, sorting, and fuzzy search
- Vacancy rate statistics endpoints (by location, type, floor scatter)
- Activity logging service for audit trail
- Swagger/Knife4j API documentation at /doc.html

### Frontend (Vue 3 + TypeScript + Element Plus + ECharts)
- Vite project with Vue 3, Composition API, `<script setup>` syntax
- TypeScript type definitions for all data models
- Axios HTTP client with JWT interceptor and 401 auto-redirect
- Pinia auth store for user state management
- Vue Router with role-based navigation guards
- Element Plus UI framework with custom color scheme (#1a73e8 primary)
- ECharts integration for vacancy correlation analytics
- Sidebar layout with role-based menu items
- Login page with role selector (developer/customer)
- Registration page with password strength indicator and username uniqueness check
- Developer views: Dashboard (metric cards), Property CRUD (table + form), Analytics (3 charts), Profile (company info), Suggestions
- Customer views: Dashboard, Property Search (multi-condition), Guided Wizard (4-step), Developer List, Suggestions

## What Changed This Iteration
- Initial implementation of the complete project

## Known Issues
- Image upload not yet implemented (Nice-to-Have for Sprint 5)
- Dark mode not implemented (Nice-to-Have for Sprint 5)
- Report/PDF export uses basic browser print only
- Property image management is placeholder only
- No Excel export implementation

## Dev Server
- Backend URL: http://localhost:8088
- Frontend URL: http://localhost:3000
- Status: both running
- Backend command: `mvn spring-boot:run` (from backend/)
- Frontend command: `npx vite --port 3000 --host` (from frontend/)
- API Docs: http://localhost:8088/doc.html
- H2 Console: http://localhost:8088/h2-console

## Seed Accounts
- Developer: bgy_admin / 123456
- Customer: zhangsan / 123456
