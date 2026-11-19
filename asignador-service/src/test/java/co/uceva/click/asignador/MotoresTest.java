package co.uceva.click.asignador;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * RN-07: Java y C devuelven exactamente los mismos enteros en las 30 corridas de referencia.
 * Ademas: un caso de punta a punta por JNA con valor calculado a mano, y una prueba
 * con escenarios aleatorios (semilla fija) que tambien compara los codigos de error.
 */
class MotoresTest {

    static MotorNativo nativo;
    static MotorJava java = new MotorJava();

    @BeforeAll
    static void cargarBiblioteca() {
        // Falla si libmemoria.so no se encuentra: construir antes con "make" en ../c
        nativo = new MotorNativo();
    }

    // ---------- casos de referencia (mismas entradas que la suite en C) ----------

    private static Escenario caso(int[] tb, int[] tipo, int[] proc, int[] tam) {
        int[] ini = new int[tb.length];
        int dir = 0;
        for (int i = 0; i < tb.length; i++) { ini[i] = dir; dir += tb[i]; }
        return new Escenario(ini, tb, tipo, proc, tam);
    }

    static Stream<Arguments> corridas() {
        Escenario[] casos = {
            caso(new int[]{100,500,200,300}, new int[]{0,0,0,0}, new int[]{1,2,3,4}, new int[]{212,417,112,426}),
            caso(new int[]{200,200,200,200}, new int[]{0,0,0,1,1,0}, new int[]{1,2,3,1,2,4}, new int[]{200,200,200,0,0,350}),
            caso(new int[]{100,100,100,100}, new int[]{0,0,0,0,1,1,0}, new int[]{1,2,3,4,1,3,5}, new int[]{100,100,100,100,0,0,150}),
            caso(new int[]{100,50,200}, new int[]{0,0,0,0}, new int[]{1,2,3,4}, new int[]{95,50,197,1}),
            caso(new int[]{100,100,100,100,100}, new int[]{0,0,0,0,1,1,1,0}, new int[]{1,2,3,4,1,3,2,5}, new int[]{100,100,100,100,0,0,0,250}),
            caso(new int[]{60,100,60,100,30}, new int[]{0,0,0,1,0,0}, new int[]{1,2,3,1,4,5}, new int[]{50,30,50,0,55,25}),
            caso(new int[]{300,200,100}, new int[]{0,0,0,0,1,0}, new int[]{1,2,3,4,2,5}, new int[]{300,200,100,10,0,150}),
            caso(new int[]{80,120,100,100}, new int[]{0,0,0,0,1,1,1,0}, new int[]{1,2,3,4,4,3,1,5}, new int[]{80,120,100,100,0,0,0,150}),
            caso(new int[]{150,80,200,60,110,40}, new int[]{0,0,0,1,0,1,0,0}, new int[]{1,2,3,1,4,3,5,6}, new int[]{70,60,120,0,100,0,90,170}),
            caso(new int[]{90,210,40,160,100,50}, new int[]{0,0,0,1,0,0,1,0}, new int[]{1,2,3,1,4,5,3,6}, new int[]{85,150,38,0,100,150,0,120}),
        };
        String[] est = {"primero", "mejor", "peor"};
        List<Arguments> lista = new ArrayList<>();
        for (int c = 0; c < casos.length; c++) {
            for (int e = 0; e < 3; e++) {
                lista.add(Arguments.of("caso " + (c + 1) + " / " + est[e], casos[c], e));
            }
        }
        return lista.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corridas")
    void javaYCDevuelvenLosMismosEnteros(String nombre, Escenario escenario, int estrategia) {
        Resultado deC = nativo.simular(escenario, estrategia);
        Resultado deJava = java.simular(escenario, estrategia);
        assertTrue(deC.igualA(deJava), () -> nombre + "\n C:    " + deC + "\n Java: " + deJava);
    }

    // ---------- un caso de punta a punta por JNA, con valor calculado a mano ----------

    @Test
    void casoDelCodelabPuntaAPunta() {
        Escenario e = caso(new int[]{100,500,200,300}, new int[]{0,0,0,0},
                new int[]{1,2,3,4}, new int[]{212,417,112,426});

        Resultado primero = nativo.simular(e, 0);
        assertArrayEquals(new int[]{-1,1,3,-1,-1,-1}, primero.ocupante());
        assertArrayEquals(new int[]{100,212,112,176,200,300}, primero.tamano());
        assertArrayEquals(new int[]{0,2,0,2}, primero.causaRechazo());
        assertEquals(300, primero.huecoMayor());
        assertEquals(476, primero.fragExterna());
        assertEquals(0, primero.fragInterna());

        Resultado mejor = nativo.simular(e, 1);
        assertArrayEquals(new int[]{-1,2,-1,3,-1,1,-1}, mejor.ocupante());
        assertArrayEquals(new int[]{100,417,83,112,88,212,88}, mejor.tamano());
        assertArrayEquals(new int[]{0,0,0,1}, mejor.causaRechazo());
        assertEquals(100, mejor.huecoMayor());
        assertEquals(259, mejor.fragExterna());

        Resultado peor = nativo.simular(e, 2);
        assertArrayEquals(new int[]{-1,1,-1,-1,3,-1}, peor.ocupante());
        assertArrayEquals(new int[]{100,212,288,200,112,188}, peor.tamano());
        assertArrayEquals(new int[]{0,2,0,2}, peor.causaRechazo());
        assertEquals(288, peor.huecoMayor());
        assertEquals(488, peor.fragExterna());
    }

    // ---------- errores: C devuelve codigos, Java los traduce a excepcion ----------

    @Test
    void erroresDelContratoSeTraducenAExcepcion() {
        Escenario liberarInexistente = caso(new int[]{100,100}, new int[]{1}, new int[]{7}, new int[]{0});
        SimulacionException ex = assertThrows(SimulacionException.class, () -> nativo.simular(liberarInexistente, 0));
        assertEquals(-4, ex.getCodigo());
        assertEquals(422, ex.httpSugerido());

        Escenario noContiguo = new Escenario(new int[]{0,150}, new int[]{100,100},
                new int[]{0}, new int[]{1}, new int[]{50});
        assertEquals(-3, assertThrows(SimulacionException.class, () -> nativo.simular(noContiguo, 0)).getCodigo());

        Escenario ok = caso(new int[]{100}, new int[]{0}, new int[]{1}, new int[]{50});
        assertEquals(-2, assertThrows(SimulacionException.class, () -> nativo.simular(ok, 5)).getCodigo());
    }

    // ---------- escenarios aleatorios con semilla fija: Java == C, incluidos los errores ----------

    private static Escenario aleatorio(Random r) {
        int nb = 3 + r.nextInt(20);
        int[] tb = new int[nb];
        for (int i = 0; i < nb; i++) tb[i] = 10 + r.nextInt(300);
        int nOps = 5 + r.nextInt(60);
        int[] tipo = new int[nOps], proc = new int[nOps], tam = new int[nOps];
        List<Integer> pedidos = new ArrayList<>();
        int siguiente = 0;
        for (int j = 0; j < nOps; j++) {
            if (!pedidos.isEmpty() && r.nextInt(100) < 30) {
                tipo[j] = 1;
                proc[j] = pedidos.remove(r.nextInt(pedidos.size()));
            } else {
                tipo[j] = 0;
                proc[j] = siguiente;
                tam[j] = 1 + r.nextInt(250);
                pedidos.add(siguiente++);
            }
        }
        return caso(tb, tipo, proc, tam);
    }

    private static Object ejecutar(Motor m, Escenario e, int est) {
        try {
            return m.simular(e, est);
        } catch (SimulacionException ex) {
            return ex.getCodigo();
        }
    }

    @Test
    void escenariosAleatoriosJavaIgualC() {
        Random r = new Random(42);
        int exitosos = 0;
        int total = 0;
        for (int i = 0; i < 300; i++) {
            Escenario e = aleatorio(r);
            for (int est = 0; est < 3; est++) {
                Object deC = ejecutar(nativo, e, est);
                Object deJava = ejecutar(java, e, est);
                total++;
                if (deC instanceof Resultado rc) {
                    exitosos++;
                    assertTrue(rc.igualA((Resultado) deJava), "escenario " + i + " estrategia " + est);
                } else {
                    assertEquals(deC, deJava, "codigo de error, escenario " + i + " estrategia " + est);
                }
            }
        }
        assertTrue(exitosos > total / 2, "la prueba debe ejercitar corridas exitosas: " + exitosos + "/" + total);
    }
}
