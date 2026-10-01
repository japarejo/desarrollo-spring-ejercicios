package com.atech.curso.m4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.atech.curso.m4.dominio.EquipoRepository;
import com.atech.curso.m4.dominio.Sala;
import com.atech.curso.m4.dominio.SalaRepository;
import com.atech.curso.m4.dominio.TipoSala;
import com.atech.curso.m4.servicio.ReglaNegocioException;
import com.atech.curso.m4.servicio.SalaService;
import com.atech.curso.m4.web.SalaForm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EJ 4.5 - La regla del aforo se cumple aunque el formulario no haya validado nada: el servicio la protege
 * por sí mismo. Test unitario con Mockito, sin Spring.
 */
@ExtendWith(MockitoExtension.class)
class SalaServiceTest {

    @Mock
    SalaRepository salas;

    @Mock
    EquipoRepository equipos;

    @InjectMocks
    SalaService servicio;

    private static SalaForm sala(TipoSala tipo, int capacidad) {
        SalaForm form = new SalaForm();
        form.setNombre("Babbage");
        form.setTipo(tipo);
        form.setCapacidad(capacidad);
        return form;
    }

    @Test
    void tipoSalaAdmiteHastaSuAforoMaximo() {
        assertThat(TipoSala.REUNIONES.admite(20)).isTrue();
        assertThat(TipoSala.REUNIONES.admite(21)).isFalse();
        assertThat(TipoSala.AUDITORIO.admite(21)).isTrue();
    }

    @Test
    void noGuardaUnaSalaQueSuperaElAforoDeSuTipo() {
        assertThatThrownBy(() -> servicio.guardar(sala(TipoSala.REUNIONES, 120)))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessage("Una sala de tipo REUNIONES admite como máximo 20 personas");
        verify(salas, never()).save(any());
    }

    @Test
    void guardaUnaSalaDentroDelAforoDeSuTipo() {
        given(salas.save(any(Sala.class))).willAnswer(invocacion -> invocacion.getArgument(0));

        Sala guardada = servicio.guardar(sala(TipoSala.AUDITORIO, 120));

        assertThat(guardada.getCapacidad()).isEqualTo(120);
        verify(salas).save(any(Sala.class));
    }
}
