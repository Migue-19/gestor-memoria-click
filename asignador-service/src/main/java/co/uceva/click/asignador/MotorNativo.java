package co.uceva.click.asignador;

import com.sun.jna.ptr.IntByReference;
import java.util.Arrays;

/** Invoca libmemoria.so por JNA. Java preasigna las salidas; C no reserva memoria hacia afuera. */
public final class MotorNativo implements Motor {

    private final MemoriaLib lib;

    /** Lanza UnsatisfiedLinkError si libmemoria.so no se encuentra (ver jna.library.path). */
    public MotorNativo() {
        this.lib = MemoriaLib.cargar();
    }

    @Override
    public Resultado simular(Escenario e, int estrategia) {
        int nBloques = e.nBloques();
        int nOps = e.nOps();
        int capacidad = nBloques + nOps;   // contrato: cada asignacion parte a lo sumo un bloque

        int[] ocupante = new int[capacidad];
        int[] tamano = new int[capacidad];
        int[] causa = new int[nOps];
        IntByReference nFinal = new IntByReference();
        IntByReference interna = new IntByReference();
        IntByReference externa = new IntByReference();
        IntByReference hueco = new IntByReference();

        int rc = lib.simular(
                e.bloqueInicio(), e.bloqueTamano(), nBloques,
                e.opTipo(), e.opProceso(), e.opTamano(), nOps,
                estrategia,
                ocupante, tamano, nFinal,
                causa, interna, externa, hueco);

        if (rc != 0) {
            throw new SimulacionException(rc);
        }
        int n = nFinal.getValue();
        return new Resultado(
                Arrays.copyOf(ocupante, n), Arrays.copyOf(tamano, n), causa,
                interna.getValue(), externa.getValue(), hueco.getValue());
    }
}
