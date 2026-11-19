package co.uceva.click.asignador;

import java.util.List;

/** Formas JSON de POST /simular (decision D-12: mismo formato que el codelab, con campos extra). */
public final class SimulacionDtos {

    private SimulacionDtos() { }

    public record BloqueEntrada(int inicio, int tamano) { }

    /** tipo: "ASIGNAR" o "LIBERAR". tamano se ignora al liberar. */
    public record OperacionEntrada(String tipo, int proceso, Integer tamano) { }

    /** estrategias: PRIMERO_QUE_QUEPA, MEJOR_QUE_QUEPA, PEOR_QUE_QUEPA. */
    public record SimularRequest(List<BloqueEntrada> bloques,
                                 List<OperacionEntrada> operaciones,
                                 List<String> estrategias) { }

    public record MapaBloque(int inicio, int tamano, boolean libre, Integer proceso) { }

    /** asignadoEn solo se conoce si el proceso sigue en el mapa final; si no, es null. */
    public record Decision(int operacion, String tipo, int proceso, Integer solicitud,
                           boolean aceptada, String causaRechazo, Integer asignadoEn) { }

    public record ResultadoEstrategia(String estrategia,
                                      int atendidas, int rechazadas,
                                      int libreTotal, int huecoMayor,
                                      int fragmentacionExternaKb, double fragmentacionExterna,
                                      int fragmentacionInterna,
                                      List<MapaBloque> mapa, List<Decision> decisiones) { }

    public record SimularResponse(String motor, List<ResultadoEstrategia> resultados) { }
}
