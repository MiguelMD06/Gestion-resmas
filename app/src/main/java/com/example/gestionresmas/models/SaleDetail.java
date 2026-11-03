package com.example.gestionresmas.models;

import java.io.Serializable;

public class SaleDetail implements Serializable {
    private long id;
    private long ventaId;
    private long cajaId;
    private int cantidad;
    private double precioUnitarioAplicado;
    private double subtotal;

    // Campos auxiliares para visualización
    private String tipoResma;
    private double costoUnitarioCaja;
    private double margenAplicado;

    public SaleDetail() {
    }

    public SaleDetail(long id, long ventaId, long cajaId, int cantidad, double precioUnitarioAplicado, double subtotal) {
        this.id = id;
        this.ventaId = ventaId;
        this.cajaId = cajaId;
        this.cantidad = cantidad;
        this.precioUnitarioAplicado = precioUnitarioAplicado;
        this.subtotal = subtotal;
    }

    public SaleDetail(long cajaId, int cantidad, double precioUnitarioAplicado) {
        this.cajaId = cajaId;
        this.cantidad = cantidad;
        this.precioUnitarioAplicado = precioUnitarioAplicado;
        this.subtotal = cantidad * precioUnitarioAplicado;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getVentaId() {
        return ventaId;
    }

    public void setVentaId(long ventaId) {
        this.ventaId = ventaId;
    }

    public long getCajaId() {
        return cajaId;
    }

    public void setCajaId(long cajaId) {
        this.cajaId = cajaId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        this.subtotal = this.cantidad * this.precioUnitarioAplicado;
    }

    public double getPrecioUnitarioAplicado() {
        return precioUnitarioAplicado;
    }

    public void setPrecioUnitarioAplicado(double precioUnitarioAplicado) {
        this.precioUnitarioAplicado = precioUnitarioAplicado;
        this.subtotal = this.cantidad * this.precioUnitarioAplicado;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public String getTipoResma() {
        return tipoResma != null ? tipoResma : "";
    }

    public void setTipoResma(String tipoResma) {
        this.tipoResma = tipoResma;
    }

    public double getCostoUnitarioCaja() {
        return costoUnitarioCaja;
    }

    public void setCostoUnitarioCaja(double costoUnitarioCaja) {
        this.costoUnitarioCaja = costoUnitarioCaja;
    }

    public double getMargenAplicado() {
        return getPorcentajeMargen();
    }

    public void setMargenAplicado(double margenAplicado) {
        this.margenAplicado = margenAplicado;
    }

    /**
     * Calcula la utilidad unitaria con respecto al costo de la caja:
     * Utilidad Unitaria = Precio de Venta Unitario - Costo Unitario Caja
     */
    public double getUtilidadUnitaria() {
        return precioUnitarioAplicado - costoUnitarioCaja;
    }

    /**
     * Calcula la utilidad total del detalle:
     * Utilidad Total = Utilidad Unitaria * Cantidad
     */
    public double getUtilidadTotal() {
        return getUtilidadUnitaria() * cantidad;
    }

    /**
     * Calcula el porcentaje de margen con respecto al costo de la caja:
     * % Margen = ((Precio de Venta Unitario - Costo Unitario Caja) / Costo Unitario Caja) * 100
     */
    public double getPorcentajeMargen() {
        if (costoUnitarioCaja <= 0) return 0.0;
        return ((precioUnitarioAplicado - costoUnitarioCaja) / costoUnitarioCaja) * 100.0;
    }

    // Métodos estáticos de cálculo auxiliares
    public static double calcularUtilidadUnitaria(double precioVenta, double costoUnitario) {
        return precioVenta - costoUnitario;
    }

    public static double calcularPorcentajeMargen(double precioVenta, double costoUnitario) {
        if (costoUnitario <= 0) return 0.0;
        return ((precioVenta - costoUnitario) / costoUnitario) * 100.0;
    }

    public static double calcularPrecioVentaDesdeMargen(double costoUnitario, double porcentajeMargen) {
        return costoUnitario * (1.0 + (porcentajeMargen / 100.0));
    }

    public static double calcularPrecioVentaDesdeUtilidad(double costoUnitario, double utilidadUnitaria) {
        return costoUnitario + utilidadUnitaria;
    }
}
