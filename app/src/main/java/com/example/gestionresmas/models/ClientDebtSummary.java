package com.example.gestionresmas.models;

import java.io.Serializable;

public class ClientDebtSummary implements Serializable {
    private long clienteId;
    private String clienteNombre;
    private String clienteTelefono;
    private double totalDeuda;
    private double totalAbonado;
    private int cantidadVentasDebe;

    public ClientDebtSummary() {
    }

    public ClientDebtSummary(long clienteId, String clienteNombre, String clienteTelefono,
                             double totalDeuda, int cantidadVentasDebe) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.totalDeuda = totalDeuda;
        this.totalAbonado = 0.0;
        this.cantidadVentasDebe = cantidadVentasDebe;
    }

    public ClientDebtSummary(long clienteId, String clienteNombre, String clienteTelefono,
                             double totalDeuda, double totalAbonado, int cantidadVentasDebe) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.totalDeuda = totalDeuda;
        this.totalAbonado = totalAbonado;
        this.cantidadVentasDebe = cantidadVentasDebe;
    }

    public long getClienteId() {
        return clienteId;
    }

    public void setClienteId(long clienteId) {
        this.clienteId = clienteId;
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

    public double getTotalDeuda() {
        return totalDeuda;
    }

    public void setTotalDeuda(double totalDeuda) {
        this.totalDeuda = totalDeuda;
    }

    public double getTotalAbonado() {
        return totalAbonado;
    }

    public void setTotalAbonado(double totalAbonado) {
        this.totalAbonado = totalAbonado;
    }

    public int getCantidadVentasDebe() {
        return cantidadVentasDebe;
    }

    public void setCantidadVentasDebe(int cantidadVentasDebe) {
        this.cantidadVentasDebe = cantidadVentasDebe;
    }
}
