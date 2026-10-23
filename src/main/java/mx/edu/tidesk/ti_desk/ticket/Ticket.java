package mx.edu.tidesk.ti_desk.ticket;

import jakarta.persistence.*;
import java.time.Instant;
import mx.edu.tidesk.ti_desk.activo.Activo;
import mx.edu.tidesk.ti_desk.usuario.Usuario;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String folio;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTicket estado = EstadoTicket.ABIERTO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activo_id")
    private Activo activo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "asignado_en")
    private Instant asignadoEn;

    @Column(name = "primera_respuesta_en")
    private Instant primeraRespuestaEn;

    @Column(name = "resuelto_en")
    private Instant resueltoEn;

    @Column(name = "cerrado_en")
    private Instant cerradoEn;

    @Column(name = "fecha_limite_sla", nullable = false)
    private Instant fechaLimiteSla;

    @Column(name = "sla_vencido", nullable = false)
    private boolean slaVencido;

    @Column(name = "sla_en_riesgo", nullable = false)
    private boolean slaEnRiesgo;

    @Version
    @Column(nullable = false)
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public EstadoTicket getEstado() {
        return estado;
    }

    public void setEstado(EstadoTicket estado) {
        this.estado = estado;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
    }

    public Usuario getTecnico() {
        return tecnico;
    }

    public void setTecnico(Usuario tecnico) {
        this.tecnico = tecnico;
    }

    public Activo getActivo() {
        return activo;
    }

    public void setActivo(Activo activo) {
        this.activo = activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getAsignadoEn() {
        return asignadoEn;
    }

    public void setAsignadoEn(Instant asignadoEn) {
        this.asignadoEn = asignadoEn;
    }

    public Instant getPrimeraRespuestaEn() {
        return primeraRespuestaEn;
    }

    public void setPrimeraRespuestaEn(Instant primeraRespuestaEn) {
        this.primeraRespuestaEn = primeraRespuestaEn;
    }

    public Instant getResueltoEn() {
        return resueltoEn;
    }

    public void setResueltoEn(Instant resueltoEn) {
        this.resueltoEn = resueltoEn;
    }

    public Instant getCerradoEn() {
        return cerradoEn;
    }

    public void setCerradoEn(Instant cerradoEn) {
        this.cerradoEn = cerradoEn;
    }

    public Instant getFechaLimiteSla() {
        return fechaLimiteSla;
    }

    public void setFechaLimiteSla(Instant fechaLimiteSla) {
        this.fechaLimiteSla = fechaLimiteSla;
    }

    public boolean isSlaVencido() {
        return slaVencido;
    }

    public void setSlaVencido(boolean slaVencido) {
        this.slaVencido = slaVencido;
    }

    public boolean isSlaEnRiesgo() {
        return slaEnRiesgo;
    }

    public void setSlaEnRiesgo(boolean slaEnRiesgo) {
        this.slaEnRiesgo = slaEnRiesgo;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    
}