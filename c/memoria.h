/*
 * memoria.h - Biblioteca libmemoria.so (Click +, Arquitectura de Computadores)
 *
 * Una llamada por corrida, arreglos planos de enteros (JNA los mapea directo).
 * Java preasigna TODOS los arreglos de salida; C no devuelve punteros.
 *
 * Tamano de los arreglos de salida que Java debe reservar:
 *   estado_ocupante, estado_tamano : n_bloques + n_ops
 *   causa_rechazo                  : n_ops
 *
 * estrategia: 0 = primer ajuste, 1 = mejor ajuste, 2 = peor ajuste
 */
#ifndef MEMORIA_H
#define MEMORIA_H

/* Codigos de retorno de simular() */
#define MEM_OK            0
#define MEM_E_PUNTERO    -1  /* puntero nulo o cantidad negativa            */
#define MEM_E_ESTRATEGIA -2  /* estrategia fuera de 0..2                    */
#define MEM_E_MAPA       -3  /* mapa vacio, no contiguo o tamano <= 0       */
#define MEM_E_LIBERAR    -4  /* liberar un proceso que no esta en el mapa   */
#define MEM_E_DUPLICADO  -5  /* asignar un proceso que ya esta en el mapa   */
#define MEM_E_OPERACION  -6  /* tipo distinto de 0/1 o id de proceso < 0    */
#define MEM_E_TAMANO     -7  /* tamano pedido <= 0 en una asignacion        */
#define MEM_E_MEMORIA    -8  /* sin memoria para el trabajo interno de C    */

/* causa_rechazo[i]: 0 ok o liberacion, 1 sin memoria, 2 fragmentacion */
#define CAUSA_OK              0
#define CAUSA_SIN_MEMORIA     1
#define CAUSA_FRAGMENTACION   2

/* Si el remanente de partir un bloque es menor a este valor (KB), se
 * entrega el bloque completo y el resto cuenta como fragmentacion interna. */
#define UMBRAL_INTERNA 8

int simular(
    const int *bloque_inicio,   /* direccion de inicio de cada bloque        */
    const int *bloque_tamano,   /* tamano en KB de cada bloque               */
    int n_bloques,
    const int *op_tipo,         /* 0 = asignar, 1 = liberar                  */
    const int *op_proceso,      /* id del proceso (>= 0)                     */
    const int *op_tamano,       /* KB pedidos (ignorado si es liberar)       */
    int n_ops,
    int estrategia,
    /* salidas: arreglos que Java preasigna */
    int *estado_ocupante,       /* proceso en cada bloque, -1 si libre       */
    int *estado_tamano,         /* tamano final de cada bloque               */
    int *n_bloques_final,
    int *causa_rechazo,         /* por operacion: 0 ok, 1 sin memoria, 2 frag */
    int *frag_interna,
    int *frag_externa,          /* libre_total - hueco_mayor (KB)            */
    int *hueco_mayor);

#endif /* MEMORIA_H */
