# CLAUDE.md — QUANTA MRP · Módulo BOM (Prueba Técnica Backend Java 21)

## 1. Descripción del proyecto

Microservicio **reactivo** en **Java 21 + Spring Boot 3 (WebFlux)** con **arquitectura hexagonal (Ports & Adapters)** para el módulo **Bill of Materials (BOM)** de QUANTA MRP.

Funcionalidades:
1. Crear productos.
2. Agregar materiales al BOM de un producto (cantidad por unidad).
3. Calcular materiales requeridos para una orden de producción (`cantidad_bom × cantidad_solicitada`).
4. Consultar operarios desde la API pública `https://jsonplaceholder.typicode.com/users` con **WebClient** reactivo.

Ejemplo: Zapato = Cuero 2, Suela 1, Cordones 1 → 100 zapatos = Cuero 200, Suela 100, Cordones 100.

## 2. Reglas no negociables

- **Java 21** (records para DTOs/modelos, `sealed` para excepciones de dominio si aplica, `var` donde mejore legibilidad).
- **Spring Boot 3.x** con `spring-boot-starter-webflux`.
- Persistencia: **R2DBC + H2** (`spring-boot-starter-data-r2dbc` + `io.r2dbc:r2dbc-h2`).
- **PROHIBIDO**: JPA, Hibernate, JDBC clásico, `spring-boot-starter-web` (MVC), `.block()`, `.subscribe()` manual en código productivo, `Thread.sleep`, cualquier llamada bloqueante.
- **Toda la cadena** (controllers → use cases → puertos → adapters) retorna `Mono<T>` o `Flux<T>`.
- **El dominio no depende de Spring ni de ningún framework** (sin anotaciones de Spring, R2DBC ni Jackson en `domain`).
- WebClient definido como **@Bean** (nunca instanciado por llamada); URL base y timeout configurables en `application.yml`.
- Tests: **JUnit 5 + StepVerifier** (mínimo uno; objetivo: varios).
- `schema.sql` en `src/main/resources` para inicialización automática.

## 3. Stack y build

| Elemento | Elección |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3.x |
| Build | Maven (con Maven Wrapper `mvnw`) |
| Web | WebFlux (Netty) |
| Persistencia | Spring Data R2DBC + r2dbc-h2 (in-memory) |
| HTTP client | WebClient (+ timeout de Netty / operador `timeout`) |
| Validación | `spring-boot-starter-validation` |
| Tests | JUnit 5, reactor-test (StepVerifier), Mockito, WebTestClient, MockWebServer (okhttp) |
| Docs API (opcional) | springdoc-openapi-starter-webflux-ui |

Paquete raíz: `co.quanta.mrp.bom`

## 4. Estructura hexagonal

```
src/main/java/co/quanta/mrp/bom
├── BomApplication.java
├── domain
│   ├── model            # Product, BomItem, MaterialRequirement, ProductionCalculation, Operator (records)
│   ├── exception        # DomainException (sealed), ProductNotFoundException, InvalidQuantityException, ExternalServiceException
│   └── port
│       ├── in           # CreateProductUseCase, AddBomItemUseCase, CalculateProductionUseCase, GetOperatorsUseCase
│       └── out          # ProductRepositoryPort, BomItemRepositoryPort, OperatorClientPort
├── application
│   └── service          # Implementaciones de los casos de uso (POJOs, sin anotaciones Spring)
└── infrastructure
    ├── config           # BeanConfiguration (registra servicios), WebClientConfig, OperatorApiProperties
    ├── adapter
    │   ├── in/web       # ProductController, ProductionController, OperatorController
    │   │   ├── dto      # Request/Response records
    │   │   ├── mapper
    │   │   └── GlobalExceptionHandler (@RestControllerAdvice)
    │   └── out
    │       ├── persistence  # ProductEntity, BomItemEntity, R2DBC repos, adapters que implementan puertos
    │       └── external     # JsonPlaceholderOperatorClient, ExternalUserDto
src/main/resources
├── application.yml
└── schema.sql
src/test/java/...       # tests unitarios (StepVerifier) e integración (WebTestClient)
```

