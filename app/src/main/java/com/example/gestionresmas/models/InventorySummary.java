package com.example.gestionresmas.models;

import java.io.Serializable;

public class InventorySummary implements Serializable {
    private int stockCarta;
    private int stockOficio;
    private double valorTotalInventario; // Suma de (resmas_disponibles * precio_costo_unitario)
    private double totalCuentasPorCobrar; // Suma de ventas con estado DEBE
    private double totalVentasCobrado; // Suma de ventas con estado PAGADO
    private int totalCajasActivas;

    public InventorySummary() {
    }

    public int getStockCarta() {
        return stockCarta;
    }

    public void setStockCarta(int stockCarta) {
        this.stockCarta = stockCarta;
    }

    public int getStockOficio() {
        return stockOficio;
    }

    public void setStockOficio(int stockOficio) {
        this.stockOficio = stockOficio;
    }

    public int getStockTotal() {
        return stockCarta + stockOficio;
    }

    public double getValorTotalInventario() {
        return valorTotalInventario;
    }

    public void setValorTotalInventario(double valorTotalInventario) {
        this.valorTotalInventario = valorTotalInventario;
    }

    public double getTotalCuentasPorCobrar() {
        return totalCuentasPorCobrar;
    }

    public void setTotalCuentasPorCobrar(double totalCuentasPorCobrar) {
        this.totalCuentasPorCobrar = totalCuentasPorCobrar;
    }

    public double getTotalVentasCobrado() {
        return totalVentasCobrado;
    }

    public void setTotalVentasCobrado(double totalVentasCobrado) {
        this.totalVentasCobrado = totalVentasCobrado;
    }

    public int getTotalCajasActivas() {
        return totalCajasActivas;
    }

    public void setTotalCajasActivas(int totalCajasActivas) {
        this.totalCajasActivas = totalCajasActivas;
    }
}
