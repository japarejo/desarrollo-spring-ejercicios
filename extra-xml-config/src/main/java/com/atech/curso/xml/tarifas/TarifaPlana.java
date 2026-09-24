package com.atech.curso.xml.tarifas;

import java.math.BigDecimal;

import com.atech.curso.xml.dominio.Sala;

/** Segunda implementación: sirve para ver cómo se desambigua en cada estilo de configuración. */
public class TarifaPlana implements Tarifa {

    private BigDecimal precioDia = BigDecimal.ZERO;

    public void setPrecioDia(BigDecimal precioDia) {
        this.precioDia = precioDia;
    }

    public BigDecimal getPrecioDia() {
        return precioDia;
    }

    @Override
    public String nombre() {
        return "PLANA";
    }

    @Override
    public BigDecimal importe(Sala sala, int horas) {
        return precioDia;
    }
}
