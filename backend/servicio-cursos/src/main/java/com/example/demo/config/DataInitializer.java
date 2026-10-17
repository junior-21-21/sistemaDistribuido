package com.example.demo.config;

import com.example.demo.model.TicketMantenimiento;
import com.example.demo.repository.TicketMantenimientoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private final TicketMantenimientoRepository ticketRepository;

    public DataInitializer(TicketMantenimientoRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (ticketRepository.count() == 0) {
            TicketMantenimiento t1 = new TicketMantenimiento();
            t1.setDocenteId(2L); // asumiendo ID 2 para un docente
            t1.setAulaId(101L);
            t1.setCategoria("MOBILIARIO");
            t1.setDescripcion("Dos carpetas tienen las patas rotas y representan un peligro para los estudiantes.");
            t1.setUrgencia("MEDIA");
            t1.setEstado("PENDIENTE");
            
            TicketMantenimiento t2 = new TicketMantenimiento();
            t2.setDocenteId(2L);
            t2.setAulaId(204L);
            t2.setCategoria("ELECTRICO");
            t2.setDescripcion("El proyector no enciende y el tomacorriente tira chispas.");
            t2.setUrgencia("EMERGENCIA");
            t2.setEstado("PENDIENTE");

            ticketRepository.save(t1);
            ticketRepository.save(t2);

            System.out.println(">>> Tickets de mantenimiento inicializados.");
        }
    }
}
