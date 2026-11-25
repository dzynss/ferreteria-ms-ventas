package cl.duoc.ferreteria.ventas.service;

import cl.duoc.ferreteria.ventas.client.CatalogoClient;
import cl.duoc.ferreteria.ventas.dto.BoletaRespuesta;
import cl.duoc.ferreteria.ventas.dto.CompraEntrada;
import cl.duoc.ferreteria.ventas.dto.ItemCompraEntrada;
import cl.duoc.ferreteria.ventas.dto.ItemDescontado;
import cl.duoc.ferreteria.ventas.dto.ItemStock;
import cl.duoc.ferreteria.ventas.exception.AccesoDenegadoException;
import cl.duoc.ferreteria.ventas.exception.ConflictoException;
import cl.duoc.ferreteria.ventas.exception.NegocioException;
import cl.duoc.ferreteria.ventas.exception.NoEncontradoException;
import cl.duoc.ferreteria.ventas.exception.SolicitudInvalidaException;
import cl.duoc.ferreteria.ventas.model.Boleta;
import cl.duoc.ferreteria.ventas.model.DetalleBoleta;
import cl.duoc.ferreteria.ventas.model.EstadoBoleta;
import cl.duoc.ferreteria.ventas.repository.BoletaRepository;
import cl.duoc.ferreteria.ventas.security.UsuarioAutenticado;
import cl.duoc.ferreteria.ventas.util.ValidadorRut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BoletaService {

    private static final Logger log = LoggerFactory.getLogger(BoletaService.class);

    /** Explícito: dentro de Docker la JVM corre en UTC si no se configura. */
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final BoletaRepository boletaRepository;
    private final CatalogoClient catalogoClient;

    public BoletaService(BoletaRepository boletaRepository, CatalogoClient catalogoClient) {
        this.boletaRepository = boletaRepository;
        this.catalogoClient = catalogoClient;
    }

    /**
     * SIN @Transactional a propósito (ver sección 5.1 de la especificación): con @Transactional
     * el commit ocurre al salir del método y un fallo ahí no pasaría por el catch de la
     * compensación, dejando el stock descontado sin boleta. Con saveAndFlush la escritura
     * termina dentro del try. Una transacción de BD no puede deshacer una llamada HTTP:
     * para eso está la compensación (saga simple).
     */
    public BoletaRespuesta registrarCompra(CompraEntrada entrada, UsuarioAutenticado usuario) {

        // 1. Agrupar ítems por código (se suman los repetidos) y validar el máximo
        Map<String, Integer> agrupados = new LinkedHashMap<>();
        for (ItemCompraEntrada item : entrada.items()) {
            String codigo = item.codigo().trim().toUpperCase();
            int cantidad = agrupados.merge(codigo, item.cantidad(), Integer::sum);
            if (cantidad > 999) {
                throw new SolicitudInvalidaException("items", "La cantidad máxima por producto es 999");
            }
        }
        List<ItemStock> items = agrupados.entrySet().stream()
                .map(e -> new ItemStock(e.getKey(), e.getValue()))
                .toList();

        // 2. Determinar el comprador. Los pasos 1 y 2 van ANTES del descuento:
        //    un 400 o 403 nunca toca el stock.
        Long usuarioId = null;
        String nombreCliente;
        String correoCliente;
        String rutCliente = null;
        if (usuario != null) {
            if (!"CLIENTE".equals(usuario.rol())) {
                throw new AccesoDenegadoException("Solo clientes o invitados pueden comprar");
            }
            usuarioId = usuario.id();
            nombreCliente = usuario.nombre();
            correoCliente = usuario.correo();
            // C9.10: el token no trae RUT, queda null
        } else {
            var comprador = entrada.comprador();
            if (comprador == null || comprador.nombre() == null || comprador.nombre().isBlank()) {
                throw new SolicitudInvalidaException("comprador.nombre", "El nombre del comprador es obligatorio");
            }
            if (comprador.correo() == null || comprador.correo().isBlank()) {
                throw new SolicitudInvalidaException("comprador.correo", "El correo del comprador es obligatorio");
            }
            nombreCliente = comprador.nombre().trim();
            correoCliente = comprador.correo().trim().toLowerCase();
            if (comprador.rut() != null && !comprador.rut().isBlank()) {
                if (!ValidadorRut.esValido(comprador.rut())) {
                    throw new SolicitudInvalidaException("comprador.rut", "RUT inválido");
                }
                rutCliente = ValidadorRut.normalizar(comprador.rut());
            }
        }

        // 3. Descontar stock en ms-catalogo (única fuente de precios y control de stock)
        List<ItemDescontado> descontados = catalogoClient.descontarStock(items);

        // 4. Calcular subtotales y total EN EL SERVIDOR con los precios devueltos
        Boleta boleta = new Boleta();
        boleta.setFecha(LocalDateTime.now(ZONA));
        boleta.setUsuarioId(usuarioId);
        boleta.setNombreCliente(nombreCliente);
        boleta.setCorreoCliente(correoCliente);
        boleta.setRutCliente(rutCliente);
        boleta.setEstado(EstadoBoleta.RECIBIDA);
        long total = 0;
        for (ItemDescontado d : descontados) {
            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setProductoCodigo(d.codigo());
            detalle.setNombreProducto(d.nombre());       // copia al momento de la compra
            detalle.setPrecioUnitario(d.precioUnitario()); // copia al momento de la compra
            detalle.setCantidad(d.cantidad());
            long subtotal = (long) d.precioUnitario() * d.cantidad();
            detalle.setSubtotal(subtotal);
            total += subtotal;
            boleta.agregarDetalle(detalle);
        }
        boleta.setTotal(total);

        // 5 y 6. Guardar boleta + detalles juntos (cascade); si falla, compensar el stock
        try {
            boletaRepository.saveAndFlush(boleta);
        } catch (RuntimeException e) {
            try {
                catalogoClient.reponerStock(items);
            } catch (RuntimeException ex) {
                log.error("Falló también la reposición de stock tras no poder guardar la boleta", ex);
            }
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo registrar la compra. El stock fue repuesto.");
        }

        return BoletaRespuesta.desde(boleta);
    }

    @Transactional(readOnly = true)
    public List<BoletaRespuesta> listar(EstadoBoleta estado) {
        List<Boleta> boletas = estado == null
                ? boletaRepository.findAllByOrderByFechaDesc()
                : boletaRepository.findByEstadoOrderByFechaDesc(estado);
        return boletas.stream().map(BoletaRespuesta::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<BoletaRespuesta> listarMias(UsuarioAutenticado usuario) {
        return boletaRepository.findByUsuarioIdOrderByFechaDesc(usuario.id())
                .stream().map(BoletaRespuesta::desde).toList();
    }

    @Transactional(readOnly = true)
    public BoletaRespuesta obtener(Long id) {
        return BoletaRespuesta.desde(buscar(id));
    }

    /**
     * Se repone el stock ANTES de guardar el estado: si ms-catalogo falla (503),
     * la boleta no queda marcada ANULADA con el stock sin devolver.
     */
    @Transactional
    public BoletaRespuesta cambiarEstado(Long id, EstadoBoleta nuevo) {
        Boleta boleta = buscar(id);
        if (!boleta.getEstado().puedeCambiarA(nuevo)) {
            throw new ConflictoException("No se puede cambiar de " + boleta.getEstado() + " a " + nuevo);
        }
        if (nuevo == EstadoBoleta.ANULADA) {
            catalogoClient.reponerStock(itemsDe(boleta));
        }
        boleta.setEstado(nuevo);
        return BoletaRespuesta.desde(boletaRepository.save(boleta));
    }

    @Transactional
    public void eliminar(Long id) {
        Boleta boleta = buscar(id);
        if (boleta.getEstado() != EstadoBoleta.ANULADA) {
            catalogoClient.reponerStock(itemsDe(boleta));
        }
        boletaRepository.delete(boleta);
    }

    private Boleta buscar(Long id) {
        return boletaRepository.findWithDetallesById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la boleta " + id));
    }

    private List<ItemStock> itemsDe(Boleta boleta) {
        return boleta.getDetalles().stream()
                .map(d -> new ItemStock(d.getProductoCodigo(), d.getCantidad()))
                .toList();
    }
}
