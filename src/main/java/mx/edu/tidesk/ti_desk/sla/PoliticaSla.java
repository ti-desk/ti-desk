package mx.edu.tidesk.ti_desk.sla;

import jakarta.persistence.*;
import java.math.BigDecimal;
import mx.edu.tidesk.ti_desk.ticket.Prioridad;

@Entity
@Table(name = "politica_sla")
public class PoliticaSla {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Prioridad prioridad;

    @Column(name = "horas_primera_respuesta", nullable = false, precision = 6, scale = 2)
    private BigDecimal horasPrimeraRespuesta;

    @Column(name = "horas_resolucion", nullable = false, precision = 6, scale = 2)
    private BigDecimal horasResolucion;

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public BigDecimal getHorasPrimeraRespuesta() {
        return horasPrimeraRespuesta;
    }

    public void setHorasPrimeraRespuesta(BigDecimal horasPrimeraRespuesta) {
        this.horasPrimeraRespuesta = horasPrimeraRespuesta;
    }

    public BigDecimal getHorasResolucion() {
        return horasResolucion;
    }

    public void setHorasResolucion(BigDecimal horasResolucion) {
        this.horasResolucion = horasResolucion;
    }

    
}