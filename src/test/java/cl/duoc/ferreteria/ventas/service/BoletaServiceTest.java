package cl.duoc.ferreteria.ventas.service;

import cl.duoc.ferreteria.ventas.client.CatalogoClient;
import cl.duoc.ferreteria.ventas.dto.BoletaRespuesta;
import cl.duoc.ferreteria.ventas.dto.CompraEntrada;
import cl.duoc.ferreteria.ventas.dto.CompradorEntrada;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de las reglas de negocio: el repository y CatalogoClient son
 * simulados con Mockito, sin base de datos ni red.
 */
@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock
    private BoletaRepository boletaRepository;

    @Mock
    private CatalogoClient catalogoClient;

    @InjectMocks
    private BoletaService servicio;

    private CompraEntrada compraInvitada(String nombre, String correo, String rut, ItemCompraEntrada... items) {
        CompradorEntrada comprador = (nombre == null && correo == null && rut == null)
                ? null : new CompradorEntrada(nombre, correo, rut);
        return new CompraEntrada(comprador, List.of(items));
    }

    private UsuarioAutenticado usuario(String rol) {
        return new UsuarioAutenticado(7L, "prueba@ferreteria.cl", "Usuario Prueba", rol);
    }

    private Boleta boleta(EstadoBoleta estado) {
        Boleta b = new Boleta();
        b.setEstado(estado);
        DetalleBoleta d = new DetalleBoleta();
        d.setProductoCodigo("MC001");
        d.setNombreProducto("Cemento Polpaico gris 25 kg");
        d.setPrecioUnitario(5990);
        d.setCantidad(2);
        d.setSubtotal(11980L);
        b.agregarDetalle(d);
        return b;
    }

    @Test
    void invitado_TotalCalculadoConLosPreciosDelCatalogo() {
        when(catalogoClient.descontarStock(any())).thenReturn(List.of(
                new ItemDescontado("MC001", "Cemento Polpaico gris 25 kg", 5990, 2)));

        BoletaRespuesta r = servicio.registrarCompra(
                compraInvitada("Juan Pérez", "Juan@Correo.cl", null, new ItemCompraEntrada("MC001", 2)), null);

        assertEquals(11980L, r.total());                       // precio del catálogo, no del front
        assertEquals("Juan Pérez", r.nombreCliente());
        assertEquals("juan@correo.cl", r.correoCliente());    // en minúsculas
        assertNull(r.usuarioId());                           // invitado
        assertNull(r.rutCliente());
        assertEquals(EstadoBoleta.RECIBIDA, r.estado());
        assertEquals(1, r.items().size());
        assertEquals(11980L, r.items().get(0).subtotal());

        ArgumentCaptor<Boleta> boletaCaptor = ArgumentCaptor.forClass(Boleta.class);
        verify(boletaRepository).saveAndFlush(boletaCaptor.capture());
        assertEquals(11980L, boletaCaptor.getValue().getTotal());
        assertEquals("Cemento Polpaico gris 25 kg",      // copia del nombre al momento de la compra
                boletaCaptor.getValue().getDetalles().get(0).getNombreProducto());
    }

    @Test
    @SuppressWarnings("unchecked")
    void codigosRepetidosSeSumanAntesDeLlamarAlCatalogo() {
        when(catalogoClient.descontarStock(any())).thenReturn(List.of(
                new ItemDescontado("MC001", "Cemento Polpaico gris 25 kg", 5990, 5)));

        servicio.registrarCompra(compraInvitada("Juan Pérez", "juan@correo.cl", null,
                new ItemCompraEntrada("MC001", 2), new ItemCompraEntrada("mc001", 3)), null);

        ArgumentCaptor<List<ItemStock>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(catalogoClient).descontarStock(itemsCaptor.capture());
        assertEquals(1, itemsCaptor.getValue().size());       // un solo ítem agrupado
        assertEquals(new ItemStock("MC001", 5), itemsCaptor.getValue().get(0));   // 2 + 3
        verify(boletaRepository).saveAndFlush(any(Boleta.class));
    }

    @Test
    void clienteConSesion_UsaDatosDelTokenEIgnoraComprador() {
        when(catalogoClient.descontarStock(any())).thenReturn(List.of(
                new ItemDescontado("MC001", "Cemento Polpaico gris 25 kg", 5990, 1)));

        BoletaRespuesta r = servicio.registrarCompra(
                new CompraEntrada(new CompradorEntrada("Otro Nombre", "otro@correo.cl", "12345678-5"),
                        List.of(new ItemCompraEntrada("MC001", 1))),
                usuario("CLIENTE"));

        assertEquals(7L, r.usuarioId());                       // sub del token
        assertEquals("Usuario Prueba", r.nombreCliente());    // claim nombre
        assertEquals("prueba@ferreteria.cl", r.correoCliente());
        assertNull(r.rutCliente());                           // C9.10: el token no trae RUT
    }

    @Test
    void vendedorNoPuedeComprar() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> servicio.registrarCompra(
                        new CompraEntrada(null, List.of(new ItemCompraEntrada("MC001", 1))),
                        usuario("VENDEDOR")));

        assertEquals(403, ex.getStatus().value());
        verify(catalogoClient, never()).descontarStock(any());   // el 403 nunca toca el stock
        verify(boletaRepository, never()).saveAndFlush(any());
    }

    @Test
    void invitadoSinCorreo_Da400SinLlamarAlCatalogo() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrarCompra(compraInvitada("Juan Pérez", null, null,
                        new ItemCompraEntrada("MC001", 1)), null));

        assertEquals("comprador.correo", ex.getDetalles().keySet().iterator().next());
        verify(catalogoClient, never()).descontarStock(any());
    }

    @Test
    void invitadoConRutInvalido_Da400SinLlamarAlCatalogo() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrarCompra(compraInvitada("Juan Pérez", "juan@correo.cl",
                        "12345678-9", new ItemCompraEntrada("MC001", 1)), null));

        assertEquals("comprador.rut", ex.getDetalles().keySet().iterator().next());
        assertEquals("RUT inválido", ex.getMessage());
        verify(catalogoClient, never()).descontarStock(any());
    }

    @Test
    void error409DelCatalogo_SePropagaConSuMensaje() {
        when(catalogoClient.descontarStock(any()))
                .thenThrow(new ConflictoException("Stock insuficiente para HE004: solicitado 5, disponible 4"));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> servicio.registrarCompra(compraInvitada("Juan Pérez", "juan@correo.cl", null,
                        new ItemCompraEntrada("HE004", 5)), null));

        assertEquals(409, ex.getStatus().value());
        assertEquals("Stock insuficiente para HE004: solicitado 5, disponible 4", ex.getMessage());
        verify(boletaRepository, never()).saveAndFlush(any());   // no se guarda nada
        verify(catalogoClient, never()).reponerStock(any());     // y no hubo nada que compensar
    }

    @Test
    void falloAlGuardar_CompensaReponeElStockYLanza500() {
        when(catalogoClient.descontarStock(any())).thenReturn(List.of(
                new ItemDescontado("MC001", "Cemento Polpaico gris 25 kg", 5990, 1)));
        when(boletaRepository.saveAndFlush(any(Boleta.class)))
                .thenThrow(new RuntimeException("falló la BD"));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> servicio.registrarCompra(compraInvitada("Juan Pérez", "juan@correo.cl", null,
                        new ItemCompraEntrada("MC001", 1)), null));

        assertEquals(500, ex.getStatus().value());
        verify(catalogoClient).reponerStock(List.of(new ItemStock("MC001", 1)));   // compensación
    }

    @Test
    void anular_ReponeElStockYPersistaAnulada() {
        Boleta b = boleta(EstadoBoleta.RECIBIDA);
        when(boletaRepository.findWithDetallesById(1L)).thenReturn(Optional.of(b));
        when(boletaRepository.save(any(Boleta.class))).thenReturn(b);

        BoletaRespuesta r = servicio.cambiarEstado(1L, EstadoBoleta.ANULADA);

        assertEquals(EstadoBoleta.ANULADA, r.estado());
        verify(catalogoClient).reponerStock(List.of(new ItemStock("MC001", 2)));
    }

    @Test
    void anularUnaEntregada_Da409SinReponer() {
        when(boletaRepository.findWithDetallesById(1L))
                .thenReturn(Optional.of(boleta(EstadoBoleta.ENTREGADA)));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> servicio.cambiarEstado(1L, EstadoBoleta.ANULADA));

        assertEquals("No se puede cambiar de ENTREGADA a ANULADA", ex.getMessage());
        verify(catalogoClient, never()).reponerStock(any());
        verify(boletaRepository, never()).save(any(Boleta.class));
    }

    @Test
    void eliminarUnaBoletaNoAnulada_ReponeElStock() {
        when(boletaRepository.findWithDetallesById(1L))
                .thenReturn(Optional.of(boleta(EstadoBoleta.RECIBIDA)));

        servicio.eliminar(1L);

        verify(catalogoClient).reponerStock(List.of(new ItemStock("MC001", 2)));
        verify(boletaRepository).delete(any(Boleta.class));
    }

    @Test
    void eliminarUnaBoletaAnulada_NoReponeOtraVez() {
        when(boletaRepository.findWithDetallesById(1L))
                .thenReturn(Optional.of(boleta(EstadoBoleta.ANULADA)));

        servicio.eliminar(1L);

        verify(catalogoClient, never()).reponerStock(any());   // ya se repuso al anular
        verify(boletaRepository).delete(any(Boleta.class));
    }

    @Test
    void boletaInexistente_Da404() {
        when(boletaRepository.findWithDetallesById(9L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.obtener(9L));
    }
}
