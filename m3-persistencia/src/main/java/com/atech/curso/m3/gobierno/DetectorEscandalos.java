package com.atech.curso.m3.gobierno;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

/**
 * EJ 3.7 - La prensa investiga a cada titular antes de su nombramiento.
 * <p>
 * Se tira un único número aleatorio en [0, 1) y se reparte el intervalo entre los tres escándalos:
 * <pre>
 *  0          casoplon    +cutreMaster   +joyasOcultas          1
 *  |--Casoplón--|--CutreMaster--|--JoyasOcultas--|---limpio---|
 * </pre>
 * Así cada escándalo sale exactamente con la probabilidad configurada y nunca dos a la vez.
 */
@Component
public class DetectorEscandalos {

    private final RandomGenerator azar;
    private volatile ProbabilidadesEscandalo probabilidades;
    /** Orden del órgano en el que estallará un escándalo seguro (null = dejarlo al azar). */
    private volatile Integer chivatazo;

    public DetectorEscandalos(RandomGenerator azar, ProbabilidadesEscandalo probabilidades) {
        this.azar = azar;
        this.probabilidades = probabilidades;
    }

    public void investigar(String titular, Organo organo) throws EscandaloException {
        if (chivatazo != null && chivatazo == organo.getOrden()) {
            chivatazo = null;
            throw switch (azar.nextInt(3)) {
                case 0 -> new CasoplonException(titular, organo.getNombre());
                case 1 -> new CutreMasterException(titular, organo.getNombre());
                default -> new JoyasOcultasException(titular, organo.getNombre());
            };
        }
        ProbabilidadesEscandalo p = probabilidades;
        double tirada = azar.nextDouble();
        if (tirada < p.casoplon()) {
            throw new CasoplonException(titular, organo.getNombre());
        }
        tirada -= p.casoplon();
        if (tirada < p.cutreMaster()) {
            throw new CutreMasterException(titular, organo.getNombre());
        }
        tirada -= p.cutreMaster();
        if (tirada < p.joyasOcultas()) {
            throw new JoyasOcultasException(titular, organo.getNombre());
        }
    }

    /** Probabilidad de completar {@code nombramientos} seguidos sin ningún escándalo: (1 - p)^n. */
    public double probabilidadDeExito(int nombramientos) {
        return Math.pow(1 - probabilidades.total(), nombramientos);
    }

    public ProbabilidadesEscandalo getProbabilidades() {
        return probabilidades;
    }

    /**
     * Alguien filtra a la prensa un escándalo del titular de ese órgano: en la próxima alternancia estallará
     * seguro al llegar a él (uno de los tres, al azar). Sirve para hacer la demostración sin esperar a la suerte.
     */
    public void filtrarChivatazo(int ordenOrgano) {
        this.chivatazo = ordenOrgano;
    }

    public void olvidarChivatazo() {
        this.chivatazo = null;
    }

    /** Permite cambiar las probabilidades en caliente (desde la consola). */
    public void ajustar(ProbabilidadesEscandalo nuevas) {
        this.probabilidades = nuevas;
    }
}
