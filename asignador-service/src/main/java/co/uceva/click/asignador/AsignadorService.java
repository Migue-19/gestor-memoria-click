package co.uceva.click.asignador;

import co.uceva.click.asignador.SimulacionDtos.BloqueEntrada;
import co.uceva.click.asignador.SimulacionDtos.Decision;
import co.uceva.click.asignador.SimulacionDtos.MapaBloque;
import co.uceva.click.asignador.SimulacionDtos.OperacionEntrada;
import co.uceva.click.asignador.SimulacionDtos.ResultadoEstrategia;
import co.uceva.click.asignador.SimulacionDtos.SimularRequest;
import co.uceva.click.asignador.SimulacionDtos.SimularResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Traduce entre el modelo del dominio (JSON) y los arreglos planos que entiende el motor. */
@Service
public class AsignadorService {

    private final Motor motor;
    private final String nombreMotor;

    public AsignadorService(Motor motor, @Value("${asignador.motor:native}") String nombreMotor) {
        this.motor = motor;
        this.nombreMotor = nombreMotor.trim().toLowerCase();
    }

    public SimularResponse simular(SimularRequest req) {
        if (req == null || req.bloques() == null || req.bloques().isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos un bloque");
        }
        if (req.operaciones() == null) {
            throw new IllegalArgumentException("Se requiere la lista de operaciones");
        }
        if (req.estrategias() == null || req.estrategias().isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos una estrategia");
        }

        Escenario escenario = aEscenario(req);
        List<ResultadoEstrategia> resultados = new ArrayList<>();
        for (String nombre : req.estrategias()) {
            int codigo = codigoEstrategia(nombre);
            Resultado r = motor.simular(escenario, codigo);
            resultados.add(aRespuesta(nombre, req, escenario, r));
        }
        return new SimularResponse(nombreMotor, resultados);
    }

    static int codigoEstrategia(String nombre) {
        if (nombre == null) {
            throw new IllegalArgumentException("Estrategia nula");
        }
        return switch (nombre) {
            case "PRIMERO_QUE_QUEPA" -> 0;
            case "MEJOR_QUE_QUEPA" -> 1;
            case "PEOR_QUE_QUEPA" -> 2;
            default -> throw new IllegalArgumentException(
                    "Estrategia desconocida: " + nombre
                    + " (use PRIMERO_QUE_QUEPA, MEJOR_QUE_QUEPA o PEOR_QUE_QUEPA)");
        };
    }

    private static Escenario aEscenario(SimularRequest req) {
        int nb = req.bloques().size();
        int[] ini = new int[nb];
        int[] tb = new int[nb];
        for (int i = 0; i < nb; i++) {
            BloqueEntrada b = req.bloques().get(i);
            ini[i] = b.inicio();
            tb[i] = b.tamano();
        }
        int no = req.operaciones().size();
        int[] tipo = new int[no];
        int[] proc = new int[no];
        int[] tam = new int[no];
        for (int j = 0; j < no; j++) {
            OperacionEntrada o = req.operaciones().get(j);
            if (o.tipo() == null) {
                throw new IllegalArgumentException("La operacion " + j + " no tiene tipo");
            }
            switch (o.tipo()) {
                case "ASIGNAR" -> tipo[j] = 0;
                case "LIBERAR" -> tipo[j] = 1;
                default -> throw new IllegalArgumentException(
                        "Tipo de operacion desconocido en la operacion " + j + ": " + o.tipo()
                        + " (use ASIGNAR o LIBERAR)");
            }
            proc[j] = o.proceso();
            tam[j] = (tipo[j] == 0 && o.tamano() != null) ? o.tamano() : 0;
        }
        return new Escenario(ini, tb, tipo, proc, tam);
    }

    private static ResultadoEstrategia aRespuesta(String nombre, SimularRequest req,
                                                  Escenario esc, Resultado r) {
        // Mapa final: las direcciones se reconstruyen por suma acumulada (decision D-09)
        List<MapaBloque> mapa = new ArrayList<>();
        int dir = esc.bloqueInicio()[0];
        int libreTotal = 0;
        for (int i = 0; i < r.ocupante().length; i++) {
            boolean libre = r.ocupante()[i] == -1;
            mapa.add(new MapaBloque(dir, r.tamano()[i], libre, libre ? null : r.ocupante()[i]));
            if (libre) {
                libreTotal += r.tamano()[i];
            }
            dir += r.tamano()[i];
        }

        List<Decision> decisiones = new ArrayList<>();
        int atendidas = 0;
        int rechazadas = 0;
        for (int j = 0; j < esc.nOps(); j++) {
            boolean asignar = esc.opTipo()[j] == 0;
            int causa = r.causaRechazo()[j];
            String tipo = asignar ? "ASIGNAR" : "LIBERAR";
            if (asignar) {
                if (causa == 0) atendidas++; else rechazadas++;
            }
            String causaTexto = switch (causa) {
                case 1 -> "SIN_MEMORIA";
                case 2 -> "FRAGMENTACION_EXTERNA";
                default -> null;
            };
            Integer asignadoEn = null;
            if (asignar && causa == 0) {
                for (MapaBloque b : mapa) {
                    if (b.proceso() != null && b.proceso() == esc.opProceso()[j]) {
                        asignadoEn = b.inicio();
                        break;
                    }
                }
            }
            decisiones.add(new Decision(j, tipo, esc.opProceso()[j],
                    asignar ? esc.opTamano()[j] : null, causa == 0, causaTexto, asignadoEn));
        }

        double porcentaje = libreTotal == 0 ? 0.0
                : Math.round((1.0 - (double) r.huecoMayor() / libreTotal) * 100.0) / 100.0;
        return new ResultadoEstrategia(nombre, atendidas, rechazadas, libreTotal, r.huecoMayor(),
                r.fragExterna(), porcentaje, r.fragInterna(), mapa, decisiones);
    }
}
