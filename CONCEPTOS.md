# Guía de conceptos y decisiones — Prueba Técnica QUANTA MRP (Módulo BOM)

> Documento de estudio personal para entender **qué se pide**, **por qué** y **cómo defenderlo** en la entrevista.

## Índice

1. [El negocio: ¿qué es un BOM en un MRP?](#1-el-negocio-qué-es-un-bom-en-un-mrp)
2. [Programación reactiva](#2-programación-reactiva)
3. [Arquitectura hexagonal](#3-arquitectura-hexagonal)
4. [WebClient](#4-webclient)
5. [Diseño de endpoints y HTTP](#5-diseño-de-endpoints-y-http)
6. [Java 21](#6-java-21)
7. [Tests reactivos](#7-tests-reactivos)
8. [Decisiones tomadas](#8-decisiones-tomadas)
9. [Preguntas probables de entrevista](#9-preguntas-probables-de-entrevista)

---

## 1. El negocio: ¿qué es un BOM en un MRP?

- **MRP (Material Requirements Planning):** un sistema que planifica qué materiales hacen falta, en qué cantidad y cuándo, para producir lo que se va a fabricar.
- **BOM (Bill of Materials):** la "receta" de un producto. Indica qué insumos se necesitan para fabricar **1 unidad**.

```
Zapato (1 u.) = Cuero ×2 + Suela ×1 + Cordones ×1
```

- **Orden de producción:** "quiero fabricar 100 zapatos". El sistema multiplica la receta por 100:

```
requerido = cantidad_en_BOM × cantidad_a_producir
Cuero = 2 × 100 = 200 | Suela = 1 × 100 = 100 | Cordones = 1 × 100 = 100
```

| Endpoint | Propósito |
|---|---|
| `POST /products` | Crear el producto (Zapato) |
| `POST /products/{id}/materials` | Definir la receta (BOM) |
| `GET /production/calculate` | Calcular materiales para N unidades |
| `GET /operators` | Demostrar consumo reactivo de una API externa |

---

## 2. Programación reactiva

> Peso en la evaluación: **25 %** (pipeline reactivo) + **20 %** (WebClient).

### El problema que resuelve

En Spring MVC tradicional, **cada petición ocupa un hilo** mientras espera la base de datos o una API externa. Si llegan 1000 peticiones, necesitas cerca de 1000 hilos, y casi todos están sin hacer nada, solo esperando.

### La solución reactiva

Unos **pocos hilos** atienden muchas peticiones. En vez de quedarse esperando, el hilo dice "avísame cuando llegue el dato" y mientras tanto atiende otra cosa. Esto se llama **I/O no bloqueante**.

### Mono y Flux (Project Reactor)

Son "promesas" de datos que llegarán en el futuro:

| Tipo | Significado | Ejemplo en la prueba |
|---|---|---|
| `Mono<T>` | 0 o 1 elemento | Crear un producto, el cálculo de producción |
| `Flux<T>` | 0 a N elementos | Lista de operarios, materiales del BOM |

**Lo importante:** nada se ejecuta hasta que alguien se suscribe. En WebFlux, el que se suscribe es el propio framework cuando responde al cliente. Tú solo **describes el pipeline**:

```java
productRepository.findById(id)                                  // Mono<Product>
    .switchIfEmpty(Mono.error(new ProductNotFoundException(id))) // si no existe → error
    .flatMap(product -> bomRepository.findByProductId(id)        // Flux<BomItem>
        .map(item -> item.requiredFor(quantity))                 // transforma cada uno
        .collectList()                                           // Flux → Mono<List>
        .map(list -> new ProductionCalculation(product.name(), quantity, list)));
```

### Operadores clave

| Operador | Para qué sirve |
|---|---|
| `map` | Transforma el valor de forma síncrona (A → B) |
| `flatMap` | Transforma el valor en **otro Mono/Flux** (A → Mono<B>); se usa cuando la transformación implica otra operación asíncrona, como consultar la BD |
| `switchIfEmpty` | Qué hacer si no llegó nada (por ejemplo, devolver 404) |
| `collectList` | Convierte un `Flux<T>` en un `Mono<List<T>>` |
| `onErrorMap` | Convierte un error en otro (un error técnico en un error de dominio) |
| `onErrorResume` | Recupera el flujo ante un error (por ejemplo, devolver un valor por defecto) |
| `timeout` | Si no llega respuesta en X tiempo, emite un error |

### ¿Por qué "no bloquear" es tan importante?

Como hay pocos hilos, si uno se bloquea **se congela una parte grande del servidor**. Por eso la prueba prohíbe:

| Prohibido | Motivo | Se usa en su lugar |
|---|---|---|
| JPA / Hibernate / JDBC | Son bloqueantes por diseño | **R2DBC** (equivalente reactivo para SQL) |
| `.block()` en código productivo | Detiene el hilo esperando el resultado | Encadenar operadores |
| Spring MVC (`spring-boot-starter-web`) | Usa un hilo por petición | **WebFlux** (sobre Netty) |
| `RestTemplate` | Cliente HTTP bloqueante | **WebClient** |

> **Sobre Virtual Threads:** la prueba los menciona "si aplica". En un stack 100 % reactivo **no aplican**, porque resuelven el mismo problema por otro camino (hilos baratos en vez de no bloquear). Mencionarlo en el README demuestra criterio.

---

## 3. Arquitectura hexagonal

> Peso en la evaluación: **25 %**.

### La idea

El **dominio (la lógica de negocio) está en el centro** y no sabe nada del mundo exterior: no sabe si los datos vienen de H2, Postgres o un archivo, ni si lo llaman por REST o por Kafka.

```
            [Controller REST]                ← adaptador de ENTRADA
                   │
                   ▼
      ┌──── Puerto IN (interfaz) ────┐
      │                              │
      │   APLICACIÓN (casos de uso)  │
      │   DOMINIO (reglas, modelos)  │
      │                              │
      └──── Puerto OUT (interfaz) ───┘
                   │
         ┌─────────┴─────────┐
         ▼                   ▼
  [R2DBC Adapter]    [WebClient Adapter]     ← adaptadores de SALIDA
```

### Conceptos

| Concepto | Qué es | Ejemplo |
|---|---|---|
| **Puerto de entrada (in)** | Interfaz que dice **qué puede hacer** el sistema | `CalculateProductionUseCase` |
| **Puerto de salida (out)** | Interfaz que dice **qué necesita** el dominio del exterior | `ProductRepositoryPort`, `OperatorClientPort` |
| **Adaptador** | Implementación concreta con tecnología real | `ProductController`, `ProductPersistenceAdapter`, `JsonPlaceholderOperatorClient` |
| **Regla de dependencias** | Las flechas apuntan **hacia adentro** | La infraestructura conoce al dominio; el dominio **nunca** conoce la infraestructura |

### Beneficio que debes saber defender

Si mañana cambian H2 por PostgreSQL, o jsonplaceholder por otra API, **solo cambias un adaptador** y el dominio no se toca. Además, el dominio se puede testear sin Spring.

### Decisiones relacionadas

- **Servicios sin `@Service`:** se registran como beans en `BeanConfiguration`, así la capa de aplicación queda libre de Spring. Es la versión "purista".
- **Tres modelos distintos para la misma cosa:**

  | Capa | Clase | Responsabilidad |
  |---|---|---|
  | Base de datos | `ProductEntity` | Mapeo a la tabla |
  | Dominio | `Product` | Reglas de negocio |
  | API | `ProductResponse` | Contrato con el cliente |

  Parece repetitivo, pero cada capa evoluciona sin romper las demás. Por eso hay **mappers en cada frontera**.

- **Reactor en el dominio:** es la única excepción "impura". Los puertos devuelven `Mono`/`Flux` porque la prueba exige que toda la cadena sea reactiva. Reactor es una librería, no un framework, y conviene justificarlo en el README.

---

## 4. WebClient

> Peso en la evaluación: **20 %**. Es el diferenciador para nivel Semi-Senior.

Es el cliente HTTP **reactivo** de Spring, el reemplazo de `RestTemplate` (que es bloqueante).

| Lo que evalúan | Qué significa |
|---|---|
| **Bean de Spring** | Se crea una sola vez y se reutiliza. Crearlo en cada llamada desperdicia el pool de conexiones. |
| **`bodyToFlux()`** | La API devuelve un array JSON, y se convierte en un `Flux<ExternalUserDto>`. |
| **Manejo de errores** | Si jsonplaceholder devuelve 500 o se cae, no le muestras al usuario un stacktrace: devuelves un error controlado (502). |
| **DTO externo → modelo propio** | La API trae muchos campos (address, company…). Mapeas solo los relevantes; si la API externa cambia, solo se toca el mapper. |
| **Timeout (bono)** | Si la API tarda, no esperas para siempre: a los X segundos devuelves 504. |

Esquema del adaptador:

```java
return webClient.get()
        .uri("/users")
        .retrieve()
        .onStatus(HttpStatusCode::isError, resp -> Mono.error(new ExternalServiceException(...)))
        .bodyToFlux(ExternalUserDto.class)
        .map(this::toDomain)
        .timeout(properties.readTimeout())
        .onErrorMap(TimeoutException.class, ex -> new ExternalServiceTimeoutException(...));
```

### 502 vs 504

| Código | Nombre | Cuándo |
|---|---|---|
| **502** | Bad Gateway | El servicio externo respondió mal (error 5xx, respuesta inválida) |
| **504** | Gateway Timeout | El servicio externo no respondió a tiempo |

---

## 5. Diseño de endpoints y HTTP

> Peso en la evaluación: **15 %**.

| Código | Cuándo |
|---|---|
| **201 Created** | POST que creó un recurso; lo ideal es incluir el header `Location: /products/1` |
| **200 OK** | GET exitoso |
| **400 Bad Request** | El cliente envía datos inválidos (nombre vacío, cantidad ≤ 0) |
| **404 Not Found** | El producto no existe |
| **409 Conflict** | Producto o material duplicado (decisión propia: si agregas "Cuero" dos veces al mismo BOM, el cálculo sería ambiguo) |
| **502 / 504** | Errores de la API externa (ver sección 4) |

**GlobalExceptionHandler:** un único lugar que traduce excepciones a respuestas HTTP con un formato uniforme. Así los controllers quedan limpios.

```json
{
  "timestamp": "2026-10-07T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product 99 not found",
  "path": "/production/calculate"
}
```

---

## 6. Java 21

| Característica | Uso en el proyecto |
|---|---|
| **Records** | Clases inmutables de datos en una línea: `record Product(Long id, String name) {}`. Son ideales para DTOs y modelos. |
| **Constructor compacto** | Permite validar dentro del record (por ejemplo, `quantity > 0`). Así **es imposible crear un objeto de dominio inválido**. |
| **Sealed classes** | Limitan qué clases pueden heredar: `sealed DomainException permits ProductNotFoundException, ...` hace que la jerarquía de errores quede cerrada y controlada. |
| **`var`** | Solo donde mejore la legibilidad. |

```java
public record BomItem(Long id, Long productId, String material, int quantity) {
    public BomItem {
        if (quantity <= 0) throw new InvalidQuantityException(quantity);
    }
    public MaterialRequirement requiredFor(int units) {
        return new MaterialRequirement(material, Math.multiplyExact((long) quantity, units));
    }
}
```

---

## 7. Tests reactivos

> Peso en la evaluación: **10 %**.

No puedes hacer un `assertEquals` directo sobre un `Mono`, porque el valor aún no existe. **StepVerifier** se suscribe y verifica paso a paso:

```java
StepVerifier.create(service.calculate(1L, 100))
    .assertNext(r -> assertThat(r.materials()).hasSize(3))
    .verifyComplete();

StepVerifier.create(service.calculate(99L, 100))
    .expectError(ProductNotFoundException.class)
    .verify();
```

| Herramienta | Para qué |
|---|---|
| **StepVerifier** | Verificar `Mono`/`Flux` paso a paso |
| **Mockito** | Simular los puertos para probar los servicios aislados |
| **MockWebServer** | Servidor HTTP falso para probar el WebClient sin internet, incluidos los casos de error y timeout |
| **WebTestClient** | Probar los controllers y los códigos HTTP |

---

## 8. Decisiones tomadas

Todas se pueden cambiar.

| Decisión | Por qué | Alternativa |
|---|---|---|
| Maven | Es el más común; `mvn spring-boot:run` aparece en el enunciado | Gradle |
| Controllers anotados (`@RestController`) | Son más legibles y conocidos | RouterFunctions (funcional, también válido en WebFlux) |
| Servicios sin anotaciones Spring | Hexagonal más estricto | Usar `@Service` (más simple, menos purista) |
| Material único por producto (409) | Evita cálculos ambiguos | Sumar cantidades si se repite |
| `long` en `required` | Evita overflow (cantidad grande × cantidad) | `int` |
| Producto sin BOM → lista vacía | Un producto sin receta es válido; simplemente no requiere materiales | Devolver 422 |

---

## 9. Preguntas probables de entrevista

<details>
<summary><b>1. ¿Diferencia entre <code>map</code> y <code>flatMap</code>?</b></summary>

`map` aplica una función síncrona que devuelve un valor (A → B). `flatMap` aplica una función que devuelve otro publisher (A → Mono<B>) y "aplana" el resultado. Se usa `flatMap` cuando la transformación es asíncrona, como una consulta a BD o una llamada HTTP.
</details>

<details>
<summary><b>2. ¿Qué pasa si llamas <code>.block()</code> en un controller WebFlux?</b></summary>

Bloqueas uno de los pocos hilos del event loop de Netty. Reactor lanza `IllegalStateException` ("block() is not supported in thread reactor-http-nio") y, aunque no lo hiciera, degradaría el rendimiento de todo el servidor.
</details>

<details>
<summary><b>3. ¿Por qué R2DBC y no JPA?</b></summary>

JPA/JDBC son bloqueantes: cada consulta retiene el hilo hasta que la BD responde. R2DBC es un driver no bloqueante que devuelve `Mono`/`Flux`, coherente con el pipeline reactivo. A cambio, no tiene lazy loading ni relaciones automáticas (no es un ORM completo).
</details>

<details>
<summary><b>4. ¿Por qué el dominio no tiene anotaciones de Spring?</b></summary>

Para que la lógica de negocio sea independiente del framework: se puede testear sin levantar Spring, y cambiar la infraestructura (BD, API externa, framework web) no obliga a tocar las reglas de negocio.
</details>

<details>
<summary><b>5. ¿Qué haces si la API externa no responde?</b></summary>

Se configura un timeout (a nivel de Netty y con el operador `timeout`), se convierte el error con `onErrorMap` en una excepción de dominio y el GlobalExceptionHandler responde 504. Opcionalmente, `retryWhen` con backoff para errores transitorios.
</details>

<details>
<summary><b>6. ¿Cuándo usar <code>onErrorResume</code> en vez de <code>onErrorMap</code>?</b></summary>

`onErrorMap` cuando quieres que el error siga propagándose pero con otro tipo (más significativo). `onErrorResume` cuando quieres recuperarte y continuar con un valor alternativo (fallback, caché, lista vacía).
</details>
