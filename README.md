# QUANTA MRP · Módulo BOM (Bill of Materials)

Microservicio **100 % reactivo** en **Java 21 + Spring Boot 3 (WebFlux)** con **arquitectura hexagonal (Ports & Adapters)**.

Permite:

1. Crear productos.
2. Agregar materiales al BOM de un producto (cantidad por unidad).
3. Calcular los materiales requeridos para una orden de producción (`cantidad_bom × cantidad_solicitada`).
4. Consultar operarios desde la API pública `https://jsonplaceholder.typicode.com/users` con **WebClient**.

> Ejemplo: Zapato = Cuero 2, Suela 1, Cordones 1 → 100 zapatos = Cuero 200, Suela 100, Cordones 100.

---

## Requisitos

- **Java 21** (`java -version`).
- No hace falta instalar Maven: se incluye el **Maven Wrapper** (`mvnw` / `mvnw.cmd`).
- Puerto **8080** libre. La base de datos es **H2 en memoria** (R2DBC), no requiere instalación.

## Ejecución

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
./mvnw test                     # tests unitarios, de slice e integración
./mvnw clean verify             # compilación + todos los tests
```

Swagger UI (springdoc): <http://localhost:8080/swagger-ui.html>. También hay una colección lista para usar en [`requests.http`](requests.http) (IntelliJ / VS Code REST Client).

### Configuración (`application.yml`)

| Propiedad | Por defecto | Descripción |
|---|---|---|
| `spring.r2dbc.url` | `r2dbc:h2:mem:///bomdb;DB_CLOSE_DELAY=-1` | H2 en memoria vía R2DBC |
| `quanta.operators-api.base-url` | `https://jsonplaceholder.typicode.com` | URL base de la API de operarios |
| `quanta.operators-api.connect-timeout` | `2s` | Timeout de conexión TCP |
| `quanta.operators-api.read-timeout` | `5s` | Timeout de respuesta / lectura |

El esquema se crea automáticamente desde `src/main/resources/schema.sql`.

---

## Arquitectura hexagonal

```
                    ┌──────────────────────── infrastructure ────────────────────────┐
                    │                                                                │
  HTTP ──► adapter/in/web                                  adapter/out/persistence ──┼──► H2 (R2DBC)
           ProductController                               ProductPersistenceAdapter │
           ProductionController                            BomItemPersistenceAdapter │
           OperatorController                                                        │
           GlobalExceptionHandler                          adapter/out/external ─────┼──► jsonplaceholder
                    │                                      JsonPlaceholderOperatorClient (WebClient)
                    │                                                ▲               │
                    └────────────┬───────────────────────────────────┼───────────────┘
                                 ▼                                   │
                    ┌──────── domain/port/in ────────┐   ┌──── domain/port/out ────┐
                    │ CreateProductUseCase           │   │ ProductRepositoryPort   │
                    │ AddBomItemUseCase              │   │ BomItemRepositoryPort   │
                    │ CalculateProductionUseCase     │   │ OperatorClientPort      │
                    │ GetOperatorsUseCase            │   └─────────────▲───────────┘
                    └────────────┬───────────────────┘                 │
                                 ▼                                     │
                    application/service (POJOs, sin Spring) ───────────┘
                                 │
                                 ▼
                    domain/model (records + reglas)  ·  domain/exception (sealed)
```

```
src/main/java/co/quanta/mrp/bom
├── domain
│   ├── model         Product, BomItem, MaterialRequirement, ProductionCalculation, Operator
│   ├── exception     DomainException (sealed) y sus subtipos
│   └── port/in, out  Casos de uso y puertos de salida (Mono/Flux)
├── application/service   Implementaciones de los casos de uso
└── infrastructure
    ├── config        BeanConfiguration, WebClientConfig, OperatorApiProperties
    └── adapter
        ├── in/web    Controllers, DTOs, WebMapper, GlobalExceptionHandler
        └── out       persistence (R2DBC) · external (WebClient)
```

Las dependencias apuntan siempre hacia el dominio: `infrastructure → application → domain`. El dominio y la capa de aplicación no importan nada de Spring, R2DBC ni Jackson.

---

## Endpoints

| Método | URL | Body | Respuesta | HTTP |
|---|---|---|---|---|
| POST | `/products` | `{"name":"Zapato"}` | `{id,name}` + header `Location` | 201 |
| POST | `/products/{productId}/materials` | `{"material":"Cuero","quantity":2}` | `{id,productId,material,quantity}` | 201 |
| GET | `/production/calculate?productId=1&quantity=100` | — | `{product,quantity,materials:[{material,required}]}` | 200 |
| GET | `/operators` | — | `[{id,name,email,phone}]` | 200 |

### Ejemplos curl

