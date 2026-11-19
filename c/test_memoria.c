/*
 * test_memoria.c - Casos de referencia de libmemoria.so (RQ-02)
 * Los valores esperados fueron calculados a mano ANTES de implementar la
 * biblioteca (ver documento de contrato). No se copian de ninguna corrida.
 * 10 casos x 3 estrategias = 30 corridas, desviacion cero.
 */
#include <stdio.h>
#include <string.h>
#include "memoria.h"

#define MAXB 16
#define MAXO 8

typedef struct {
    int n;
    int ocu[MAXB];
    int tam[MAXB];
    int libre, hueco, ext, interna;
    int causas[MAXO];
} Esperado;

typedef struct {
    const char *nombre;
    int nb;  int ini[MAXB], tb[MAXB];
    int nops; int tipo[MAXO], proc[MAXO], tam[MAXO];
    Esperado esp[3];   /* 0 primero, 1 mejor, 2 peor */
} Caso;

static const char *NOMBRE_EST[3] = { "primero", "mejor", "peor" };

/* Resultados identicos para las tres estrategias */
#define E_C2 { 4, {4,-1,3,-1}, {350,50,200,200}, 250,200,50,0, {0,0,0,0,0,0} }
#define E_C3 { 4, {-1,2,-1,4}, {100,100,100,100}, 200,100,100,0, {0,0,0,0,0,0,2} }
#define E_C5 { 4, {5,-1,4,-1}, {250,50,100,100}, 150,100,50,0, {0,0,0,0,0,0,0,0} }
#define E_C7 { 4, {1,5,-1,3}, {300,150,50,100}, 50,50,0,0, {0,0,0,1,0,0} }
#define E_C4A { 3, {1,2,3}, {100,50,200}, 0,0,0,8, {0,0,0,1} }
#define E_C8A { 4, {-1,2,5,-1}, {80,120,150,50}, 130,80,50,0, {0,0,0,0,0,0,0,0} }

static const Caso CASOS[10] = {
 { "caso 1: ejemplo del codelab",
   4, {0,100,600,800}, {100,500,200,300},
   4, {0,0,0,0}, {1,2,3,4}, {212,417,112,426},
   { { 6, {-1,1,3,-1,-1,-1},   {100,212,112,176,200,300},     776,300,476,0, {0,2,0,2} },
     { 7, {-1,2,-1,3,-1,1,-1}, {100,417,83,112,88,212,88},    359,100,259,0, {0,0,0,1} },
     { 6, {-1,1,-1,-1,3,-1},   {100,212,288,200,112,188},     776,288,488,0, {0,2,0,2} } } },

 { "caso 2: union de dos huecos al liberar",
   4, {0,200,400,600}, {200,200,200,200},
   6, {0,0,0,1,1,0}, {1,2,3,1,2,4}, {200,200,200,0,0,350},
   { E_C2, E_C2, E_C2 } },

 { "caso 3: rechazo por fragmentacion externa",
   4, {0,100,200,300}, {100,100,100,100},
   7, {0,0,0,0,1,1,0}, {1,2,3,4,1,3,5}, {100,100,100,100,0,0,150},
   { E_C3, E_C3, E_C3 } },

 { "caso 4: interna por umbral y sin memoria",
   3, {0,100,150}, {100,50,200},
   4, {0,0,0,0}, {1,2,3,4}, {95,50,197,1},
   { E_C4A, E_C4A,
     { 6, {4,-1,-1,1,2,-1}, {1,99,50,95,50,55}, 204,99,105,0, {0,0,2,0} } } },

 { "caso 5: union de tres bloques a la vez",
   5, {0,100,200,300,400}, {100,100,100,100,100},
   8, {0,0,0,0,1,1,1,0}, {1,2,3,4,1,3,2,5}, {100,100,100,100,0,0,0,250},
   { E_C5, E_C5, E_C5 } },

 { "caso 6: interna con tres estrategias distintas",
   5, {0,60,160,220,320}, {60,100,60,100,30},
   6, {0,0,0,1,0,0}, {1,2,3,1,4,5}, {50,30,50,0,55,25},
   { { 8, {4,2,3,-1,5,-1,-1,-1}, {60,30,50,20,25,35,100,30}, 185,100,85,5, {0,0,0,0,0,0} },
     { 7, {4,5,-1,3,-1,-1,2},    {60,25,75,50,10,100,30},    185,100,85,5, {0,0,0,0,0,0} },
     { 8, {4,5,-1,-1,2,3,-1,-1}, {55,25,80,60,30,50,20,30},  190,80,110,0, {0,0,0,0,0,0} } } },

 { "caso 7: mapa lleno y reutilizacion tras liberar",
   3, {0,300,500}, {300,200,100},
   6, {0,0,0,0,1,0}, {1,2,3,4,2,5}, {300,200,100,10,0,150},
   { E_C7, E_C7, E_C7 } },

 { "caso 8: liberaciones en cadena, union triple y bordes",
   4, {0,80,200,300}, {80,120,100,100},
   8, {0,0,0,0,1,1,1,0}, {1,2,3,4,4,3,1,5}, {80,120,100,100,0,0,0,150},
   { E_C8A, E_C8A,
     { 2, {5,-1}, {150,250}, 250,250,0,0, {0,2,0,0,0,0,0,0} } } },

 { "caso 9: seis bloques, huecos adyacentes sin unir",
   6, {0,150,230,430,490,600}, {150,80,200,60,110,40},
   8, {0,0,0,1,0,1,0,0}, {1,2,3,1,4,3,5,6}, {70,60,120,0,100,0,90,170},
   { { 10, {-1,2,-1,5,6,-1,-1,4,-1,-1}, {70,60,20,90,170,20,60,100,10,40}, 220,70,150,0, {0,0,0,0,0,0,0,0} },
     { 9,  {-1,4,-1,6,-1,2,5,-1,-1},    {120,100,10,170,30,60,90,20,40},   220,120,100,0, {0,0,0,0,0,0,0,0} },
     { 8,  {2,-1,4,5,-1,-1,-1,-1},      {60,90,100,90,90,60,110,40},       390,110,280,0, {0,0,0,0,0,0,0,2} } } },

 { "caso 10: seis bloques, liberaciones y mejor ajuste sin rechazos",
   6, {0,90,300,340,500,600}, {90,210,40,160,100,50},
   8, {0,0,0,1,0,0,1,0}, {1,2,3,1,4,5,3,6}, {85,150,38,0,100,150,0,120},
   { { 8, {-1,2,-1,-1,4,-1,-1,-1}, {90,150,60,40,100,60,100,50}, 400,100,300,0, {0,0,0,0,0,2,0,2} },
     { 7, {5,6,-1,2,-1,4,-1},      {150,120,70,150,10,100,50},   130,70,60,0,   {0,0,0,0,0,0,0,0} },
     { 8, {4,6,-1,-1,2,-1,-1,-1},  {100,120,80,40,150,10,100,50}, 280,100,180,0, {0,0,0,0,0,2,0,0} } } },
};

