package co.uceva.click.asignador;

import static org.junit.jupiter.api.Assertions.*;

import co.uceva.click.asignador.SimulacionDtos.BloqueEntrada;
import co.uceva.click.asignador.SimulacionDtos.Decision;
import co.uceva.click.asignador.SimulacionDtos.OperacionEntrada;
import co.uceva.click.asignador.SimulacionDtos.ResultadoEstrategia;
import co.uceva.click.asignador.SimulacionDtos.SimularRequest;
import co.uceva.click.asignador.SimulacionDtos.SimularResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Prueba de la capa de traduccion (@Service) sin levantar Spring: el caso del codelab. */
class AsignadorServiceTest {

    private static SimularRequest casoDelCodelab() {
        return new SimularRequest(
                List.of(new BloqueEntrada(0, 100), new BloqueEntrada(100, 500),
                        new BloqueEntrada(600, 200), new BloqueEntrada(800, 300)),
                List.of(new OperacionEntrada("ASIGNAR", 1, 212), new OperacionEntrada("ASIGNAR", 2, 417),
                        new OperacionEntrada("ASIGNAR", 3, 112), new OperacionEntrada("ASIGNAR", 4, 426)),
                List.of("PRIMERO_QUE_QUEPA", "MEJOR_QUE_QUEPA", "PEOR_QUE_QUEPA"));
    }

    @Test
    void respuestaDelCasoDelCodelabConMotorJava() {
        SimularResponse resp = new AsignadorService(new MotorJava(), "java").simular(casoDelCodelab());
        assertEquals("java", resp.motor());
        assertEquals(3, resp.resultados().size());

        ResultadoEstrategia primero = resp.resultados().get(0);
        assertEquals("PRIMERO_QUE_QUEPA", primero.estrategia());
        assertEquals(2, primero.atendidas());
        assertEquals(2, primero.rechazadas());
        assertEquals(776, primero.libreTotal());
        assertEquals(300, primero.huecoMayor());
        assertEquals(476, primero.fragmentacionExternaKb());
        assertEquals(0.61, primero.fragmentacionExterna(), 1e-9);
        assertEquals(0, primero.fragmentacionInterna());
        assertEquals(6, primero.mapa().size());
        assertEquals(0, primero.mapa().get(0).inicio());
        assertTrue(primero.mapa().get(0).libre());
        assertEquals(100, primero.mapa().get(1).inicio());
        assertEquals(212, primero.mapa().get(1).tamano());
        assertEquals(1, primero.mapa().get(1).proceso());
        assertEquals(312, primero.mapa().get(2).inicio());

        Decision d0 = primero.decisiones().get(0);
        assertTrue(d0.aceptada());
        assertEquals(100, d0.asignadoEn());
        Decision d1 = primero.decisiones().get(1);
        assertFalse(d1.aceptada());
        assertEquals("FRAGMENTACION_EXTERNA", d1.causaRechazo());
        assertNull(d1.asignadoEn());

        ResultadoEstrategia mejor = resp.resultados().get(1);
        assertEquals(3, mejor.atendidas());
        assertEquals(1, mejor.rechazadas());
        assertEquals(359, mejor.libreTotal());
        assertEquals(0.72, mejor.fragmentacionExterna(), 1e-9);
        assertEquals("SIN_MEMORIA", mejor.decisiones().get(3).causaRechazo());

        ResultadoEstrategia peor = resp.resultados().get(2);
        assertEquals(2, peor.atendidas());
        assertEquals(288, peor.huecoMayor());
        assertEquals(0.63, peor.fragmentacionExterna(), 1e-9);
    }

    @Test
    void motorNativoYJavaDanLaMismaRespuesta() {
        SimularResponse deJava = new AsignadorService(new MotorJava(), "x").simular(casoDelCodelab());
        SimularResponse deC = new AsignadorService(new MotorNativo(), "x").simular(casoDelCodelab());
        assertEquals(deJava, deC);
    }

    @Test
    void liberarUnProcesoInexistenteLanzaElCodigoDelContrato() {
        SimularRequest req = new SimularRequest(
                List.of(new BloqueEntrada(0, 100)),
                List.of(new OperacionEntrada("LIBERAR", 9, null)),
                List.of("PRIMERO_QUE_QUEPA"));
        SimulacionException ex = assertThrows(SimulacionException.class,
                () -> new AsignadorService(new MotorJava(), "java").simular(req));
        assertEquals(-4, ex.getCodigo());
        assertEquals(422, ex.httpSugerido());
    }

    @Test
    void datosMalFormadosSeRechazanConMensaje() {
        AsignadorService s = new AsignadorService(new MotorJava(), "java");
        SimularRequest estrategiaMala = new SimularRequest(
                List.of(new BloqueEntrada(0, 100)), List.of(), List.of("MAGICA"));
        assertThrows(IllegalArgumentException.class, () -> s.simular(estrategiaMala));
        SimularRequest sinBloques = new SimularRequest(List.of(), List.of(), List.of("PEOR_QUE_QUEPA"));
        assertThrows(IllegalArgumentException.class, () -> s.simular(sinBloques));
        SimularRequest tipoMalo = new SimularRequest(
                List.of(new BloqueEntrada(0, 100)),
                List.of(new OperacionEntrada("BORRAR", 1, 10)), List.of("PEOR_QUE_QUEPA"));
        assertThrows(IllegalArgumentException.class, () -> s.simular(tipoMalo));
    }
}