Los servicios de `application` se registran como beans en `infrastructure/config/BeanConfiguration` para que no tengan dependencia de Spring.

## 5. Modelo de datos (`schema.sql`)

```sql
CREATE TABLE IF NOT EXISTS product (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS bom_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    material   VARCHAR(150) NOT NULL,
    quantity   INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_bom_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT uq_product_material UNIQUE (product_id, material)
);
```

## 6. Endpoints

| Método | URL | Body | Respuesta | HTTP |
|---|---|---|---|---|
| POST | `/products` | `{"name":"Zapato"}` | `Mono<ProductResponse>` `{id,name}` | 201 (+ header `Location`) |
| POST | `/products/{productId}/materials` | `{"material":"Cuero","quantity":2}` | `Mono<BomItemResponse>` `{id,productId,material,quantity}` | 201 |
| GET | `/production/calculate?productId=1&quantity=100` | — | `Mono<ProductionCalculationResponse>` `{product,quantity,materials:[{material,required}]}` | 200 |
| GET | `/operators` | — | `Flux<OperatorResponse>` `{id,name,email,phone}` | 200 |

Errores (formato uniforme `ErrorResponse {timestamp,status,error,message,path}`):
- 400: validación (`name` vacío, `quantity <= 0`, parámetros faltantes/ inválidos).
- 404: producto inexistente.
- 409: producto duplicado / material ya existente en el BOM.
- 502: API externa responde con error. 504: timeout de la API externa.

## 7. Fases del proyecto

> **Regla en cada fase:** al finalizar se ejecuta `./mvnw clean verify` (en Windows `mvnw.cmd clean verify`). **No se avanza a la siguiente fase si hay errores de compilación o tests fallidos.** Cada fase termina con un commit Git descriptivo (historial visible es requisito de entrega).

### Fase 0 — Preparación
- [ ] Verificar `java -version` = 21 y Maven/Wrapper disponible.
- [ ] `git init`, `.gitignore` (target/, .idea/, *.iml, .vscode/).
- **Verificación:** `java -version` muestra 21.
- **Commit:** `chore: initial repository setup`

### Fase 1 — Scaffold del proyecto
- [ ] `pom.xml` con Spring Boot 3.3.x, `java.version=21`, dependencias: webflux, data-r2dbc, r2dbc-h2, validation, lombok NO (usar records), reactor-test, mockwebserver, springdoc (opcional).
- [ ] Maven Wrapper (`mvn wrapper:wrapper`).
- [ ] `BomApplication.java`, `application.yml` (puerto, r2dbc url `r2dbc:h2:mem:///bomdb;DB_CLOSE_DELAY=-1`, `spring.sql.init.mode=always`, `quanta.operators-api.base-url`, `connect-timeout`, `read-timeout`).
- [ ] `schema.sql`.
- [ ] Confirmar que NO existe dependencia JPA/JDBC/MVC (`./mvnw dependency:tree | findstr /i "jpa hibernate spring-webmvc"` debe salir vacío).
- **Verificación:** `./mvnw clean verify` OK y la app arranca (`./mvnw spring-boot:run`) creando las tablas.
- **Commit:** `chore: scaffold Spring Boot WebFlux + R2DBC project`

### Fase 2 — Dominio (puro Java)
- [ ] Records: `Product(Long id, String name)`, `BomItem(Long id, Long productId, String material, int quantity)`, `MaterialRequirement(String material, long required)`, `ProductionCalculation(String product, int quantity, List<MaterialRequirement> materials)`, `Operator(Long id, String name, String email, String phone)`.
- [ ] Validaciones invariantes en constructores compactos (nombre no vacío, cantidad > 0).
- [ ] Lógica de negocio en el dominio: `BomItem.requiredFor(int units)` → `MaterialRequirement` (usar `long`/`Math.multiplyExact` para evitar overflow).
- [ ] Excepciones `sealed` de dominio.
- [ ] Puertos `in` y `out` como interfaces que retornan `Mono`/`Flux` (Reactor es la única dependencia permitida en dominio, justificada en README).
- **Verificación:** `./mvnw clean verify`; revisar que `domain` no importa `org.springframework.*`.
- **Commit:** `feat(domain): add BOM domain model, exceptions and ports`

