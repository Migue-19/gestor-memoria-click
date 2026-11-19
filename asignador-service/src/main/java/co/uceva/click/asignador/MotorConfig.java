package co.uceva.click.asignador;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige el motor por configuracion (asignador.motor). Decision D-18: si se pide "native"
 * y libmemoria.so no se encuentra, el servicio NO arranca (falla rapido y con mensaje claro);
 * para operar sin la biblioteca se configura "java" de forma explicita.
 */
@Configuration
public class MotorConfig {

    @Bean
    public Motor motor(@Value("${asignador.motor:native}") String modo) {
        switch (modo.trim().toLowerCase()) {
            case "java":
                return new MotorJava();
            case "native":
                try {
                    return new MotorNativo();
                } catch (UnsatisfiedLinkError e) {
                    throw new IllegalStateException(
                            "asignador.motor=native pero no se pudo cargar libmemoria.so. "
                            + "jna.library.path=" + System.getProperty("jna.library.path")
                            + ". Verifique la ruta o use asignador.motor=java. Causa: " + e.getMessage(), e);
                }
            default:
                throw new IllegalStateException(
                        "asignador.motor debe ser 'native' o 'java', no '" + modo + "'");
        }
    }
}
