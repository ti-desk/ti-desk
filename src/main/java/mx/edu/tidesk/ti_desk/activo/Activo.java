package mx.edu.tidesk.ti_desk.activo;

import jakarta.persistence.*;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import java.time.LocalDate;

@Entity
@Table(name = "activo",
       indexes = @Index(name = "idx_activo_etiqueta", columnList = "etiqueta_inventario"))
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etiqueta_inventario", nullable = false, unique = true, length = 50)
    private String etiquetaInventario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoActivo tipo;

    @Column(length = 80)  private String marca;
    @Column(length = 80)  private String modelo;

    @Column(name = "numero_serie", length = 80)
    private String numeroSerie;

    @Column(length = 120) private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoActivo estado = EstadoActivo.ACTIVO;

    @Column(name = "fecha_adquisicion")
    private LocalDate fechaAdquisicion;

    // N:1 con usuario (responsable). LAZY para evitar consultas innecesarias (N+1).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    public Long getId() { return id; }
    public String getEtiquetaInventario() { return etiquetaInventario; }
    public void setEtiquetaInventario(String v) { this.etiquetaInventario = v; }
    public TipoActivo getTipo() { return tipo; }
    public void setTipo(TipoActivo v) { this.tipo = v; }
    public String getMarca() { return marca; }
    public void setMarca(String v) { this.marca = v; }
    public String getModelo() { return modelo; }
    public void setModelo(String v) { this.modelo = v; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String v) { this.numeroSerie = v; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String v) { this.ubicacion = v; }
    public EstadoActivo getEstado() { return estado; }
    public void setEstado(EstadoActivo v) { this.estado = v; }
    public LocalDate getFechaAdquisicion() { return fechaAdquisicion; }
    public void setFechaAdquisicion(LocalDate v) { this.fechaAdquisicion = v; }
    public Usuario getResponsable() { return responsable; }
    public void setResponsable(Usuario v) { this.responsable = v; }
}