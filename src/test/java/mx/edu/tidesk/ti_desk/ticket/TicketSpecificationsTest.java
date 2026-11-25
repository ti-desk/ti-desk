package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import mx.edu.tidesk.ti_desk.IntegracionBaseTest;
import mx.edu.tidesk.ti_desk.ticket.dto.FiltroTickets;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketResponse;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * RF-05 con PostgreSQL real: visibilidad por rol, filtros y ausencia de N+1.
 * Cada prueba crea sus propios usuarios y tickets con datos únicos para no
 * depender de lo que hayan creado otras pruebas en la misma base.
 */
class TicketSpecificationsTest extends IntegracionBaseTest {

    private static final Pageable TODO = PageRequest.of(0, 100);

    @Autowired
    TicketService servicio;

    @Autowired
    TicketRepository tickets;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    EntityManagerFactory emf;

    @Test
    void elUsuarioSoloVeLosTicketsQueCreo() {
        Usuario ana = usuario(Rol.USUARIO);
        Usuario beto = usuario(Rol.USUARIO);
        Ticket deAna = ticket(ana, null, Prioridad.MEDIA, false);
        Ticket deBeto = ticket(beto, null, Prioridad.MEDIA, false);

        List<String> folios = folios(FiltroTickets.sinFiltros(), ana.getId(), Rol.USUARIO);

        assertThat(folios).contains(deAna.getFolio()).doesNotContain(deBeto.getFolio());
    }

    @Test
    void elTecnicoVeLosSuyosYLosSinAsignarPeroNoLosDeOtroTecnico() {
        Usuario solicitante = usuario(Rol.USUARIO);
        Usuario tecnico = usuario(Rol.TECNICO);
        Usuario otroTecnico = usuario(Rol.TECNICO);
        Ticket asignadoAMi = ticket(solicitante, tecnico, Prioridad.ALTA, false);
        Ticket sinAsignar = ticket(solicitante, null, Prioridad.ALTA, false);
        Ticket deOtro = ticket(solicitante, otroTecnico, Prioridad.ALTA, false);

        List<String> folios = folios(FiltroTickets.sinFiltros(), tecnico.getId(), Rol.TECNICO);

        assertThat(folios).contains(asignadoAMi.getFolio(), sinAsignar.getFolio())
                .doesNotContain(deOtro.getFolio());
    }

    @Test
    void elAdminVeTodos() {
        Usuario solicitante = usuario(Rol.USUARIO);
        Usuario tecnico = usuario(Rol.TECNICO);
        Usuario admin = usuario(Rol.ADMIN);
        Ticket uno = ticket(solicitante, tecnico, Prioridad.BAJA, false);
        Ticket dos = ticket(solicitante, null, Prioridad.BAJA, false);

        List<String> folios = folios(FiltroTickets.sinFiltros(), admin.getId(), Rol.ADMIN);

        assertThat(folios).contains(uno.getFolio(), dos.getFolio());
    }

    @Test
    void losFiltrosSeCombinan() {
        Usuario solicitante = usuario(Rol.USUARIO);
        Usuario tecnico = usuario(Rol.TECNICO);
        Usuario admin = usuario(Rol.ADMIN);
        Ticket buscado = ticket(solicitante, tecnico, Prioridad.CRITICA, true);
        Ticket noVencido = ticket(solicitante, tecnico, Prioridad.CRITICA, false);
        Ticket otraPrioridad = ticket(solicitante, tecnico, Prioridad.BAJA, true);
        Ticket otroTecnico = ticket(solicitante, usuario(Rol.TECNICO), Prioridad.CRITICA, true);

        FiltroTickets filtro = new FiltroTickets(EstadoTicket.ABIERTO, Prioridad.CRITICA, tecnico.getId(), true);
        List<TicketResponse> resultado = servicio.listar(filtro, TODO, admin.getId(), Rol.ADMIN).contenido();

        assertThat(resultado).extracting(TicketResponse::folio)
                .containsExactly(buscado.getFolio())
                .doesNotContain(noVencido.getFolio(), otraPrioridad.getFolio(), otroTecnico.getFolio());
    }

    @Test
    void listarUnaPaginaNoGeneraConsultasNMasUno() {
        Usuario solicitante = usuario(Rol.USUARIO);
        Usuario tecnico = usuario(Rol.TECNICO);
        for (int i = 0; i < 5; i++) {
            ticket(solicitante, tecnico, Prioridad.MEDIA, false);
        }

        Statistics estadisticas = emf.unwrap(SessionFactory.class).getStatistics();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            List<TicketResponse> resultado = servicio.listar(
                    FiltroTickets.sinFiltros(), PageRequest.of(0, 3), solicitante.getId(), Rol.USUARIO).contenido();

            assertThat(resultado).hasSize(3).allSatisfy(t -> {
                assertThat(t.solicitanteId()).isEqualTo(solicitante.getId());
                assertThat(t.tecnicoId()).isEqualTo(tecnico.getId());
            });
            // Una consulta para la página y otra para el total; nada por cada ticket.
            assertThat(estadisticas.getPrepareStatementCount()).isLessThanOrEqualTo(2);
        } finally {
            estadisticas.setStatisticsEnabled(false);
        }
    }

    private List<String> folios(FiltroTickets filtro, Long usuarioId, Rol rol) {
        return servicio.listar(filtro, TODO, usuarioId, rol).contenido().stream()
                .map(TicketResponse::folio)
                .toList();
    }

    private Usuario usuario(Rol rol) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Usuario u = new Usuario();
        u.setNombre("Prueba " + id);
        u.setEmail("prueba." + id + "@unistmo.edu.mx");
        u.setPasswordHash("$2a$10$hashDePruebaNoSeUsaParaLogin..........................");
        u.setRol(rol);
        return usuarios.save(u);
    }

    private Ticket ticket(Usuario solicitante, Usuario tecnico, Prioridad prioridad, boolean vencido) {
        Ticket t = new Ticket();
        t.setFolio("PRB-" + UUID.randomUUID().toString().substring(0, 12));
        t.setTitulo("Ticket de prueba");
        t.setDescripcion("Creado por TicketSpecificationsTest");
        t.setCategoria(Categoria.OTRO);
        t.setPrioridad(prioridad);
        t.setEstado(EstadoTicket.ABIERTO);
        t.setSolicitante(solicitante);
        t.setTecnico(tecnico);
        t.setCreadoEn(Instant.now());
        t.setFechaLimiteSla(Instant.now().plusSeconds(3600));
        t.setSlaVencido(vencido);
        return tickets.save(t);
    }
}
