package com.example.gestionresmas.models;

import java.io.Serializable;

public class Payment implements Serializable {
    private long id;
    private long clienteId;
    private long ventaId;
    private String fecha;
    private double monto;
    private String nota;

    public Payment() {
    }

    public Payment(long id, long clienteId, long ventaId, String fecha, double monto, String nota) {
        this.id = id;
        this.clienteId = clienteId;
        this.ventaId = ventaId;
        this.fecha = fecha;
        this.monto = monto;
        this.nota = nota;
    }

    public Payment(long clienteId, long ventaId, String fecha, double monto, String nota) {
        this.clienteId = clienteId;
        this.ventaId = ventaId;
        this.fecha = fecha;
        this.monto = monto;
        this.nota = nota;
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

    public long getVentaId() {
        return ventaId;
    }

    public void setVentaId(long ventaId) {
        this.ventaId = ventaId;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getNota() {
        return nota != null ? nota : "";
    }

    public void setNota(String nota) {
        this.nota = nota;
    }
}
