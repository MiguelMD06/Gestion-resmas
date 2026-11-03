package com.example.gestionresmas.models;

import java.io.Serializable;

public class BoxPurchase implements Serializable {
    private long id;
    private String tipoResma; // "CARTA" o "OFICIO"
    private String fechaCompra;
    private double precioCompraCaja;
    private int cantidadResmasTotal;
    private int resmasDisponibles;
    private double precioCostoUnitario;

    public BoxPurchase() {
    }

    public BoxPurchase(long id, String tipoResma, String fechaCompra, double precioCompraCaja,
                       int cantidadResmasTotal, int resmasDisponibles, double precioCostoUnitario) {
        this.id = id;
        this.tipoResma = tipoResma;
        this.fechaCompra = fechaCompra;
        this.precioCompraCaja = precioCompraCaja;
        this.cantidadResmasTotal = cantidadResmasTotal;
        this.resmasDisponibles = resmasDisponibles;
        this.precioCostoUnitario = precioCostoUnitario;
    }

    public BoxPurchase(String tipoResma, String fechaCompra, double precioCompraCaja,
                       int cantidadResmasTotal, int resmasDisponibles) {
        this.tipoResma = tipoResma;
        this.fechaCompra = fechaCompra;
        this.precioCompraCaja = precioCompraCaja;
        this.cantidadResmasTotal = cantidadResmasTotal;
        this.resmasDisponibles = resmasDisponibles;
        if (cantidadResmasTotal > 0) {
            this.precioCostoUnitario = precioCompraCaja / cantidadResmasTotal;
        } else {
            this.precioCostoUnitario = 0.0;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTipoResma() {
        return tipoResma;
    }

    public void setTipoResma(String tipoResma) {
        this.tipoResma = tipoResma;
    }

    public String getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(String fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public double getPrecioCompraCaja() {
        return precioCompraCaja;
    }

    public void setPrecioCompraCaja(double precioCompraCaja) {
        this.precioCompraCaja = precioCompraCaja;
        if (cantidadResmasTotal > 0) {
            this.precioCostoUnitario = precioCompraCaja / cantidadResmasTotal;
        }
    }

    public int getCantidadResmasTotal() {
        return cantidadResmasTotal;
    }

    public void setCantidadResmasTotal(int cantidadResmasTotal) {
        this.cantidadResmasTotal = cantidadResmasTotal;
        if (cantidadResmasTotal > 0) {
            this.precioCostoUnitario = precioCompraCaja / cantidadResmasTotal;
        }
    }

    public int getResmasDisponibles() {
        return resmasDisponibles;
    }

    public void setResmasDisponibles(int resmasDisponibles) {
        this.resmasDisponibles = resmasDisponibles;
    }

    public double getPrecioCostoUnitario() {
        return precioCostoUnitario;
    }

    public void setPrecioCostoUnitario(double precioCostoUnitario) {
        this.precioCostoUnitario = precioCostoUnitario;
    }

    /**
     * Calcula el precio unitario de venta aplicando el margen del cliente:
     * Precio Unitario = Costo Unitario * (1 + Margen% / 100)
     */
    public double calcularPrecioVenta(double margenUtilidadPorcentaje) {
        return precioCostoUnitario * (1.0 + (margenUtilidadPorcentaje / 100.0));
    }

    @Override
    public String toString() {
        return "Lote #" + id + " - " + tipoResma + " (Disp: " + resmasDisponibles + "/" + cantidadResmasTotal + ") - Costo: $" + String.format("%.2f", precioCostoUnitario);
    }
}
