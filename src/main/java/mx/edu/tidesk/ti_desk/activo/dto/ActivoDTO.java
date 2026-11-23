package mx.edu.tidesk.ti_desk.activo.dto;

import mx.edu.tidesk.ti_desk.activo.TipoActivo;
import mx.edu.tidesk.ti_desk.activo.EstadoActivo;

public class ActivoDTO {
    private String etiquetaInventario;
    private TipoActivo tipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String ubicacion;
    private EstadoActivo estado;

    // Getters y Setters (Genera los tuyos en VS Code o pégalos aquí)
    public String getEtiquetaInventario() { return etiquetaInventario; }
    public void setEtiquetaInventario(String etiquetaInventario) { this.etiquetaInventario = etiquetaInventario; }
    
    public TipoActivo getTipo() { return tipo; }
    public void setTipo(TipoActivo tipo) { this.tipo = tipo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public EstadoActivo getEstado() { return estado; }
    public void setEstado(EstadoActivo estado) { this.estado = estado; }
}