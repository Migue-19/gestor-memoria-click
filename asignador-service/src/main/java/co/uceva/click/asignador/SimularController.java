package co.uceva.click.asignador;

import co.uceva.click.asignador.SimulacionDtos.SimularRequest;
import co.uceva.click.asignador.SimulacionDtos.SimularResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimularController {

    private final AsignadorService servicio;

    public SimularController(AsignadorService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/simular")
    public SimularResponse simular(@RequestBody SimularRequest peticion) {
        return servicio.simular(peticion);
    }
}