static void imprimir_mapa(const char *rotulo, int n, const int *ocu, const int *tam)
{
    printf("      %s: ", rotulo);
    for (int i = 0; i < n; i++) {
        if (ocu[i] == -1) printf("[libre %d] ", tam[i]);
        else              printf("[P%d %d] ", ocu[i], tam[i]);
    }
    printf("\n");
}

/* Ejecuta una corrida y compara. Devuelve 1 si coincide exactamente. */
static int correr(const Caso *c, int est, int detalle)
{
    const Esperado *e = &c->esp[est];
    int ocu[64], tam[64], causa[64];
    int n = 0, interna = 0, ext = 0, hueco = 0;

    int rc = simular(c->ini, c->tb, c->nb, c->tipo, c->proc, c->tam, c->nops,
                     est, ocu, tam, &n, causa, &interna, &ext, &hueco);

    int ok = (rc == 0 && n == e->n && interna == e->interna &&
              ext == e->ext && hueco == e->hueco && ext + hueco == e->libre);
    for (int i = 0; ok && i < n; i++)
        ok = (ocu[i] == e->ocu[i] && tam[i] == e->tam[i]);
    for (int j = 0; ok && j < c->nops; j++)
        ok = (causa[j] == e->causas[j]);

    printf("  [%s] %-8s %-56s esperado n/libre/hueco/ext/int=%d/%d/%d/%d/%d  obtenido=%d/%d/%d/%d/%d\n",
           ok ? "OK " : "FALLA", NOMBRE_EST[est], c->nombre,
           e->n, e->libre, e->hueco, e->ext, e->interna,
           n, ext + hueco, hueco, ext, interna);
    if (!ok && detalle) {
        printf("      rc=%d  esperado n=%d libre=%d hueco=%d ext=%d int=%d | obtenido n=%d libre=%d hueco=%d ext=%d int=%d\n",
               rc, e->n, e->libre, e->hueco, e->ext, e->interna, n, ext + hueco, hueco, ext, interna);
        imprimir_mapa("esperado", e->n, e->ocu, e->tam);
        imprimir_mapa("obtenido", n, ocu, tam);
        printf("      causas esperadas/obtenidas:");
        for (int j = 0; j < c->nops; j++) printf(" %d/%d", e->causas[j], causa[j]);
        printf("\n");
    }
    return ok;
}

