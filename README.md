# Gestor de memoria · Click + (Arquitectura de Computadores)

Asignador de memoria en C (`libmemoria.so`): primer, mejor y peor ajuste con fragmentación
interna y externa, invocado desde Java con JNA por `asignador-service` (Spring Boot).

## Levantar todo desde cero (un solo comando)

Requisito: Docker con el plugin Compose. Nada más (sin gcc, sin JDK, sin Maven).

    git clone https://github.com/Migue-19/gestor-memoria-click.git
    cd gestor-memoria-click
    docker compose -f deploy/docker-compose.yml up -d --build

Probar (caso de referencia del enunciado, primer ajuste; debe dar `atendidas: 2`):

    curl -s -X POST localhost:8080/simular -H 'Content-Type: application/json' -d '{
      "bloques":[{"inicio":0,"tamano":100},{"inicio":100,"tamano":500},{"inicio":600,"tamano":200},{"inicio":800,"tamano":300}],
      "operaciones":[{"tipo":"ASIGNAR","proceso":1,"tamano":212},{"tipo":"ASIGNAR","proceso":2,"tamano":417},
                     {"tipo":"ASIGNAR","proceso":3,"tamano":112},{"tipo":"ASIGNAR","proceso":4,"tamano":426}],
      "estrategias":["PRIMERO_QUE_QUEPA","MEJOR_QUE_QUEPA","PEOR_QUE_QUEPA"]}'

Sistema completo con los servicios de Not Found (cuando existan sus imágenes):
`docker compose -f deploy/docker-compose.yml --profile web2 up -d --build`.
Ver `docs/INTEGRACION_NOT_FOUND.md`.

## Pruebas y mediciones

| Qué | Comando |
|---|---|
| RQ-02: suite de la biblioteca (30 corridas) | `make -C c clean test` |
| Memoria sin fugas | `cd c && valgrind --leak-check=full --error-exitcode=1 ./test_memoria` |
| Pruebas Java (JNA y servicio) | `cd asignador-service && mvn test` |
| RQ-01 en la máquina | `cd asignador-service && mvn test -Dtest=BancoRq01` |
| RQ-01 en contenedor (1 CPU, 512 MB) | `deploy/medir-rq01-contenedor.sh` |
| RQ-03: imagen y arranque | `deploy/medir-rq03.sh nativo 3` y `deploy/medir-rq03.sh java 3` |

`medir-rq03.sh` ejecuta `docker system prune -af`: borra todas las imágenes sin uso de la máquina.

## Estructura

- `c/` biblioteca (`memoria.c`, `memoria.h`), `Makefile` y suite `test_memoria.c`.
- `asignador-service/` interfaz JNA, línea base en Java, `@Service` y `POST /simular`.
- `deploy/` Dockerfile multietapa (nativo y solo Java), manifiestos y scripts de medición.
- `docs/` guía de integración y datos crudos de las mediciones.

## Cómo encuentra el contenedor la biblioteca

`libmemoria.so` queda en `/app/lib` y se arranca con `-Djna.library.path=/app/lib`.
Si el motor es `native` y falta la biblioteca, el servicio **no arranca** y muestra la ruta buscada.
Para operar sin ella: `ASIGNADOR_MOTOR=java`.