### Fase 3 — Casos de uso (application)
- [ ] `CreateProductService`, `AddBomItemService` (valida que el producto exista → `ProductNotFoundException`), `CalculateProductionService` (busca producto, obtiene `Flux<BomItem>`, mapea a requerimientos, `collectList`, arma `ProductionCalculation`; producto sin BOM → lista vacía), `GetOperatorsService`.
- [ ] Uso de `switchIfEmpty(Mono.error(...))`, `flatMap`, `map`, sin `block()`.
- [ ] **Tests unitarios con StepVerifier + Mockito** para cada servicio (caso feliz, producto no encontrado, cantidad inválida, ejemplo Zapato×100 = 200/100/100).
- **Verificación:** `./mvnw clean verify` con tests en verde.
- **Commit:** `feat(application): implement BOM use cases with StepVerifier tests`

### Fase 4 — Adaptador de persistencia (R2DBC)
- [ ] Entidades `@Table` `ProductEntity`, `BomItemEntity` (records en infraestructura).
- [ ] `ReactiveCrudRepository` / `R2dbcRepository`: `findByProductId(Long)`, `existsByProductIdAndMaterialIgnoreCase`.
- [ ] Adapters `ProductPersistenceAdapter`, `BomItemPersistenceAdapter` que implementan puertos `out` y mapean entidad ↔ dominio.
- [ ] Traducir `DataIntegrityViolationException` → excepción de dominio de conflicto (`onErrorMap`).
- [ ] Test `@DataR2dbcTest` con StepVerifier sobre los adapters.
- **Verificación:** `./mvnw clean verify`.
- **Commit:** `feat(persistence): add R2DBC H2 adapters`

### Fase 5 — Adaptador externo WebClient (★ diferenciador)
- [ ] `OperatorApiProperties` (`@ConfigurationProperties("quanta.operators-api")`: `baseUrl`, `connectTimeout`, `readTimeout`).
- [ ] `WebClientConfig`: `@Bean WebClient operatorsWebClient(...)` con `ReactorClientHttpConnector` (connect timeout, `ReadTimeoutHandler`, `responseTimeout`).
- [ ] `JsonPlaceholderOperatorClient implements OperatorClientPort`:
  - `.get().uri("/users").retrieve()`
  - `.onStatus(HttpStatusCode::isError, ...)` → `ExternalServiceException`
  - `.bodyToFlux(ExternalUserDto.class)`
  - `.map(this::toDomain)` (DTO externo record con `@JsonIgnoreProperties(ignoreUnknown = true)`)
  - `.timeout(Duration)` + `onErrorMap(TimeoutException → ExternalServiceTimeoutException)` y `onErrorMap(WebClientRequestException → ExternalServiceException)`
  - (Opcional) `retryWhen(Retry.backoff(2, 200ms))` solo para errores transitorios.
- [ ] Tests con **MockWebServer** + StepVerifier: respuesta OK mapeada, 500 → error mapeado, respuesta lenta → timeout.
- **Verificación:** `./mvnw clean verify`.
- **Commit:** `feat(external): add reactive WebClient operators adapter with timeout and error handling`

