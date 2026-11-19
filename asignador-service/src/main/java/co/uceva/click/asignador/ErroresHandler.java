package co.uceva.click.asignador;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce los codigos de error de C (y los datos mal formados) a respuestas HTTP explicadas. */
@RestControllerAdvice
public class ErroresHandler {

    @ExceptionHandler(SimulacionException.class)
    public ResponseEntity<Map<String, Object>> contrato(SimulacionException e) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("codigo", e.getCodigo());
        cuerpo.put("mensaje", SimulacionException.descripcion(e.getCodigo()));
        return ResponseEntity.status(e.httpSugerido()).body(cuerpo);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> peticionInvalida(Exception e) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", e instanceof IllegalArgumentException
                ? e.getMessage() : "El cuerpo de la peticion no es un JSON valido");
        return ResponseEntity.badRequest().body(cuerpo);
    }
}
