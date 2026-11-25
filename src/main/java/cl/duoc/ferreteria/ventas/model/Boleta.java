package cl.duoc.ferreteria.ventas.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Boleta (orden). usuario_id NO tiene foreign key: el usuario vive en ms-usuarios.
 * null significa compra de invitado.
 */
@Entity
@Table(name = "boleta")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nombre_cliente", nullable = false, length = 120)
    private String nombreCliente;

    @Column(name = "correo_cliente", nullable = false, length = 120)
    private String correoCliente;

    @Column(name = "rut_cliente", length = 12)
    private String rutCliente;

    @Column(nullable = false)
    private Long total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoBoleta estado = EstadoBoleta.RECIBIDA;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleBoleta> detalles = new ArrayList<>();

    public Boleta() { }

    /** Guardar la boleta guarda también sus detalles (cascade ALL), en la misma operación. */
    public void agregarDetalle(DetalleBoleta detalle) {
        detalle.setBoleta(this);
        detalles.add(detalle);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getCorreoCliente() { return correoCliente; }
    public void setCorreoCliente(String correoCliente) { this.correoCliente = correoCliente; }

    public String getRutCliente() { return rutCliente; }
    public void setRutCliente(String rutCliente) { this.rutCliente = rutCliente; }

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }

    public EstadoBoleta getEstado() { return estado; }
    public void setEstado(EstadoBoleta estado) { this.estado = estado; }

    public List<DetalleBoleta> getDetalles() { return detalles; }
}
