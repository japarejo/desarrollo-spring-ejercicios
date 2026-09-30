package com.atech.curso.m3;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.atech.curso.m3.dominio.Direccion;
import com.atech.curso.m3.dominio.Intervencion;
import com.atech.curso.m3.dominio.Reserva;
import com.atech.curso.m3.dominio.Sala;
import com.atech.curso.m3.repositorio.IntervencionRepository;
import com.atech.curso.m3.repositorio.SalaRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest 
public class TestEntityManager {
    @Autowired 
    private EntityManager em;
    @Autowired 
    SalaRepository salaRepository;
    @Autowired 
    IntervencionRepository intervencionRepository;
    
    @Test 
    public void test() {
        
        Sala s=em.find(Sala.class,1L);
        Sala s2=new Sala("Satoru Iwata", 100, new Direccion("Calle Nintendo", "Kioto", "600-0000"));
        assertNotNull(s);
        em.persist(s2);
        System.out.println(s2.getId()+" - "+s2.getNombre());
        
    }   

    @Test 
    public void test2() {
        salaRepository.findByDireccionCiudadIgnoreCase("Sevilla").
                forEach(s->System.out.println(s.getId()+" - "+s.getNombre()));
    }

    @Test 
    public void test3() {
        intervencionRepository.findAll().
            forEach(i->System.out.println(i.getId()+" - "+i.getContenido()));
        
        Reserva r=em.find(Reserva.class, 1L);
        Intervencion i=new Intervencion();
        i.setContenido("Quierooo und descansoooo por DIOOOOOOSSSS!!!!");
        i.setFechaHora(LocalDateTime.now());
        i.setReserva(r);
        intervencionRepository.save(i);
        Intervencion i2=new Intervencion();
        i2.setContenido("Continuamos con la reunión, no hay descanso para los héroes");
        i2.setFechaHora(LocalDateTime.now().plusMinutes(2));
        i2.setReserva(r);
        intervencionRepository.save(i2);        

        List<Intervencion> is=intervencionRepository.findByReservaIdOrderByFechaHora(r.getId());
        is.stream().forEach(inter->System.out.println(inter.getId()+" - "+inter.getContenido()));  

    }
}
