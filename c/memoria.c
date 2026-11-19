/*
 * memoria.c - Simulador de asignacion de memoria (primer, mejor y peor ajuste)
 * Compilar: gcc -Wall -Wextra -O2 -shared -fPIC -o libmemoria.so memoria.c
 */
#include "memoria.h"

#include <stdlib.h>
#include <string.h>

/* Quita el bloque idx del mapa (desplaza a la izquierda). */
static void quitar(int *ocu, int *tam, int *ped, int *n, int idx)
{
    int resto = *n - idx - 1;
    if (resto > 0) {
        memmove(&ocu[idx], &ocu[idx + 1], (size_t)resto * sizeof(int));
        memmove(&tam[idx], &tam[idx + 1], (size_t)resto * sizeof(int));
        memmove(&ped[idx], &ped[idx + 1], (size_t)resto * sizeof(int));
    }
    (*n)--;
}

/* Inserta un bloque libre en la posicion idx (desplaza a la derecha). */
static void insertar_libre(int *ocu, int *tam, int *ped, int *n, int idx, int tamano)
{
    int resto = *n - idx;
    if (resto > 0) {
        memmove(&ocu[idx + 1], &ocu[idx], (size_t)resto * sizeof(int));
        memmove(&tam[idx + 1], &tam[idx], (size_t)resto * sizeof(int));
        memmove(&ped[idx + 1], &ped[idx], (size_t)resto * sizeof(int));
    }
    ocu[idx] = -1;
    tam[idx] = tamano;
    ped[idx] = 0;
    (*n)++;
}

/*
 * Elige el hueco segun la estrategia. Se recorre en orden de direccion y los
 * empates se resuelven con comparacion estricta: gana el de menor inicio.
 * Devuelve el indice del bloque o -1 si ninguno sirve.
 */
static int elegir_hueco(const int *ocu, const int *tam, int n, int t, int estrategia)
{
    int elegido = -1;
    for (int i = 0; i < n; i++) {
        if (ocu[i] != -1 || tam[i] < t)
            continue;
        if (estrategia == 0)
            return i;
        if (elegido < 0 ||
            (estrategia == 1 && tam[i] < tam[elegido]) ||
            (estrategia == 2 && tam[i] > tam[elegido]))
            elegido = i;
    }
    return elegido;
}

int simular(
    const int *bloque_inicio, const int *bloque_tamano, int n_bloques,
    const int *op_tipo, const int *op_proceso, const int *op_tamano, int n_ops,
    int estrategia,
    int *estado_ocupante, int *estado_tamano, int *n_bloques_final,
    int *causa_rechazo,
    int *frag_interna, int *frag_externa, int *hueco_mayor)
{
    /* ---- validacion de entradas ---- */
    if (!bloque_inicio || !bloque_tamano || !estado_ocupante || !estado_tamano ||
        !n_bloques_final || !causa_rechazo || !frag_interna || !frag_externa ||
        !hueco_mayor)
        return MEM_E_PUNTERO;
    if (n_bloques < 0 || n_ops < 0)
        return MEM_E_PUNTERO;
    if (n_ops > 0 && (!op_tipo || !op_proceso || !op_tamano))
        return MEM_E_PUNTERO;
    if (estrategia < 0 || estrategia > 2)
        return MEM_E_ESTRATEGIA;

    if (n_bloques == 0)
        return MEM_E_MAPA;
    for (int i = 0; i < n_bloques; i++) {
        if (bloque_tamano[i] <= 0)
            return MEM_E_MAPA;
        if (i > 0 && bloque_inicio[i] != bloque_inicio[i - 1] + bloque_tamano[i - 1])
            return MEM_E_MAPA;
    }
    for (int j = 0; j < n_ops; j++) {
        if ((op_tipo[j] != 0 && op_tipo[j] != 1) || op_proceso[j] < 0)
            return MEM_E_OPERACION;
        if (op_tipo[j] == 0 && op_tamano[j] <= 0)
            return MEM_E_TAMANO;
    }

    /* ---- trabajo interno: KB pedidos por bloque (para la interna) ---- */
    int capacidad = n_bloques + n_ops;
    int *pedido = (int *)calloc((size_t)capacidad, sizeof(int));
    if (!pedido)
        return MEM_E_MEMORIA;

    int n = n_bloques;
    for (int i = 0; i < n_bloques; i++) {
        estado_ocupante[i] = -1;
        estado_tamano[i] = bloque_tamano[i];
    }
    for (int j = 0; j < n_ops; j++)
        causa_rechazo[j] = CAUSA_OK;

    int rc = MEM_OK;

    /* ---- simulacion ---- */
    for (int j = 0; j < n_ops && rc == MEM_OK; j++) {
        int p = op_proceso[j];
        int k = -1;
        for (int i = 0; i < n; i++) {
            if (estado_ocupante[i] == p) {
                k = i;
                break;
            }
        }

        if (op_tipo[j] == 1) {                       /* liberar */
            if (k < 0) {
                rc = MEM_E_LIBERAR;
                break;
            }
            estado_ocupante[k] = -1;
            pedido[k] = 0;
            if (k + 1 < n && estado_ocupante[k + 1] == -1) {   /* unir derecha */
                estado_tamano[k] += estado_tamano[k + 1];
                quitar(estado_ocupante, estado_tamano, pedido, &n, k + 1);
            }
            if (k > 0 && estado_ocupante[k - 1] == -1) {       /* unir izquierda */
                estado_tamano[k - 1] += estado_tamano[k];
                quitar(estado_ocupante, estado_tamano, pedido, &n, k);
            }
        } else {                                     /* asignar */
            if (k >= 0) {
                rc = MEM_E_DUPLICADO;
                break;
            }
            int t = op_tamano[j];
            int h = elegir_hueco(estado_ocupante, estado_tamano, n, t, estrategia);
            if (h < 0) {
                long libre = 0;
                for (int i = 0; i < n; i++)
                    if (estado_ocupante[i] == -1)
                        libre += estado_tamano[i];
                causa_rechazo[j] = (libre < t) ? CAUSA_SIN_MEMORIA : CAUSA_FRAGMENTACION;
                continue;
            }
            int resto = estado_tamano[h] - t;
            if (resto >= UMBRAL_INTERNA) {
                insertar_libre(estado_ocupante, estado_tamano, pedido, &n, h + 1, resto);
                estado_tamano[h] = t;
            }
            estado_ocupante[h] = p;
            pedido[h] = t;
        }
    }

    if (rc == MEM_OK) {
        long libre = 0, mayor = 0, interna = 0;
        for (int i = 0; i < n; i++) {
            if (estado_ocupante[i] == -1) {
                libre += estado_tamano[i];
                if (estado_tamano[i] > mayor)
                    mayor = estado_tamano[i];
            } else {
                interna += estado_tamano[i] - pedido[i];
            }
        }
        *n_bloques_final = n;
        *hueco_mayor = (int)mayor;
        *frag_externa = (int)(libre - mayor);
        *frag_interna = (int)interna;
    }

    free(pedido);
    return rc;
}
