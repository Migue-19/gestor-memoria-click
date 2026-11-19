package co.uceva.click.asignador;

import java.util.Arrays;

/** Salida de una corrida. Los arreglos ya vienen recortados al numero de bloques finales. */
public record Resultado(
        int[] ocupante,        // proceso en cada bloque, -1 si libre
        int[] tamano,          // tamano final de cada bloque
        int[] causaRechazo,    // por operacion: 0 ok, 1 sin memoria, 2 fragmentacion
        int fragInterna,
        int fragExterna,
        int huecoMayor) {

    /** Igualdad entero por entero (los records no comparan el contenido de los arreglos). */
    public boolean igualA(Resultado o) {
        return o != null
                && fragInterna == o.fragInterna
                && fragExterna == o.fragExterna
                && huecoMayor == o.huecoMayor
                && Arrays.equals(ocupante, o.ocupante)
                && Arrays.equals(tamano, o.tamano)
                && Arrays.equals(causaRechazo, o.causaRechazo);
    }

    @Override
    public String toString() {
        return "Resultado{ocupante=" + Arrays.toString(ocupante)
                + ", tamano=" + Arrays.toString(tamano)
                + ", causas=" + Arrays.toString(causaRechazo)
                + ", interna=" + fragInterna + ", externa=" + fragExterna
                + ", huecoMayor=" + huecoMayor + "}";
    }
}
