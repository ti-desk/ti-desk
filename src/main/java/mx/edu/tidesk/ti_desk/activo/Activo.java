package mx.edu.tidesk.ti_desk.activo;

import jakarta.persistence.*;
import java.time.LocalDate;
import mx.edu.tidesk.ti_desk.usuario.Usuario;

@Entity
@Table(name = "activo")
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etiqueta_inventario", nullable = false, length = 40, unique = true)
    private String etiquetaInventario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoActivo tipo;

    @Column(length = 80)
    private String marca;

    @Column(length = 80)
    private String modelo;

    @Column(name = "numero_serie", length = 80)
    private String numeroSerie;

    @Column(length = 160)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoActivo estado = EstadoActivo.ACTIVO;

    @Column(name = "fecha_adquisicion")
    private LocalDate fechaAdquisicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEtiquetaInventario() {
        return etiquetaInventario;
    }

    public void setEtiquetaInventario(String etiquetaInventario) {
        this.etiquetaInventario = etiquetaInventario;
    }

    public TipoActivo getTipo() {
        return tipo;
    }

    public void setTipo(TipoActivo tipo) {
        this.tipo = tipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public EstadoActivo getEstado() {
        return estado;
    }

    public void setEstado(EstadoActivo estado) {
        this.estado = estado;
    }

    public LocalDate getFechaAdquisicion() {
        return fechaAdquisicion;
    }

    public void setFechaAdquisicion(LocalDate fechaAdquisicion) {
        this.fechaAdquisicion = fechaAdquisicion;
    }

    public Usuario getResponsable() {
        return responsable;
    }

    public void setResponsable(Usuario responsable) {
        this.responsable = responsable;
    }

    
}