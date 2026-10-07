# QUANTA MRP · Módulo BOM (Bill of Materials)

Microservicio **100 % reactivo** en **Java 21 + Spring Boot 3 (WebFlux)** con **arquitectura hexagonal (Ports & Adapters)**.

Permite:

1. Crear productos.
2. Agregar materiales al BOM de un producto (cantidad por unidad).
3. Calcular los materiales requeridos para una orden de producción (`cantidad_bom × cantidad_solicitada`).
4. Consultar operarios desde la API pública `https://jsonplaceholder.typicode.com/users` con **WebClient**.

> Ejemplo: Zapato = Cuero 2, Suela 1, Cordones 1 → 100 zapatos = Cuero 200, Suela 100, Cordones 100.

## Despliegue con Docker Compose (backend + frontend)

La forma más rápida de ver la app funcionando. Levanta dos contenedores:

| Servicio | Imagen | Puerto host | Descripción |
|---|---|---|---|
| `backend` | `quanta-bom-service` (raíz, `Dockerfile`) | `8080` | API WebFlux + H2 en memoria |
| `frontend` | `quanta-bom-frontend` (`frontend/Dockerfile`) | `3000` | HTML/JS estático servido por nginx |

```
Navegador ──► http://localhost:3000 (nginx)
                 ├── /         → index.html, app.js, styles.css
                 └── /api/*    → proxy → http://backend:8080/*  (red interna de compose)
```

El frontend llama siempre a `/api/...` y nginx reenvía al backend, por eso no hace falta configurar CORS. El `frontend` espera a que el `backend` pase su healthcheck antes de arrancar.

### Requisitos

- Docker y Docker Compose v2 (`docker compose version`).
- Puertos **3000** y **8080** libres.
- Acceso a Internet para descargar imágenes y para que `/operators` consulte jsonplaceholder.

### Paso a paso

```bash
git clone https://github.com/juansanchez091711/backend-technical.git
cd backend-technical

docker compose up -d --build      # construye ambas imágenes y levanta los contenedores
docker compose ps                 # backend debe aparecer como "healthy"
```

Luego abre:

- Frontend: <http://localhost:3000>
- API directa / Swagger UI: <http://localhost:8080/swagger-ui.html>

### Flujo de demostración en el frontend

1. **Crear producto**: `Zapato`.
2. **Agregar materiales**: Cuero 2, Suela 1, Cordones 1.
3. **Calcular producción**: 100 unidades → Cuero 200, Suela 100, Cordones 100.
4. **Consultar operarios**: muestra los usuarios de jsonplaceholder.

El panel "Registro de peticiones" muestra cada llamada con su código HTTP y el JSON de respuesta. Así también se ven los errores: 400 con cantidad ≤ 0, 409 con material duplicado, etc.

> El backend no tiene `GET /products`, así que el frontend solo lista los productos creados en la sesión actual del navegador. Los datos viven en H2 en memoria y se pierden al reiniciar el contenedor.

### Operación

```bash
docker compose logs -f backend    # logs del backend
docker compose restart backend    # reiniciar (borra los datos en memoria)
docker compose down               # detener y eliminar contenedores
docker compose up -d --build      # redesplegar tras cambios en el código
```

Para cambiar la configuración del cliente externo, edita las variables `QUANTA_OPERATORSAPI_*` en `docker-compose.yml`. Para cambiar los puertos expuestos, edita las secciones `ports`.

---

## Ejecución solo del backend con Docker

### Requisitos

- [Docker](https://docs.docker.com/get-docker/) instalado y en ejecución.
- Puerto **8080** libre.

No necesitas Java ni Maven: la imagen compila el proyecto. La base de datos es H2 en memoria y no hace falta instalarla.

### Paso a paso

1. **Clonar el repositorio**

   ```bash
   git clone https://github.com/juansanchez091711/backend-technical.git
   cd backend-technical
   ```

2. **Construir la imagen**

   ```bash
   docker build -t quanta-bom-service .
   ```

3. **Ejecutar el contenedor**

   ```bash
   docker run -d --name quanta-bom -p 8080:8080 quanta-bom-service
   ```

4. **Verificar**

   - Swagger UI: <http://localhost:8080/swagger-ui.html>
   - Logs: `docker logs -f quanta-bom`

5. **Detener y eliminar el contenedor**

   ```bash
   docker stop quanta-bom && docker rm quanta-bom
   ```

### Configuración opcional

Puedes cambiar propiedades con variables de entorno, por ejemplo:

```bash
docker run -d --name quanta-bom -p 8080:8080 \
  -e QUANTA_OPERATORSAPI_BASEURL=https://jsonplaceholder.typicode.com \
  quanta-bom-service
```
