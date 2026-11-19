package co.uceva.click.asignador;

/** Error de contrato: C no lanza excepciones, devuelve codigos; aqui se traducen. */
public class SimulacionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int codigo;

    public SimulacionException(int codigo) {
        super("simular() devolvio el codigo " + codigo + ": " + descripcion(codigo));
        this.codigo = codigo;
    }

    public int getCodigo() { return codigo; }

    /** Codigo HTTP sugerido para la capa web. */
    public int httpSugerido() {
        return switch (codigo) {
            case -2, -3, -6, -7 -> 400;
            case -4, -5 -> 422;
            default -> 500;
        };
    }

    public static String descripcion(int codigo) {
        return switch (codigo) {
            case -1 -> "puntero nulo o cantidad negativa";
            case -2 -> "estrategia invalida";
            case -3 -> "mapa invalido (vacio, no contiguo o tamano <= 0)";
            case -4 -> "se libera un proceso que no esta en el mapa";
            case -5 -> "se asigna un proceso que ya esta en el mapa";
            case -6 -> "tipo de operacion invalido o id de proceso negativo";
            case -7 -> "tamano pedido <= 0";
            case -8 -> "sin memoria para el trabajo interno";
            default -> "codigo desconocido";
        };
    }
}
