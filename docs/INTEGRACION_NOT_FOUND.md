# Guía de integración para Not Found (Web 2)

Click + define todo lo de este documento porque no hubo comunicación con Not Found
(bitácora D-13 y D-27). Es la interfaz congelada: si algo no sirve, avisen y se ajusta.

## 1. Qué hay y qué falta
- **Listo y probado:** `asignador-service` (puerto 8080) con `POST /simular`.
- **Falta de su lado:** imágenes de `memoria-service`, gateway y frontend; el `realm` de Keycloak
  en `keycloak/`; el resource server (401/403) en el servicio. En `deploy/docker-compose.yml`
  sus servicios están declarados con perfil `web2` y límites de CPU/memoria como propuesta:
  reemplacen `image:` por las suyas y levanten con `--profile web2`.

## 2. Contrato HTTP: `POST /simular`
Entrada (bloques y operaciones van en el cuerpo; el servicio no guarda nada):

    { "bloques":[{"inicio":0,"tamano":100}, ...],
      "operaciones":[{"tipo":"ASIGNAR","proceso":1,"tamano":212},
                     {"tipo":"LIBERAR","proceso":1}],
      "estrategias":["PRIMERO_QUE_QUEPA","MEJOR_QUE_QUEPA","PEOR_QUE_QUEPA"] }

- `tipo`: `ASIGNAR` o `LIBERAR` (en liberar `tamano` se ignora). Tamaños en KB, enteros.
- El mapa inicial debe ser contiguo (cada bloque empieza donde termina el anterior).
- Se responde **200** (no 201: calcular no crea un recurso, D-19).

Salida, una entrada por estrategia pedida:

    { "motor":"native",
      "resultados":[ { "estrategia":"PRIMERO_QUE_QUEPA",
          "atendidas":2, "rechazadas":2,
          "libreTotal":776, "huecoMayor":300,
          "fragmentacionExternaKb":476, "fragmentacionExterna":0.61...,
          "fragmentacionInterna":0,
          "mapa":[ {"inicio":0,"tamano":100,"libre":true,"proceso":null},
                   {"inicio":100,"tamano":212,"libre":false,"proceso":1}, ... ],
          "decisiones":[ {"operacion":..,"tipo":"ASIGNAR","proceso":..,"solicitud":..,
                          "aceptada":true,"causaRechazo":null,"asignadoEn":100}, ... ] } ] }

- `fragmentacionExterna` es el cociente `1 − huecoMayor/libreTotal` (como el codelab);
  `fragmentacionExternaKb` es `libreTotal − huecoMayor`, el valor entero exacto (D-06).
- `causaRechazo`: `null`, `SIN_MEMORIA` o `FRAGMENTACION_EXTERNA` (RN-06).
- `asignadoEn: null` en una asignación aceptada significa que el proceso ya no está en el mapa
  final; **no es un error** (D-19). Una solicitud rechazada trae `aceptada:false`.
- `huecoMayor` es el del estado final, no el del instante del rechazo (D-11).

## 3. Errores
| HTTP | Cuándo | Cuerpo |
|---|---|---|
| 400 | JSON inválido, estrategia o tipo desconocido, mapa no contiguo, operación inválida | `{"mensaje":...}` o `{"codigo":..,"mensaje":..}` |
| 422 | liberar un proceso que no está (−4) o asignar uno que ya está (−5) | `{"codigo":-4,"mensaje":...}` |
| 500 | error interno de la biblioteca | `{"codigo":..,"mensaje":..}` |

Con el motor nativo y sin `libmemoria.so` el servicio **no arranca** (D-18).

## 4. Cómo conectarlo con su API
Su `POST /api/simulaciones` (escenarioId + estrategias) debe: leer del escenario los bloques y
operaciones, llamar a `POST http://asignador-service:8080/simular` con las estrategias elegidas,
guardar el resultado y devolverlo. El campo `mapa` es exactamente lo que dibuja la barra de Angular.

## 5. Autenticación (les corresponde a ustedes)
`asignador-service` hoy no valida tokens. Como los demás servicios, debe ser resource server:
sin token 401, firma alterada 401, rol equivocado 403, rol correcto 200. Recuerden que Keycloak
anida los roles en `realm_access.roles`; sin convertidor propio todo responde 403.

## 6. Variables de entorno
| Variable | Valor | Efecto |
|---|---|---|
| `ASIGNADOR_MOTOR` | `native` (por defecto) o `java` | `java` es el respaldo si falta la biblioteca |

## 7. Decisiones de diseño que afectan lo que muestran
- Interna: sobrante dentro del bloque solo si es ≤ 8 KB (D-04); si no, el bloque se parte.
- Desempate por menor dirección de inicio (D-05). Mapa inicial = particiones fijas; los huecos
  se funden solo al liberar (D-03). La biblioteca no compacta.