```bash
# 1. Crear producto
curl -i -X POST localhost:8080/products -H "Content-Type: application/json" -d '{"name":"Zapato"}'
# HTTP/1.1 201 Created · Location: /products/1
# {"id":1,"name":"Zapato"}

# 2. Agregar materiales al BOM
curl -X POST localhost:8080/products/1/materials -H "Content-Type: application/json" -d '{"material":"Cuero","quantity":2}'
curl -X POST localhost:8080/products/1/materials -H "Content-Type: application/json" -d '{"material":"Suela","quantity":1}'
curl -X POST localhost:8080/products/1/materials -H "Content-Type: application/json" -d '{"material":"Cordones","quantity":1}'
# {"id":1,"productId":1,"material":"Cuero","quantity":2}

# 3. Calcular producción
curl "localhost:8080/production/calculate?productId=1&quantity=100"
# {"product":"Zapato","quantity":100,"materials":[{"material":"Cuero","required":200},
#  {"material":"Suela","required":100},{"material":"Cordones","required":100}]}

# 4. Operarios (API externa)
curl localhost:8080/operators
# [{"id":1,"name":"Leanne Graham","email":"Sincere@april.biz","phone":"1-770-736-8031 x56442"}, ...]
```

> En Windows PowerShell use `curl.exe` y escape las comillas del JSON, o use `requests.http`.

### Errores

Todas las respuestas de error tienen el mismo formato:

```json
{"timestamp":"2026-10-07T17:22:45.92Z","status":404,"error":"Not Found",
 "message":"Product with id 99 not found","path":"/products/99/materials"}
```

| HTTP | Cuándo |
|---|---|
| 400 | `name`/`material` vacío, `quantity <= 0`, parámetros faltantes o no numéricos, JSON mal formado |
| 404 | El producto no existe |
| 409 | Producto con nombre repetido, o material ya presente en el BOM (sin distinguir mayúsculas) |
| 502 | La API de operarios responde con error o no es alcanzable |
| 504 | La API de operarios supera el timeout configurado |
| 500 | Error inesperado (se registra en el log, no se expone el detalle) |

---

## Decisiones de diseño

- **Records** para modelos de dominio, entidades R2DBC y DTOs: inmutables y sin boilerplate (sin Lombok). Los modelos validan sus invariantes en el constructor compacto (nombre no vacío, cantidad > 0).
- **Lógica de negocio en el dominio**: `BomItem.requiredFor(units)` calcula el requerimiento con `Math.multiplyExact` sobre `long`, de modo que un desbordamiento falla explícitamente en lugar de producir un número incorrecto.
- **Excepciones `sealed`**: `DomainException` declara todos sus subtipos. El `GlobalExceptionHandler` usa un `switch` exhaustivo, así que añadir una excepción nueva no compila hasta que se le asigna un código HTTP.
- **Servicios de aplicación sin Spring**: son POJOs `final` registrados como beans en `BeanConfiguration`; se prueban con Mockito sin levantar contexto.
- **Reactor en los puertos**: es la única dependencia externa del dominio. Se acepta de forma deliberada porque `Mono`/`Flux` es el contrato asíncrono de todo el sistema; abstraerlo añadiría conversiones sin aportar valor. El dominio sigue sin depender de Spring, R2DBC ni Jackson.
- **Errores como señales reactivas**: las validaciones del dominio se ejecutan dentro de `Mono.fromSupplier`, de modo que llegan como `onError` y no como excepciones lanzadas fuera del pipeline. Se usa `switchIfEmpty(Mono.error(...))` para el producto inexistente.
- **Unicidad del BOM**: el servicio comprueba si el material ya existe (sin distinguir mayúsculas) y, además, la tabla tiene la restricción `UNIQUE (product_id, material)` como red de seguridad ante inserciones concurrentes. El adapter traduce `DataIntegrityViolationException` a `DuplicateResourceException` (409).
- **WebClient**: definido una sola vez como `@Bean` con `ReactorClientHttpConnector` (timeout de conexión, `responseTimeout` y `ReadTimeoutHandler`), además del operador `timeout` de Reactor sobre todo el flujo. `onStatus` convierte respuestas 4xx/5xx en `ExternalServiceException` (502) y los timeouts en `ExternalServiceTimeoutException` (504). El DTO externo ignora los campos desconocidos y se mapea DTO → dominio → respuesta. No se añadieron reintentos para mantener predecible la latencia máxima del endpoint.
- **Validación en el borde**: Bean Validation en los DTOs de entrada y en los query params; el dominio vuelve a validar sus invariantes, por lo que nunca se crea un modelo inválido aunque cambie el adaptador de entrada.

## Tests

| Capa | Tipo | Herramientas |
|---|---|---|
| Dominio | Unitarios | JUnit 5 |
| Aplicación | Unitarios | StepVerifier + Mockito |
| Persistencia | `@DataR2dbcTest` sobre H2 | StepVerifier |
| Cliente externo | OK / 500 / timeout / servidor caído | MockWebServer + StepVerifier |
| Web | `@WebFluxTest`: códigos y cuerpos HTTP | WebTestClient |
| End-to-end | `@SpringBootTest(RANDOM_PORT)` con la API externa simulada | WebTestClient + MockWebServer |