### Fase 6 — Adaptadores web (controllers + DTOs + errores)
- [ ] DTOs records: `CreateProductRequest(@NotBlank name)`, `AddBomItemRequest(@NotBlank material, @Positive quantity)`, `ProductResponse`, `BomItemResponse`, `ProductionCalculationResponse`, `MaterialRequirementResponse`, `OperatorResponse`, `ErrorResponse`.
- [ ] Controllers anotados (`@RestController`) retornando `Mono`/`Flux`, `@ResponseStatus(CREATED)` o `ResponseEntity.created(URI)` en POSTs; `@Validated` + `@Positive`/`@NotNull` en query params de `/production/calculate`.
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`) reactivo: `WebExchangeBindException`, `ServerWebInputException`, `ConstraintViolationException`/`HandlerMethodValidationException` → 400; `ProductNotFoundException` → 404; conflicto → 409; `ExternalServiceException` → 502; timeout → 504; genérico → 500.
- [ ] `BeanConfiguration` registra los servicios de aplicación.
- [ ] Tests `@WebFluxTest` con `WebTestClient` (códigos HTTP y cuerpos).
- **Verificación:** `./mvnw clean verify`.
- **Commit:** `feat(web): add REST controllers, DTOs and global exception handler`

### Fase 7 — Integración end-to-end
- [ ] Test `@SpringBootTest(webEnvironment = RANDOM_PORT)` con WebTestClient: crear Zapato → agregar Cuero 2, Suela 1, Cordones 1 → calcular 100 → esperar 200/100/100. `/operators` apuntando a MockWebServer vía `@DynamicPropertySource`.
- [ ] Prueba manual con `./mvnw spring-boot:run` + curl de los 4 endpoints (incluida la API real de jsonplaceholder).
- [ ] (Opcional) `BlockHound` en tests para garantizar ausencia de bloqueos.
- **Verificación:** `./mvnw clean verify` + curls exitosos.
- **Commit:** `test: add end-to-end integration tests`

### Fase 8 — README y entrega
- [ ] `README.md`: descripción y diagrama de la arquitectura hexagonal, requisitos (Java 21, Maven Wrapper), comandos (`./mvnw spring-boot:run`, `./mvnw test`), tabla de endpoints con ejemplos **curl**, códigos de error, decisiones de diseño (records, sealed exceptions, servicios sin Spring, BOM con constraint único, timeout/errores WebClient, por qué Reactor en puertos).
- [ ] (Opcional) Swagger UI en `/swagger-ui.html`, colección `requests.http`.
- [ ] Revisión final del checklist (sección 8).
- [ ] Publicar repo público en GitHub con historial de commits.
- **Verificación:** `./mvnw clean verify` desde un clon limpio.
- **Commit:** `docs: add README with architecture, endpoints and design decisions`

## 8. Checklist final de aceptación (criterios de evaluación)

| Criterio (peso) | Verificación |
|---|---|
| Arquitectura hexagonal (25%) | `domain` sin imports de Spring/R2DBC/Jackson; puertos in/out; adapters en `infrastructure` |
| Pipeline 100% reactivo (25%) | Sin JPA/JDBC/MVC en `dependency:tree`; `grep -r "block()" src/main` vacío; todo retorna Mono/Flux |
| WebClient + API externa (20%) | Bean configurado, `bodyToFlux`, `onErrorMap/onErrorResume`, `map` DTO→dominio→response, timeout configurable |
| Entidades y endpoints (15%) | 4 endpoints exactos, records DTO, 201/200/400/404/409/502/504 |
| Test reactivo (10%) | StepVerifier en servicios y cliente externo; WebTestClient en controllers |
| README (5%) | Instrucciones, curl de todos los endpoints, decisiones de diseño |

**El proyecto se considera terminado cuando:** `./mvnw clean verify` compila sin errores ni warnings relevantes, todos los tests pasan, los 4 endpoints responden según la especificación y cada ítem del checklist está cumplido.

## 9. Comandos útiles

```bash
./mvnw clean verify          # compilar + tests (verificación de cada fase)
./mvnw spring-boot:run       # ejecutar (puerto 8080)
./mvnw dependency:tree       # verificar ausencia de dependencias bloqueantes
```

```bash
curl -X POST localhost:8080/products -H "Content-Type: application/json" -d '{"name":"Zapato"}'
curl -X POST localhost:8080/products/1/materials -H "Content-Type: application/json" -d '{"material":"Cuero","quantity":2}'
curl "localhost:8080/production/calculate?productId=1&quantity=100"
curl localhost:8080/operators
```

## 10. Convenciones de código

- Nombres en inglés en el código; README puede ir en español.
- Records para DTOs y modelos; sin Lombok.
- Inyección por constructor; clases `final` donde aplique.
- Un mapper por frontera (web ↔ dominio, entidad ↔ dominio, externo ↔ dominio).
- Commits con Conventional Commits, uno por fase como mínimo.
