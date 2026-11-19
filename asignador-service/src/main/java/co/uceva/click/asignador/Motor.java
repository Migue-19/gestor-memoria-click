package co.uceva.click.asignador;

/** Una forma de ejecutar la simulacion: nativa (C por JNA) o Java (linea base y respaldo). */
public interface Motor {

    /** @param estrategia 0 = primer ajuste, 1 = mejor ajuste, 2 = peor ajuste */
    Resultado simular(Escenario escenario, int estrategia);
}
