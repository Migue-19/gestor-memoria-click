package co.uceva.click.asignador;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

/**
 * Interfaz JNA hacia libmemoria.so. Refleja la firma de memoria.h:
 * una llamada por corrida, arreglos planos de enteros, salidas preasignadas por Java.
 */
public interface MemoriaLib extends Library {

    /** Carga la biblioteca. Lanza UnsatisfiedLinkError si no la encuentra. */
    static MemoriaLib cargar() {
        return Native.load("memoria", MemoriaLib.class);
    }

    int simular(
            int[] bloqueInicio, int[] bloqueTamano, int nBloques,
            int[] opTipo, int[] opProceso, int[] opTamano, int nOps,
            int estrategia,
            int[] estadoOcupante, int[] estadoTamano, IntByReference nBloquesFinal,
            int[] causaRechazo,
            IntByReference fragInterna, IntByReference fragExterna,
            IntByReference huecoMayor);
}
