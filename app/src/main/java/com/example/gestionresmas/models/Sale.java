package com.example.gestionresmas.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Sale implements Serializable {
    public static final String ESTADO_PAGADO = "PAGADO";
    public static final String ESTADO_DEBE = "DEBE";

    private long id;
    private long clienteId;
    private String fecha;
    private double totalVenta;
    private double montoPagado;
    private String estadoPago; // "PAGADO" o "DEBE"

    // Campos auxiliares para visualización y relaciones
    private String clienteNombre;
    private String clienteTelefono;
    private List<SaleDetail> detalles;

    public Sale() {
        this.detalles = new ArrayList<>();
    }

    public Sale(long id, long clienteId, String fecha, double totalVenta, double montoPagado, String estadoPago) {
        this.id = id;
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.totalVenta = totalVenta;
        this.montoPagado = montoPagado;
        this.estadoPago = estadoPago;
        this.detalles = new ArrayList<>();
    }

    public Sale(long id, long clienteId, String fecha, double totalVenta, String estadoPago) {
        this.id = id;
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.totalVenta = totalVenta;
        this.montoPagado = ESTADO_PAGADO.equalsIgnoreCase(estadoPago) ? totalVenta : 0.0;
        this.estadoPago = estadoPago;
        this.detalles = new ArrayList<>();
    }

    public Sale(long clienteId, String fecha, double totalVenta, String estadoPago) {
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.totalVenta = totalVenta;
        this.montoPagado = ESTADO_PAGADO.equalsIgnoreCase(estadoPago) ? totalVenta : 0.0;
        this.estadoPago = estadoPago;
        this.detalles = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClienteId() {
        return clienteId;
    }

    public void setClienteId(long clienteId) {
        this.clienteId = clienteId;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getTotalVenta() {
        return totalVenta;
    }

    public void setTotalVenta(double totalVenta) {
        this.totalVenta = totalVenta;
    }

    public double getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(double montoPagado) {
        this.montoPagado = montoPagado;
    }

    public double getSaldoPendiente() {
        if (isPagado()) return 0.0;
        return Math.max(0.0, totalVenta - montoPagado);
    }

    public boolean tieneAbonosParciales() {
        return isDebe() && montoPagado > 0.001;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public boolean isPagado() {
        return ESTADO_PAGADO.equalsIgnoreCase(estadoPago);
    }

    public boolean isDebe() {
        return ESTADO_DEBE.equalsIgnoreCase(estadoPago);
    }

    public String getClienteNombre() {
        return clienteNombre != null ? clienteNombre : "";
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteTelefono() {
        return clienteTelefono != null ? clienteTelefono : "";
    }

    public void setClienteTelefono(String clienteTelefono) {
        this.clienteTelefono = clienteTelefono;
    }

    public List<SaleDetail> getDetalles() {
        if (detalles == null) {
            detalles = new ArrayList<>();
        }
        return detalles;
    }

    public void setDetalles(List<SaleDetail> detalles) {
        this.detalles = detalles;
    }
}
