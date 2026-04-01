# Amz Express Backend

## Requisitos
- Java 21
- Maven 3.8+
- PostgreSQL 14+

## Configuración de Base de Datos

1. Crear la base de datos PostgreSQL:
```sql
CREATE DATABASE amzexpress;
```

2. Configurar las variables de entorno o editar `src/main/resources/application.properties`:

```bash
# Variables de entorno
export DB_URL=jdbc:postgresql://localhost:5432/amzexpress
export DB_USERNAME=postgres
export DB_PASSWORD=tu_password
export JWT_SECRET=tu-secret-key-de-al-menos-32-caracteres
```

## Ejecutar la aplicación

```bash
# En el directorio backend/
mvn spring-boot:run
```

O compilar y ejecutar el JAR:
```bash
mvn clean package
java -jar target/amz-express-1.0.0.jar
```

## Endpoints de la API

### Autenticación
- `POST /api/auth/register` - Registro de usuario
- `POST /api/auth/login` - Iniciar sesión

### Órdenes
- `GET /api/orders` - Obtener órdenes del usuario (autenticado)
- `POST /api/orders` - Crear nueva orden (autenticado)
- `GET /api/orders/admin/all` - Todas las órdenes (ADMIN)
- `PUT /api/orders/admin/{id}/status` - Actualizar estado (ADMIN)

### Usuarios
- `GET /api/users` - Listar usuarios (SUPERADMIN)
- `PUT /api/users/{id}/role` - Cambiar rol (SUPERADMIN)

### Precios
- `POST /api/pricing/calculate` - Calcular precio

## Credenciales por Defecto

Al iniciar, se crea automáticamente un SuperAdmin:
- **Email:** admin@amzexpress.com
- **Password:** SuperAdmin123!

⚠️ **IMPORTANTE:** Cambiar esta contraseña en producción.
