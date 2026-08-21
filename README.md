# Kiert Backend (Spring Boot)

Backend para la comunidad **Kiert**, construido para calzar exactamente con el
frontend Angular que ya tienes (`auth.service.ts`, `post.service.ts`,
`chat.service.ts`, `upload.service.ts`, etc). Usa JWT + BCrypt, MySQL en
producción (H2 en memoria para desarrollo rápido), y WebSocket/STOMP para el
chat en tiempo real.

> ⚠️ **Nota honesta**: no tengo acceso a Maven Central desde este entorno
> (está bloqueado), así que no pude correr `mvn compile` para verificarlo de
> punta a punta. Sí revisé cada uno de los 62 archivos línea por línea a mano
> (imports, tipos, firmas de métodos). La primera vez que lo corras en tu
> máquina, si algo no compila, probablemente sea algo menor y fácil de
> arreglar — perdón de antemano si aparece algo así.

## Cómo levantarlo YA (sin instalar MySQL)

```bash
cd kiert-backend
mvn spring-boot:run
```

Por defecto corre con el perfil `dev`: base de datos H2 en memoria, con datos
de ejemplo precargados (`data.sql.txt`) para que el feed y el chat no salgan
vacíos. Queda escuchando en `http://localhost:8080`.

- Consola H2 (ver las tablas): `http://localhost:8080/h2-console`
  (JDBC URL: `jdbc:h2:mem:kiert`, usuario `sa`, sin contraseña)
- Usuarios de prueba (contraseña para los tres: `Password1`):
  `admin@kiert.com`, `ana@kiert.com`, `kai@kiert.com`

## Cómo correrlo con MySQL de verdad

1. Edita `src/main/resources/application.yml`, perfil `mysql` (usuario/clave
   de tu MySQL local), o pásalos por variables de entorno.
2. Corre:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```
   Crea la base `kiert` sola si no existe (`createDatabaseIfNotExist=true`).

## Conectar el Angular

En tu `environment.ts` / `environment.development.ts` (frontend), apunta:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  ...
};
```

Y en el frontend, en `post.service.ts` y `auth.service.ts`, descomenta las
líneas `this.http...` y borra los métodos `mock...` — el backend ya expone
exactamente esas rutas.

## Rutas expuestas

| Método | Ruta | Auth | Para qué componente/service del frontend |
|---|---|---|---|
| POST | `/api/auth/registro` | pública | `register.component.ts` |
| POST | `/api/auth/login` | pública | `login.component.ts` |
| POST | `/api/auth/recuperar-contrasena` | pública | `forgot-password.component.ts` |
| POST | `/api/auth/restablecer-contrasena` | pública | `reset-password.component.ts` |
| GET | `/api/publicaciones` | pública | `feed.component.ts` |
| GET | `/api/publicaciones/{id}` | pública | `post-detail.component.ts` |
| POST | `/api/publicaciones` (multipart) | JWT | `create-post.component.ts` |
| GET | `/api/publicaciones/{id}/comentarios` | pública | `post-detail.component.ts` |
| POST | `/api/publicaciones/{id}/comentarios` | JWT | `post-detail.component.ts` |
| POST | `/api/archivos/url-firmada` | JWT | `upload.service.ts` (foto de perfil) |
| GET / PATCH | `/api/perfil` | JWT | `profile.component.ts` |
| GET | `/api/chat/conversaciones` | JWT | `chat.component.ts` |
| GET / POST | `/api/chat/{usuarioId}` | JWT | `chat.component.ts` |
| WS | `/ws` (SockJS/STOMP) | — | tiempo real del chat |

Todas las rutas protegidas esperan el header `Authorization: Bearer <token>`
— exactamente lo que ya arma `auth.interceptor.ts` en tu frontend.

## Configuración importante antes de producción

En `application.yml`:
- `kiert.jwt.secret`: cámbialo por un secreto real (≥32 caracteres).
- `kiert.supabase.url` / `bucket` / `service-role-key`: tus credenciales
  reales de Supabase Storage (el service-role-key **nunca** debe ir al
  frontend, solo vive aquí en el backend).
- `spring.mail.*` (agrégalo si usas Gmail/SES/etc para el correo de
  recuperación de contraseña — no vino configurado porque depende de tu
  proveedor).
- Perfil `mysql`: usuario/clave de tu base de datos.

## Estructura del proyecto

```
src/main/java/com/kiert/backend/
├── entity/       Usuario, Post, Adjunto, Comentario, Mensaje, PasswordResetToken
├── repository/   Spring Data JPA
├── dto/          records — espejo de los modelos TS del frontend
├── service/      lógica de negocio (Auth, Post, Chat, Storage, Email, Perfil)
├── controller/   REST + WebSocket
├── security/     JWT, filtro, UserDetails
├── config/       Security, CORS, WebSocket, WebClient
└── exception/    manejo global de errores (404/401/409/400)
```
