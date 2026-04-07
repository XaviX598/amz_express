# Amz Express

Sistema de importación de productos desde Amazon a Ecuador.

## 🚀 Quick Start

### Prerequisites
- Docker y Docker Compose
- Java 21 (si ejecutás sin Docker)
- Node.js 18+ (si ejecutás sin Docker)

### Con Docker (Recomendado)

```bash
# En el directorio raíz
export APP_MASTER_SECURITY_CODE="cambia-este-codigo"
docker-compose up -d
```

Esto inicia:
- Frontend en http://localhost:5173
- Backend en http://localhost:8080
- PostgreSQL en puerto 5432

### Sin Docker

#### 1. Configurar PostgreSQL

```sql
CREATE DATABASE amzexpress;
```

#### 2. Backend

```bash
cd backend
cp .env.example .env
# Editar .env con tu password de PostgreSQL
export APP_MASTER_SECURITY_CODE="cambia-este-codigo"

mvn spring-boot:run
```

#### 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

## 📁 Estructura del Proyecto

```
amz-express-spring/
├── backend/              # Spring Boot API
│   ├── src/main/java/com/amzexpress/
│   │   ├── config/       # Configuración
│   │   ├── controller/   # REST Controllers
│   │   ├── dto/          # Data Transfer Objects
│   │   ├── entity/       # JPA Entities
│   │   ├── exception/    # Exception Handling
│   │   ├── repository/   # JPA Repositories
│   │   ├── security/     # JWT Security
│   │   └── service/      # Business Logic
│   └── src/main/resources/
│       └── application.properties
│
├── frontend/             # Vue 3 + Vite
│   ├── src/
│   │   ├── components/   # Vue Components
│   │   ├── views/       # Page Views
│   │   ├── stores/      # Pinia Stores
│   │   ├── services/    # API Services
│   │   └── router/      # Vue Router
│   └── ...
│
├── docker-compose.yml
└── README.md
```

## 🔐 Credenciales

### SuperAdmin (creado automáticamente)
- **Email:** admin@amzexpress.com
- **Password:** SuperAdmin123!

## 📋 Features

- ✅ Sistema de autenticación JWT
- ✅ Roles: USER, ADMIN, SUPERADMIN
- ✅ Calculadora de precios en tiempo real
- ✅ Gestión de pedidos con tracking
- ✅ Panel de administración
- ✅ Panel de gestión de usuarios (SuperAdmin)

## 💰 Fórmula de Precios

```
Total = Precio Producto + Impuestos (15%) + Handling (9.27%) + Envío + Aduana ($21)
```

- **Envío:** $5/lb (máx $40)
- **Categoría C:** >8 lbs o >$400 = Contactar por WhatsApp

## 📝 API Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/register` | Registro |
| POST | `/api/auth/login` | Login |
| GET | `/api/orders` | Mis pedidos |
| POST | `/api/orders` | Crear pedido |
| GET | `/api/orders/admin/all` | Todos los pedidos |
| PUT | `/api/orders/admin/{id}/status` | Actualizar estado |
| GET | `/api/users` | Listar usuarios |
| PUT | `/api/users/{id}/role` | Cambiar rol |

## 🛠️ Tech Stack

**Backend:**
- Java 21
- Spring Boot 3.2
- Spring Security
- JWT (jjwt)
- Spring Data JPA
- PostgreSQL

**Frontend:**
- Vue 3
- TypeScript
- Vite
- TailwindCSS
- Pinia
- Vue Router

## 📜 Licencia

MIT

## Deploy 1-click (GitHub + Vercel + Oracle)

Desde la raíz del repo:

```powershell
.\deploy-prod.ps1 -CommitMessage "fix: tu mensaje"
```

Opciones útiles:

```powershell
# Simular sin ejecutar nada
.\deploy-prod.ps1 -DryRun

# Solo push a GitHub (sin backend Oracle)
.\deploy-prod.ps1 -SkipBackend

# Solo backend Oracle (sin operaciones git)
.\deploy-prod.ps1 -SkipGit
```
