package co.uceva.click.asignador;

import java.util.Arrays;

/**
 * Linea base en Java: el mismo algoritmo y la misma estructura (arreglos planos)
 * que la version en C, para que la comparacion de RQ-01 sea justa.
 * Tambien es el respaldo si libmemoria.so no esta disponible.
 */
public final class MotorJava implements Motor {

    private static final int UMBRAL_INTERNA = 8;

    @Override
    public Resultado simular(Escenario e, int estrategia) {
        int[] ini = e.bloqueInicio();
        int[] tb = e.bloqueTamano();
        int[] tipo = e.opTipo();
        int[] proc = e.opProceso();
        int[] tamOp = e.opTamano();
        int nBloques = e.nBloques();
        int nOps = e.nOps();

        // ---- validacion (mismo orden y codigos que la version en C) ----
        if (estrategia < 0 || estrategia > 2) throw new SimulacionException(-2);
        if (nBloques == 0) throw new SimulacionException(-3);
        for (int i = 0; i < nBloques; i++) {
            if (tb[i] <= 0) throw new SimulacionException(-3);
            if (i > 0 && ini[i] != ini[i - 1] + tb[i - 1]) throw new SimulacionException(-3);
        }
        for (int j = 0; j < nOps; j++) {
            if ((tipo[j] != 0 && tipo[j] != 1) || proc[j] < 0) throw new SimulacionException(-6);
            if (tipo[j] == 0 && tamOp[j] <= 0) throw new SimulacionException(-7);
        }

        int capacidad = nBloques + nOps;
        int[] ocu = new int[capacidad];
        int[] tam = new int[capacidad];
        int[] pedido = new int[capacidad];
        int[] causa = new int[nOps];
        int n = nBloques;
        for (int i = 0; i < nBloques; i++) {
            ocu[i] = -1;
            tam[i] = tb[i];
        }

        for (int j = 0; j < nOps; j++) {
            int p = proc[j];
            int k = -1;
            for (int i = 0; i < n; i++) {
                if (ocu[i] == p) { k = i; break; }
            }

            if (tipo[j] == 1) {                                  // liberar
                if (k < 0) throw new SimulacionException(-4);
                ocu[k] = -1;
                pedido[k] = 0;
                if (k + 1 < n && ocu[k + 1] == -1) {             // unir derecha
                    tam[k] += tam[k + 1];
                    n = quitar(ocu, tam, pedido, n, k + 1);
                }
                if (k > 0 && ocu[k - 1] == -1) {                 // unir izquierda
                    tam[k - 1] += tam[k];
                    n = quitar(ocu, tam, pedido, n, k);
                }
            } else {                                             // asignar
                if (k >= 0) throw new SimulacionException(-5);
                int t = tamOp[j];
                int h = elegirHueco(ocu, tam, n, t, estrategia);
                if (h < 0) {
                    long libre = 0;
                    for (int i = 0; i < n; i++) if (ocu[i] == -1) libre += tam[i];
                    causa[j] = libre < t ? 1 : 2;
                    continue;
                }
                int resto = tam[h] - t;
                if (resto >= UMBRAL_INTERNA) {
                    n = insertarLibre(ocu, tam, pedido, n, h + 1, resto);
                    tam[h] = t;
                }
                ocu[h] = p;
                pedido[h] = t;
            }
        }

        long libre = 0, mayor = 0, interna = 0;
        for (int i = 0; i < n; i++) {
            if (ocu[i] == -1) {
                libre += tam[i];
                if (tam[i] > mayor) mayor = tam[i];
            } else {
                interna += tam[i] - pedido[i];
            }
        }
        return new Resultado(Arrays.copyOf(ocu, n), Arrays.copyOf(tam, n), causa,
                (int) interna, (int) (libre - mayor), (int) mayor);
    }

    private static int elegirHueco(int[] ocu, int[] tam, int n, int t, int estrategia) {
        int elegido = -1;
        for (int i = 0; i < n; i++) {
            if (ocu[i] != -1 || tam[i] < t) continue;
            if (estrategia == 0) return i;
            if (elegido < 0
                    || (estrategia == 1 && tam[i] < tam[elegido])
                    || (estrategia == 2 && tam[i] > tam[elegido])) {
                elegido = i;
            }
        }
        return elegido;
    }

    private static int quitar(int[] ocu, int[] tam, int[] ped, int n, int idx) {
        int resto = n - idx - 1;
        if (resto > 0) {
            System.arraycopy(ocu, idx + 1, ocu, idx, resto);
            System.arraycopy(tam, idx + 1, tam, idx, resto);
            System.arraycopy(ped, idx + 1, ped, idx, resto);
        }
        return n - 1;
    }

    private static int insertarLibre(int[] ocu, int[] tam, int[] ped, int n, int idx, int tamano) {
        int resto = n - idx;
        if (resto > 0) {
            System.arraycopy(ocu, idx, ocu, idx + 1, resto);
            System.arraycopy(tam, idx, tam, idx + 1, resto);
            System.arraycopy(ped, idx, ped, idx + 1, resto);
        }
        ocu[idx] = -1;
        tam[idx] = tamano;
        ped[idx] = 0;
        return n + 1;
    }
}
