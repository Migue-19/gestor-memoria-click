package co.uceva.click.asignador;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Banco de medicion de RQ-01 (desempeno): C por JNA contra la linea base en Java.
 *
 * Estimulo: 100.000 operaciones sobre un mapa de 500 bloques con MEJOR ajuste.
 * Secuencia generada con semilla fija (la misma para los dos lados).
 * Se hacen 8 corridas de cada lado, se descartan las 3 primeras (calentamiento del JIT)
 * y se reporta la mediana de las 5 restantes y la relacion Java / C.
 *
 * No se llama *Test a proposito: no corre con "mvn test". Se ejecuta con:
 *   mvn test -Dtest=BancoRq01
 */
class BancoRq01 {

    static final int N_BLOQUES = 500;
    static final int N_OPS = 100_000;
    static final int MEJOR_AJUSTE = 1;
    static final int DESCARTADAS = 3;
    static final int MEDIDAS = 5;
    static final long SEMILLA = 20261119L;

    /** Mapa de 500 bloques de 800 a 1200 KB; mezcla de asignaciones y liberaciones validas. */
    static Escenario generar() {
        Random r = new Random(SEMILLA);
        int[] tb = new int[N_BLOQUES];
        int[] ini = new int[N_BLOQUES];
        int dir = 0;
        for (int i = 0; i < N_BLOQUES; i++) {
            tb[i] = 800 + r.nextInt(401);
            ini[i] = dir;
            dir += tb[i];
        }
        int[] tipo = new int[N_OPS], proc = new int[N_OPS], tam = new int[N_OPS];
        List<Integer> vivos = new ArrayList<>();
        int siguiente = 0;
        int maxVivos = 400;
        for (int j = 0; j < N_OPS; j++) {
            boolean liberar = !vivos.isEmpty() && (vivos.size() >= maxVivos || r.nextInt(100) < 45);
            if (liberar) {
                int k = r.nextInt(vivos.size());
                tipo[j] = 1;
                proc[j] = vivos.get(k);
                vivos.set(k, vivos.get(vivos.size() - 1));   // quitar sin desplazar
                vivos.remove(vivos.size() - 1);
            } else {
                tipo[j] = 0;
                proc[j] = siguiente;
                tam[j] = 20 + r.nextInt(581);
                vivos.add(siguiente++);
            }
        }
        return new Escenario(ini, tb, tipo, proc, tam);
    }

    static double mediana(long[] nanos, int desde) {
        long[] v = Arrays.copyOfRange(nanos, desde, nanos.length);
        Arrays.sort(v);
        return v[v.length / 2] / 1e6;
    }

    @Test
    void medirCContraJava() {
        Escenario e = generar();
        MotorNativo nativo = new MotorNativo();
        MotorJava java = new MotorJava();

        int corridas = DESCARTADAS + MEDIDAS;
        long[] tC = new long[corridas];
        long[] tJ = new long[corridas];
        Resultado rC = null, rJ = null;

        for (int i = 0; i < corridas; i++) {
            long t0 = System.nanoTime();
            rC = nativo.simular(e, MEJOR_AJUSTE);
            tC[i] = System.nanoTime() - t0;

            t0 = System.nanoTime();
            rJ = java.simular(e, MEJOR_AJUSTE);
            tJ[i] = System.nanoTime() - t0;
        }

        boolean igual = rC.igualA(rJ);
        long rechazos = Arrays.stream(rC.causaRechazo()).filter(c -> c != 0).count();
        double medC = mediana(tC, DESCARTADAS);
        double medJ = mediana(tJ, DESCARTADAS);

        Locale l = Locale.US;
        System.out.println();
        System.out.println("=== RQ-01 desempeno: C (JNA) contra Java ===");
        System.out.printf(l, "Estimulo: %d operaciones, %d bloques, mejor ajuste, semilla %d%n", N_OPS, N_BLOQUES, SEMILLA);
        System.out.printf(l, "Secuencia: huella tipo=%d proc=%d tam=%d%n",
                Arrays.hashCode(e.opTipo()), Arrays.hashCode(e.opProceso()), Arrays.hashCode(e.opTamano()));
        System.out.printf(l, "Entorno: Java %s, %s, %d CPU, so %s%n",
                System.getProperty("java.version"), System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(), System.getProperty("os.name"));
        System.out.println();
        System.out.println("| corrida | C (ms)   | Java (ms) | nota |");
        System.out.println("|---------|----------|-----------|------|");
        for (int i = 0; i < corridas; i++) {
            System.out.printf(l, "| %d       | %8.2f | %9.2f | %s |%n",
                    i + 1, tC[i] / 1e6, tJ[i] / 1e6,
                    i < DESCARTADAS ? "descartada (calentamiento)" : "medida");
        }
        System.out.println();
        System.out.printf(l, "Mediana de %d corridas: C = %.2f ms, Java = %.2f ms%n", MEDIDAS, medC, medJ);
        System.out.printf(l, "Relacion Java / C = %.2f  (mayor que 1: C es mas rapido)%n", medJ / medC);
        System.out.printf(l, "Mismo estado final y mismas metricas: %s%n", igual ? "SI" : "NO");
        System.out.printf(l, "Bloques finales: %d, rechazos: %d, interna: %d KB, externa: %d KB, hueco mayor: %d KB%n",
                rC.ocupante().length, rechazos, rC.fragInterna(), rC.fragExterna(), rC.huecoMayor());
        System.out.printf(l, "Corrida completa en menos de 1 s (C): %s%n", medC < 1000.0 ? "SI" : "NO");
        System.out.println();

        assertTrue(igual, "C y Java deben devolver el mismo estado final y las mismas metricas");
    }
}