/* Pruebas de codigos de error del contrato */
static int prueba_error(const char *nombre, int esperado, int obtenido)
{
    int ok = (esperado == obtenido);
    printf("  [%s] error %-44s esperado %d, obtenido %d\n", ok ? "OK " : "FALLA", nombre, esperado, obtenido);
    return ok;
}

static int pruebas_error(void)
{
    int ok = 0, total = 0;
    int ini[2] = {0, 100}, tb[2] = {100, 100};
    int tipo[2] = {0, 0}, proc[2] = {1, 2}, tam[2] = {50, 50};
    int ocu[8], tz[8], causa[8], n, in, ex, hu;

#define CORRE(ini_, tb_, nb_, tipo_, proc_, tam_, nops_, est_) \
    simular(ini_, tb_, nb_, tipo_, proc_, tam_, nops_, est_, ocu, tz, &n, causa, &in, &ex, &hu)

    total++; ok += prueba_error("estrategia invalida", MEM_E_ESTRATEGIA,
                                CORRE(ini, tb, 2, tipo, proc, tam, 2, 3));
    total++; ok += prueba_error("puntero nulo", MEM_E_PUNTERO,
                                CORRE((const int *)0, tb, 2, tipo, proc, tam, 2, 0));
    { int ini2[2] = {0, 150};
      total++; ok += prueba_error("mapa no contiguo", MEM_E_MAPA,
                                  CORRE(ini2, tb, 2, tipo, proc, tam, 2, 0)); }
    { int tb2[2] = {100, 0};
      total++; ok += prueba_error("bloque de tamano 0", MEM_E_MAPA,
                                  CORRE(ini, tb2, 2, tipo, proc, tam, 2, 0)); }
    { int t1[1] = {1}, p1[1] = {7}, z1[1] = {0};
      total++; ok += prueba_error("liberar proceso inexistente", MEM_E_LIBERAR,
                                  CORRE(ini, tb, 2, t1, p1, z1, 1, 0)); }
    { int p2[2] = {1, 1};
      total++; ok += prueba_error("asignar proceso duplicado", MEM_E_DUPLICADO,
                                  CORRE(ini, tb, 2, tipo, p2, tam, 2, 0)); }
    { int t2[2] = {2, 0};
      total++; ok += prueba_error("tipo de operacion invalido", MEM_E_OPERACION,
                                  CORRE(ini, tb, 2, t2, proc, tam, 2, 0)); }
    { int p3[2] = {-3, 2};
      total++; ok += prueba_error("id de proceso negativo", MEM_E_OPERACION,
                                  CORRE(ini, tb, 2, tipo, p3, tam, 2, 0)); }
    { int z2[2] = {50, 0};
      total++; ok += prueba_error("tamano pedido 0", MEM_E_TAMANO,
                                  CORRE(ini, tb, 2, tipo, proc, z2, 2, 0)); }
#undef CORRE
    return ok == total ? 1 : 0;
}

int main(void)
{
    int ok = 0, total = 0;

    printf("Casos de referencia (10 casos x 3 estrategias = 30 corridas)\n");
    for (int c = 0; c < 10; c++)
        for (int e = 0; e < 3; e++) {
            total++;
            ok += correr(&CASOS[c], e, 1);
        }

    printf("\nCodigos de error del contrato\n");
    int errores_ok = pruebas_error();

    /* Determinismo: el mismo escenario dos veces da exactamente lo mismo */
    printf("\nDeterminismo\n");
    int det_ok = 1;
    for (int e = 0; e < 3; e++) {
        const Caso *c = &CASOS[8];
        int o1[64], t1[64], c1[64], o2[64], t2[64], c2[64];
        int n1, i1, x1, h1, n2, i2, x2, h2;
        simular(c->ini, c->tb, c->nb, c->tipo, c->proc, c->tam, c->nops, e, o1, t1, &n1, c1, &i1, &x1, &h1);
        simular(c->ini, c->tb, c->nb, c->tipo, c->proc, c->tam, c->nops, e, o2, t2, &n2, c2, &i2, &x2, &h2);
        int igual = (n1 == n2 && i1 == i2 && x1 == x2 && h1 == h2 &&
                     !memcmp(o1, o2, (size_t)n1 * sizeof(int)) &&
                     !memcmp(t1, t2, (size_t)n1 * sizeof(int)) &&
                     !memcmp(c1, c2, (size_t)c->nops * sizeof(int)));
        printf("  [%s] caso 9 repetido, %s\n", igual ? "OK " : "FALLA", NOMBRE_EST[e]);
        det_ok &= igual;
    }

    printf("\nResumen: %d de %d corridas de referencia correctas, errores %s, determinismo %s\n",
           ok, total, errores_ok ? "OK" : "FALLA", det_ok ? "OK" : "FALLA");
    return (ok == total && errores_ok && det_ok) ? 0 : 1;
}
