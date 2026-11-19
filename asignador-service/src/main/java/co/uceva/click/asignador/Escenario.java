package co.uceva.click.asignador;

/** Entrada de una corrida: el mapa de bloques y la secuencia de operaciones (arreglos planos). */
public record Escenario(
        int[] bloqueInicio,
        int[] bloqueTamano,
        int[] opTipo,       // 0 = asignar, 1 = liberar
        int[] opProceso,    // id del proceso (>= 0)
        int[] opTamano) {   // KB pedidos (ignorado si es liberar)

    public Escenario {
        if (bloqueInicio == null || bloqueTamano == null || opTipo == null
                || opProceso == null || opTamano == null) {
            throw new IllegalArgumentException("Los arreglos del escenario no pueden ser nulos");
        }
        if (bloqueInicio.length != bloqueTamano.length) {
            throw new IllegalArgumentException("bloqueInicio y bloqueTamano deben tener la misma longitud");
        }
        if (opTipo.length != opProceso.length || opTipo.length != opTamano.length) {
            throw new IllegalArgumentException("opTipo, opProceso y opTamano deben tener la misma longitud");
        }
    }

    public int nBloques() { return bloqueInicio.length; }

    public int nOps() { return opTipo.length; }
}
