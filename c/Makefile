# Makefile de libmemoria.so (Click +). Usa .RECIPEPREFIX para no depender de tabuladores.
.RECIPEPREFIX = >
CC      = gcc
CFLAGS  = -Wall -Wextra -O2 -fPIC

all: libmemoria.so

libmemoria.so: memoria.c memoria.h
> $(CC) $(CFLAGS) -shared -o $@ memoria.c

test_memoria: test_memoria.c libmemoria.so
> $(CC) -Wall -Wextra -O2 -o $@ test_memoria.c -L. -Wl,-rpath,'$$ORIGIN' -lmemoria

# Un solo comando: compila la biblioteca y ejecuta los casos de referencia
test: test_memoria
> ./test_memoria

clean:
> rm -f libmemoria.so test_memoria

.PHONY: all test clean
