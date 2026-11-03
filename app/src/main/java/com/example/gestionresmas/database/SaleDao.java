package com.example.gestionresmas.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestionresmas.models.ClientDebtSummary;
import com.example.gestionresmas.models.Payment;
import com.example.gestionresmas.models.Sale;
import com.example.gestionresmas.models.SaleDetail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SaleDao {
    private final DatabaseHelper dbHelper;

    public SaleDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Registra una venta completa con sus detalles y descuenta el inventario de cada lote en una sola transacción atómica.
     * Si no hay suficiente stock en alguno de los lotes, revierte la transacción y lanza una excepción.
     */
    public long insertSaleWithDetails(Sale sale, List<SaleDetail> details) throws Exception {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Validar y descontar inventario en cada lote
            for (SaleDetail detail : details) {
                // Consultar stock actual del lote
                Cursor cursor = db.query(DatabaseHelper.TABLE_BOX_PURCHASES,
                        new String[]{DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES, DatabaseHelper.COL_BOX_TIPO_RESMA},
                        DatabaseHelper.COL_BOX_ID + " = ?",
                        new String[]{String.valueOf(detail.getCajaId())},
                        null, null, null);

                if (cursor == null || !cursor.moveToFirst()) {
                    if (cursor != null) cursor.close();
                    throw new Exception("El lote de caja #" + detail.getCajaId() + " no fue encontrado.");
                }

                int stockActual = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES));
                String tipo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_TIPO_RESMA));
                cursor.close();

                if (detail.getCantidad() > stockActual) {
                    throw new Exception("Stock insuficiente para " + tipo + " (Lote #" + detail.getCajaId() +
                            "). Solicitado: " + detail.getCantidad() + ", Disponible: " + stockActual);
                }

                // Descontar inventario
                int nuevoStock = stockActual - detail.getCantidad();
                ContentValues boxValues = new ContentValues();
                boxValues.put(DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES, nuevoStock);

                int rowsAffected = db.update(DatabaseHelper.TABLE_BOX_PURCHASES, boxValues,
                        DatabaseHelper.COL_BOX_ID + " = ?",
                        new String[]{String.valueOf(detail.getCajaId())});

                if (rowsAffected <= 0) {
                    throw new Exception("Error al actualizar el stock del lote #" + detail.getCajaId());
                }
            }

            // 2. Insertar la venta principal
            ContentValues saleValues = new ContentValues();
            saleValues.put(DatabaseHelper.COL_SALE_CLIENTE_ID, sale.getClienteId());
            saleValues.put(DatabaseHelper.COL_SALE_FECHA, sale.getFecha());
            saleValues.put(DatabaseHelper.COL_SALE_TOTAL_VENTA, sale.getTotalVenta());
            double montoPagadoInicial = sale.isPagado() ? sale.getTotalVenta() : sale.getMontoPagado();
            saleValues.put(DatabaseHelper.COL_SALE_MONTO_PAGADO, montoPagadoInicial);
            saleValues.put(DatabaseHelper.COL_SALE_ESTADO_PAGO, sale.getEstadoPago());

            long saleId = db.insert(DatabaseHelper.TABLE_SALES, null, saleValues);
            if (saleId == -1) {
                throw new Exception("Error al registrar la venta en la base de datos.");
            }

            // 3. Insertar cada detalle de venta
            for (SaleDetail detail : details) {
                ContentValues detailValues = new ContentValues();
                detailValues.put(DatabaseHelper.COL_DETAIL_VENTA_ID, saleId);
                detailValues.put(DatabaseHelper.COL_DETAIL_CAJA_ID, detail.getCajaId());
                detailValues.put(DatabaseHelper.COL_DETAIL_CANTIDAD, detail.getCantidad());
                detailValues.put(DatabaseHelper.COL_DETAIL_PRECIO_UNITARIO, detail.getPrecioUnitarioAplicado());
                detailValues.put(DatabaseHelper.COL_DETAIL_SUBTOTAL, detail.getSubtotal());

                long detailId = db.insert(DatabaseHelper.TABLE_SALE_DETAILS, null, detailValues);
                if (detailId == -1) {
                    throw new Exception("Error al insertar el detalle de la venta para el lote #" + detail.getCajaId());
                }
            }

            // Si todo fue correcto, marcamos la transacción como exitosa
            db.setTransactionSuccessful();
            return saleId;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Obtiene todas las ventas ordenadas por fecha descendente.
     */
    public List<Sale> getAllSales() {
        List<Sale> sales = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT s." + DatabaseHelper.COL_SALE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_CLIENTE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_FECHA + ", "
                + "s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + ", "
                + "s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + ", "
                + "s." + DatabaseHelper.COL_SALE_ESTADO_PAGO + ", "
                + "c." + DatabaseHelper.COL_CLIENT_NOMBRE + ", "
                + "c." + DatabaseHelper.COL_CLIENT_TELEFONO + " "
                + "FROM " + DatabaseHelper.TABLE_SALES + " s "
                + "LEFT JOIN " + DatabaseHelper.TABLE_CLIENTS + " c ON s." + DatabaseHelper.COL_SALE_CLIENTE_ID + " = c." + DatabaseHelper.COL_CLIENT_ID + " "
                + "ORDER BY s." + DatabaseHelper.COL_SALE_ID + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Sale sale = mapCursorToSale(cursor);
                sales.add(sale);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return sales;
    }

    /**
     * Obtiene el historial de ventas de un cliente específico.
     */
    public List<Sale> getSalesByClient(long clientId) {
        List<Sale> sales = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT s." + DatabaseHelper.COL_SALE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_CLIENTE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_FECHA + ", "
                + "s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + ", "
                + "s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + ", "
                + "s." + DatabaseHelper.COL_SALE_ESTADO_PAGO + ", "
                + "c." + DatabaseHelper.COL_CLIENT_NOMBRE + ", "
                + "c." + DatabaseHelper.COL_CLIENT_TELEFONO + " "
                + "FROM " + DatabaseHelper.TABLE_SALES + " s "
                + "LEFT JOIN " + DatabaseHelper.TABLE_CLIENTS + " c ON s." + DatabaseHelper.COL_SALE_CLIENTE_ID + " = c." + DatabaseHelper.COL_CLIENT_ID + " "
                + "WHERE s." + DatabaseHelper.COL_SALE_CLIENTE_ID + " = ? "
                + "ORDER BY s." + DatabaseHelper.COL_SALE_ID + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(clientId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                sales.add(mapCursorToSale(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return sales;
    }

    /**
     * Obtiene ventas pendientes de cobro (estado_pago = 'DEBE').
     */
    public List<Sale> getDebtSales() {
        List<Sale> sales = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT s." + DatabaseHelper.COL_SALE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_CLIENTE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_FECHA + ", "
                + "s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + ", "
                + "s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + ", "
                + "s." + DatabaseHelper.COL_SALE_ESTADO_PAGO + ", "
                + "c." + DatabaseHelper.COL_CLIENT_NOMBRE + ", "
                + "c." + DatabaseHelper.COL_CLIENT_TELEFONO + " "
                + "FROM " + DatabaseHelper.TABLE_SALES + " s "
                + "LEFT JOIN " + DatabaseHelper.TABLE_CLIENTS + " c ON s." + DatabaseHelper.COL_SALE_CLIENTE_ID + " = c." + DatabaseHelper.COL_CLIENT_ID + " "
                + "WHERE s." + DatabaseHelper.COL_SALE_ESTADO_PAGO + " = ? "
                + "ORDER BY s." + DatabaseHelper.COL_SALE_ID + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{Sale.ESTADO_DEBE});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                sales.add(mapCursorToSale(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return sales;
    }

    /**
     * Obtiene las ventas con estado 'DEBE' de un cliente en específico.
     */
    public List<Sale> getDebtSalesByClient(long clientId) {
        List<Sale> sales = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT s." + DatabaseHelper.COL_SALE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_CLIENTE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_FECHA + ", "
                + "s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + ", "
                + "s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + ", "
                + "s." + DatabaseHelper.COL_SALE_ESTADO_PAGO + ", "
                + "c." + DatabaseHelper.COL_CLIENT_NOMBRE + ", "
                + "c." + DatabaseHelper.COL_CLIENT_TELEFONO + " "
                + "FROM " + DatabaseHelper.TABLE_SALES + " s "
                + "LEFT JOIN " + DatabaseHelper.TABLE_CLIENTS + " c ON s." + DatabaseHelper.COL_SALE_CLIENTE_ID + " = c." + DatabaseHelper.COL_CLIENT_ID + " "
                + "WHERE s." + DatabaseHelper.COL_SALE_CLIENTE_ID + " = ? AND s." + DatabaseHelper.COL_SALE_ESTADO_PAGO + " = ? "
                + "ORDER BY s." + DatabaseHelper.COL_SALE_ID + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(clientId), Sale.ESTADO_DEBE});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                sales.add(mapCursorToSale(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return sales;
    }

    /**
     * Resumen de deudas agrupadas por cliente (Libro de Cuentas por Cobrar).
     */
    public List<ClientDebtSummary> getClientDebtSummaries() {
        List<ClientDebtSummary> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT c." + DatabaseHelper.COL_CLIENT_ID + ", "
                + "c." + DatabaseHelper.COL_CLIENT_NOMBRE + ", "
                + "c." + DatabaseHelper.COL_CLIENT_TELEFONO + ", "
                + "SUM(s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + " - s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + ") AS total_deuda, "
                + "SUM(s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + ") AS total_abonado, "
                + "COUNT(s." + DatabaseHelper.COL_SALE_ID + ") AS cantidad_ventas "
                + "FROM " + DatabaseHelper.TABLE_SALES + " s "
                + "INNER JOIN " + DatabaseHelper.TABLE_CLIENTS + " c ON s." + DatabaseHelper.COL_SALE_CLIENTE_ID + " = c." + DatabaseHelper.COL_CLIENT_ID + " "
                + "WHERE s." + DatabaseHelper.COL_SALE_ESTADO_PAGO + " = ? AND (s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + " - s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + ") > 0.001 "
                + "GROUP BY c." + DatabaseHelper.COL_CLIENT_ID + ", c." + DatabaseHelper.COL_CLIENT_NOMBRE + ", c." + DatabaseHelper.COL_CLIENT_TELEFONO + " "
                + "ORDER BY total_deuda DESC";

        Cursor cursor = db.rawQuery(query, new String[]{Sale.ESTADO_DEBE});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long clienteId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLIENT_ID));
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLIENT_NOMBRE));
                String tel = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLIENT_TELEFONO));
                double totalDeuda = cursor.getDouble(cursor.getColumnIndexOrThrow("total_deuda"));
                double totalAbonado = cursor.getDouble(cursor.getColumnIndexOrThrow("total_abonado"));
                int cantVentas = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad_ventas"));

                list.add(new ClientDebtSummary(clienteId, nombre, tel, totalDeuda, totalAbonado, cantVentas));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * Obtiene los detalles de una venta con el tipo de resma del lote asociado.
     */
    public List<SaleDetail> getSaleDetails(long saleId) {
        List<SaleDetail> details = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT d." + DatabaseHelper.COL_DETAIL_ID + ", "
                + "d." + DatabaseHelper.COL_DETAIL_VENTA_ID + ", "
                + "d." + DatabaseHelper.COL_DETAIL_CAJA_ID + ", "
                + "d." + DatabaseHelper.COL_DETAIL_CANTIDAD + ", "
                + "d." + DatabaseHelper.COL_DETAIL_PRECIO_UNITARIO + ", "
                + "d." + DatabaseHelper.COL_DETAIL_SUBTOTAL + ", "
                + "b." + DatabaseHelper.COL_BOX_TIPO_RESMA + ", "
                + "b." + DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO + " "
                + "FROM " + DatabaseHelper.TABLE_SALE_DETAILS + " d "
                + "LEFT JOIN " + DatabaseHelper.TABLE_BOX_PURCHASES + " b ON d." + DatabaseHelper.COL_DETAIL_CAJA_ID + " = b." + DatabaseHelper.COL_BOX_ID + " "
                + "WHERE d." + DatabaseHelper.COL_DETAIL_VENTA_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(saleId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_ID));
                long vId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_VENTA_ID));
                long cajaId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_CAJA_ID));
                int cant = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_CANTIDAD));
                double precioUnitario = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_PRECIO_UNITARIO));
                double subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_SUBTOTAL));

                SaleDetail detail = new SaleDetail(id, vId, cajaId, cant, precioUnitario, subtotal);
                detail.setTipoResma(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_TIPO_RESMA)));
                detail.setCostoUnitarioCaja(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO)));
                details.add(detail);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return details;
    }

    /**
     * Actualiza el estado de pago de una venta (ej. de DEBE a PAGADO).
     * Si pasa a PAGADO y aún tenía saldo pendiente, registra el pago restante en el historial de abonos.
     */
    public int updateSalePaymentStatus(long saleId, String nuevoEstado) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (Sale.ESTADO_PAGADO.equalsIgnoreCase(nuevoEstado)) {
            Sale currentSale = getSaleById(saleId);
            if (currentSale != null && currentSale.getSaldoPendiente() > 0.001) {
                try {
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    registerPaymentForSale(saleId, currentSale.getSaldoPendiente(), today, "Liquidación total de saldo");
                    return 1;
                } catch (Exception ignored) {
                }
            }
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SALE_ESTADO_PAGO, nuevoEstado);
        if (Sale.ESTADO_PAGADO.equalsIgnoreCase(nuevoEstado)) {
            Sale s = getSaleById(saleId);
            if (s != null) {
                values.put(DatabaseHelper.COL_SALE_MONTO_PAGADO, s.getTotalVenta());
            }
        }
        return db.update(DatabaseHelper.TABLE_SALES, values,
                DatabaseHelper.COL_SALE_ID + " = ?",
                new String[]{String.valueOf(saleId)});
    }

    public double getTotalDebtAmount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_SALE_TOTAL_VENTA + " - " + DatabaseHelper.COL_SALE_MONTO_PAGADO + ") " +
                "FROM " + DatabaseHelper.TABLE_SALES + " " +
                "WHERE " + DatabaseHelper.COL_SALE_ESTADO_PAGO + " = ?", new String[]{Sale.ESTADO_DEBE});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public double getClientDebtByClientId(long clientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_SALE_TOTAL_VENTA + " - " + DatabaseHelper.COL_SALE_MONTO_PAGADO + ") " +
                "FROM " + DatabaseHelper.TABLE_SALES + " " +
                "WHERE " + DatabaseHelper.COL_SALE_CLIENTE_ID + " = ? AND " + DatabaseHelper.COL_SALE_ESTADO_PAGO + " = ?",
                new String[]{String.valueOf(clientId), Sale.ESTADO_DEBE});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public double getTotalPaidAmount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_SALE_MONTO_PAGADO + ") " +
                "FROM " + DatabaseHelper.TABLE_SALES, null);
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    /**
     * Obtiene una venta específica por su ID, incluyendo nombre y teléfono del cliente.
     */
    public Sale getSaleById(long saleId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT s." + DatabaseHelper.COL_SALE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_CLIENTE_ID + ", "
                + "s." + DatabaseHelper.COL_SALE_FECHA + ", "
                + "s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + ", "
                + "s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + ", "
                + "s." + DatabaseHelper.COL_SALE_ESTADO_PAGO + ", "
                + "c." + DatabaseHelper.COL_CLIENT_NOMBRE + ", "
                + "c." + DatabaseHelper.COL_CLIENT_TELEFONO + " "
                + "FROM " + DatabaseHelper.TABLE_SALES + " s "
                + "LEFT JOIN " + DatabaseHelper.TABLE_CLIENTS + " c ON s." + DatabaseHelper.COL_SALE_CLIENTE_ID + " = c." + DatabaseHelper.COL_CLIENT_ID + " "
                + "WHERE s." + DatabaseHelper.COL_SALE_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(saleId)});
        Sale sale = null;
        if (cursor != null && cursor.moveToFirst()) {
            sale = mapCursorToSale(cursor);
            cursor.close();
        }
        return sale;
    }

    /**
     * Actualiza una venta existente y sus detalles, revirtiendo el stock original y aplicando
     * el nuevo descuento de inventario en una única transacción atómica.
     */
    public boolean updateSaleWithDetails(Sale sale, List<SaleDetail> newDetails) throws Exception {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Obtener detalles previos para revertir inventario
            List<SaleDetail> oldDetails = new ArrayList<>();
            Cursor cursorOld = db.query(DatabaseHelper.TABLE_SALE_DETAILS, null,
                    DatabaseHelper.COL_DETAIL_VENTA_ID + " = ?",
                    new String[]{String.valueOf(sale.getId())},
                    null, null, null);

            if (cursorOld != null && cursorOld.moveToFirst()) {
                do {
                    long cajaId = cursorOld.getLong(cursorOld.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_CAJA_ID));
                    int cant = cursorOld.getInt(cursorOld.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_CANTIDAD));
                    double precio = cursorOld.getDouble(cursorOld.getColumnIndexOrThrow(DatabaseHelper.COL_DETAIL_PRECIO_UNITARIO));
                    oldDetails.add(new SaleDetail(cajaId, cant, precio));
                } while (cursorOld.moveToNext());
                cursorOld.close();
            }

            // 2. Revertir el stock a las cajas correspondientes
            for (SaleDetail oldD : oldDetails) {
                db.execSQL("UPDATE " + DatabaseHelper.TABLE_BOX_PURCHASES + " SET "
                        + DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + " = "
                        + DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + " + ? WHERE "
                        + DatabaseHelper.COL_BOX_ID + " = ?",
                        new Object[]{oldD.getCantidad(), oldD.getCajaId()});
            }

            // 3. Validar y descontar stock para los nuevos detalles
            for (SaleDetail newD : newDetails) {
                Cursor cBox = db.query(DatabaseHelper.TABLE_BOX_PURCHASES,
                        new String[]{DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES, DatabaseHelper.COL_BOX_TIPO_RESMA},
                        DatabaseHelper.COL_BOX_ID + " = ?",
                        new String[]{String.valueOf(newD.getCajaId())},
                        null, null, null);

                if (cBox == null || !cBox.moveToFirst()) {
                    if (cBox != null) cBox.close();
                    throw new Exception("El lote de caja #" + newD.getCajaId() + " no fue encontrado.");
                }

                int stockDisponible = cBox.getInt(cBox.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES));
                String tipo = cBox.getString(cBox.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_TIPO_RESMA));
                cBox.close();

                if (newD.getCantidad() > stockDisponible) {
                    throw new Exception("Stock insuficiente para " + tipo + " (Lote #" + newD.getCajaId() +
                            "). Solicitado: " + newD.getCantidad() + ", Disponible: " + stockDisponible);
                }

                int nuevoStock = stockDisponible - newD.getCantidad();
                ContentValues boxValues = new ContentValues();
                boxValues.put(DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES, nuevoStock);
                int rows = db.update(DatabaseHelper.TABLE_BOX_PURCHASES, boxValues,
                        DatabaseHelper.COL_BOX_ID + " = ?",
                        new String[]{String.valueOf(newD.getCajaId())});
                if (rows <= 0) {
                    throw new Exception("Error al actualizar el stock del lote #" + newD.getCajaId());
                }
            }

            // 4. Actualizar la cabecera de la venta manteniendo coherencia en monto_pagado
            double montoPagadoPrevio = 0.0;
            Cursor cursorPrev = db.query(DatabaseHelper.TABLE_SALES,
                    new String[]{DatabaseHelper.COL_SALE_MONTO_PAGADO},
                    DatabaseHelper.COL_SALE_ID + " = ?",
                    new String[]{String.valueOf(sale.getId())},
                    null, null, null);
            if (cursorPrev != null && cursorPrev.moveToFirst()) {
                montoPagadoPrevio = cursorPrev.getDouble(0);
                cursorPrev.close();
            }

            double montoPagadoFinal;
            String estadoPagoFinal = sale.getEstadoPago();
            if (sale.isPagado()) {
                montoPagadoFinal = sale.getTotalVenta();
            } else {
                if (montoPagadoPrevio >= sale.getTotalVenta() - 0.01) {
                    montoPagadoFinal = sale.getTotalVenta();
                    estadoPagoFinal = Sale.ESTADO_PAGADO;
                } else {
                    montoPagadoFinal = Math.max(0.0, montoPagadoPrevio);
                }
            }

            ContentValues saleValues = new ContentValues();
            saleValues.put(DatabaseHelper.COL_SALE_CLIENTE_ID, sale.getClienteId());
            saleValues.put(DatabaseHelper.COL_SALE_FECHA, sale.getFecha());
            saleValues.put(DatabaseHelper.COL_SALE_TOTAL_VENTA, sale.getTotalVenta());
            saleValues.put(DatabaseHelper.COL_SALE_MONTO_PAGADO, montoPagadoFinal);
            saleValues.put(DatabaseHelper.COL_SALE_ESTADO_PAGO, estadoPagoFinal);

            int updatedSaleRows = db.update(DatabaseHelper.TABLE_SALES, saleValues,
                    DatabaseHelper.COL_SALE_ID + " = ?",
                    new String[]{String.valueOf(sale.getId())});

            if (updatedSaleRows <= 0) {
                throw new Exception("No se pudo actualizar la venta #V-" + sale.getId());
            }

            // 5. Eliminar detalles anteriores e insertar los nuevos
            db.delete(DatabaseHelper.TABLE_SALE_DETAILS,
                    DatabaseHelper.COL_DETAIL_VENTA_ID + " = ?",
                    new String[]{String.valueOf(sale.getId())});

            for (SaleDetail detail : newDetails) {
                ContentValues detailValues = new ContentValues();
                detailValues.put(DatabaseHelper.COL_DETAIL_VENTA_ID, sale.getId());
                detailValues.put(DatabaseHelper.COL_DETAIL_CAJA_ID, detail.getCajaId());
                detailValues.put(DatabaseHelper.COL_DETAIL_CANTIDAD, detail.getCantidad());
                detailValues.put(DatabaseHelper.COL_DETAIL_PRECIO_UNITARIO, detail.getPrecioUnitarioAplicado());
                detailValues.put(DatabaseHelper.COL_DETAIL_SUBTOTAL, detail.getSubtotal());

                long detailId = db.insert(DatabaseHelper.TABLE_SALE_DETAILS, null, detailValues);
                if (detailId == -1) {
                    throw new Exception("Error al insertar el detalle de la venta para el lote #" + detail.getCajaId());
                }
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Calcula el Dinero Recuperado (Capital Base):
     * Suma del costo base de las resmas vendidas proporcionalmente al monto abonado/pagado de cada venta.
     */
    public double getRecoveredCapital() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM((d." + DatabaseHelper.COL_DETAIL_CANTIDAD + " * b." + DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO + ") * "
                + "(s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + " / s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + ")) "
                + "FROM " + DatabaseHelper.TABLE_SALE_DETAILS + " d "
                + "INNER JOIN " + DatabaseHelper.TABLE_SALES + " s ON d." + DatabaseHelper.COL_DETAIL_VENTA_ID + " = s." + DatabaseHelper.COL_SALE_ID + " "
                + "INNER JOIN " + DatabaseHelper.TABLE_BOX_PURCHASES + " b ON d." + DatabaseHelper.COL_DETAIL_CAJA_ID + " = b." + DatabaseHelper.COL_BOX_ID + " "
                + "WHERE s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + " > 0 AND s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + " > 0";

        Cursor cursor = db.rawQuery(query, null);
        double recovered = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            recovered = cursor.getDouble(0);
            cursor.close();
        }
        return recovered;
    }

    /**
     * Calcula la Ganancia Real Generada:
     * Suma de la utilidad neta proporcionalmente al monto abonado/pagado de cada venta.
     */
    public double getRealProfit() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM((d." + DatabaseHelper.COL_DETAIL_CANTIDAD + " * (d." + DatabaseHelper.COL_DETAIL_PRECIO_UNITARIO + " - b." + DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO + ")) * "
                + "(s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + " / s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + ")) "
                + "FROM " + DatabaseHelper.TABLE_SALE_DETAILS + " d "
                + "INNER JOIN " + DatabaseHelper.TABLE_SALES + " s ON d." + DatabaseHelper.COL_DETAIL_VENTA_ID + " = s." + DatabaseHelper.COL_SALE_ID + " "
                + "INNER JOIN " + DatabaseHelper.TABLE_BOX_PURCHASES + " b ON d." + DatabaseHelper.COL_DETAIL_CAJA_ID + " = b." + DatabaseHelper.COL_BOX_ID + " "
                + "WHERE s." + DatabaseHelper.COL_SALE_MONTO_PAGADO + " > 0 AND s." + DatabaseHelper.COL_SALE_TOTAL_VENTA + " > 0";

        Cursor cursor = db.rawQuery(query, null);
        double profit = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            profit = cursor.getDouble(0);
            cursor.close();
        }
        return profit;
    }

    /**
     * Registra un abono específico a una venta individual.
     */
    public boolean registerPaymentForSale(long saleId, double abonoMonto, String fecha, String nota) throws Exception {
        if (abonoMonto <= 0) {
            throw new Exception("El monto a abonar debe ser mayor a cero.");
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor cursor = db.query(DatabaseHelper.TABLE_SALES,
                    new String[]{DatabaseHelper.COL_SALE_CLIENTE_ID, DatabaseHelper.COL_SALE_TOTAL_VENTA,
                            DatabaseHelper.COL_SALE_MONTO_PAGADO, DatabaseHelper.COL_SALE_ESTADO_PAGO},
                    DatabaseHelper.COL_SALE_ID + " = ?",
                    new String[]{String.valueOf(saleId)},
                    null, null, null);

            if (cursor == null || !cursor.moveToFirst()) {
                if (cursor != null) cursor.close();
                throw new Exception("La venta #V-" + saleId + " no fue encontrada.");
            }

            long clienteId = cursor.getLong(0);
            double totalVenta = cursor.getDouble(1);
            double montoPagadoActual = cursor.getDouble(2);
            cursor.close();

            double saldoPendiente = Math.max(0.0, totalVenta - montoPagadoActual);
            if (saldoPendiente <= 0.001) {
                throw new Exception("Esta venta ya se encuentra totalmente saldada.");
            }
            if (abonoMonto > saldoPendiente + 0.01) {
                throw new Exception("El abono no puede superar el saldo pendiente de la venta.");
            }

            double nuevoMontoPagado = Math.min(totalVenta, montoPagadoActual + abonoMonto);
            String nuevoEstado = (totalVenta - nuevoMontoPagado <= 0.01) ? Sale.ESTADO_PAGADO : Sale.ESTADO_DEBE;

            ContentValues sv = new ContentValues();
            sv.put(DatabaseHelper.COL_SALE_MONTO_PAGADO, nuevoMontoPagado);
            sv.put(DatabaseHelper.COL_SALE_ESTADO_PAGO, nuevoEstado);
            int rows = db.update(DatabaseHelper.TABLE_SALES, sv, DatabaseHelper.COL_SALE_ID + " = ?", new String[]{String.valueOf(saleId)});
            if (rows <= 0) {
                throw new Exception("Error al actualizar los datos de la venta.");
            }

            ContentValues pv = new ContentValues();
            pv.put(DatabaseHelper.COL_PAYMENT_CLIENTE_ID, clienteId);
            pv.put(DatabaseHelper.COL_PAYMENT_VENTA_ID, saleId);
            pv.put(DatabaseHelper.COL_PAYMENT_FECHA, fecha);
            pv.put(DatabaseHelper.COL_PAYMENT_MONTO, abonoMonto);
            pv.put(DatabaseHelper.COL_PAYMENT_NOTA, (nota != null && !nota.trim().isEmpty()) ? nota : "Abono a Venta #V-" + saleId);
            long paymentId = db.insert(DatabaseHelper.TABLE_DEBT_PAYMENTS, null, pv);
            if (paymentId == -1) {
                throw new Exception("Error al guardar el abono en el historial.");
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Registra un abono general a la deuda de un cliente y lo distribuye automáticamente
     * aplicando el saldo a las ventas pendientes desde la más antigua a la más reciente (FIFO).
     */
    public boolean registerClientGeneralPayment(long clientId, double abonoTotal, String fecha, String nota) throws Exception {
        if (abonoTotal <= 0) {
            throw new Exception("El monto a abonar debe ser mayor a cero.");
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            String query = "SELECT " + DatabaseHelper.COL_SALE_ID + ", "
                    + DatabaseHelper.COL_SALE_TOTAL_VENTA + ", "
                    + DatabaseHelper.COL_SALE_MONTO_PAGADO + " "
                    + "FROM " + DatabaseHelper.TABLE_SALES + " "
                    + "WHERE " + DatabaseHelper.COL_SALE_CLIENTE_ID + " = ? AND "
                    + DatabaseHelper.COL_SALE_ESTADO_PAGO + " = ? "
                    + "ORDER BY " + DatabaseHelper.COL_SALE_FECHA + " ASC, " + DatabaseHelper.COL_SALE_ID + " ASC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(clientId), Sale.ESTADO_DEBE});
            List<Sale> pendingSales = new ArrayList<>();
            double deudaTotal = 0.0;
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    double total = cursor.getDouble(1);
                    double pagado = cursor.getDouble(2);
                    double saldo = Math.max(0.0, total - pagado);
                    if (saldo > 0.001) {
                        Sale s = new Sale();
                        s.setId(id);
                        s.setTotalVenta(total);
                        s.setMontoPagado(pagado);
                        pendingSales.add(s);
                        deudaTotal += saldo;
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }

            if (pendingSales.isEmpty() || deudaTotal <= 0.001) {
                throw new Exception("El cliente no registra deudas pendientes.");
            }

            if (abonoTotal > deudaTotal + 0.01) {
                throw new Exception("El abono no puede superar la deuda total pendiente.");
            }

            double restante = abonoTotal;
            for (Sale s : pendingSales) {
                if (restante <= 0.001) break;

                double saldoVenta = s.getTotalVenta() - s.getMontoPagado();
                if (saldoVenta <= 0.001) continue;

                double montoAplicado = Math.min(restante, saldoVenta);
                double nuevoPagado = Math.min(s.getTotalVenta(), s.getMontoPagado() + montoAplicado);
                String nuevoEstado = (s.getTotalVenta() - nuevoPagado <= 0.01) ? Sale.ESTADO_PAGADO : Sale.ESTADO_DEBE;

                ContentValues sv = new ContentValues();
                sv.put(DatabaseHelper.COL_SALE_MONTO_PAGADO, nuevoPagado);
                sv.put(DatabaseHelper.COL_SALE_ESTADO_PAGO, nuevoEstado);
                db.update(DatabaseHelper.TABLE_SALES, sv, DatabaseHelper.COL_SALE_ID + " = ?", new String[]{String.valueOf(s.getId())});

                ContentValues pv = new ContentValues();
                pv.put(DatabaseHelper.COL_PAYMENT_CLIENTE_ID, clientId);
                pv.put(DatabaseHelper.COL_PAYMENT_VENTA_ID, s.getId());
                pv.put(DatabaseHelper.COL_PAYMENT_FECHA, fecha);
                pv.put(DatabaseHelper.COL_PAYMENT_MONTO, montoAplicado);
                String detalleNota = (nota != null && !nota.trim().isEmpty())
                        ? nota + " (Aplicado a Venta #V-" + s.getId() + ")"
                        : "Abono general a Venta #V-" + s.getId();
                pv.put(DatabaseHelper.COL_PAYMENT_NOTA, detalleNota);
                db.insert(DatabaseHelper.TABLE_DEBT_PAYMENTS, null, pv);

                restante -= montoAplicado;
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Obtiene el historial de todos los abonos realizados por un cliente.
     */
    public List<Payment> getPaymentsByClient(long clientId) {
        List<Payment> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_DEBT_PAYMENTS, null,
                DatabaseHelper.COL_PAYMENT_CLIENTE_ID + " = ?",
                new String[]{String.valueOf(clientId)},
                null, null, DatabaseHelper.COL_PAYMENT_ID + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_ID));
                long cId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_CLIENTE_ID));
                long vId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_VENTA_ID));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_FECHA));
                double monto = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_MONTO));
                String nota = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_NOTA));
                list.add(new Payment(id, cId, vId, fecha, monto, nota));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * Obtiene el historial de abonos aplicados a una venta específica.
     */
    public List<Payment> getPaymentsBySale(long saleId) {
        List<Payment> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_DEBT_PAYMENTS, null,
                DatabaseHelper.COL_PAYMENT_VENTA_ID + " = ?",
                new String[]{String.valueOf(saleId)},
                null, null, DatabaseHelper.COL_PAYMENT_ID + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_ID));
                long cId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_CLIENTE_ID));
                long vId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_VENTA_ID));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_FECHA));
                double monto = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_MONTO));
                String nota = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PAYMENT_NOTA));
                list.add(new Payment(id, cId, vId, fecha, monto, nota));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * Calcula la Ganancia Proyectada del stock actual:
     * Si existen ventas previas, utiliza el margen promedio ponderado histórico.
     * Si aún no hay ventas, asume un margen base estimado del 10%.
     */
    public double getProjectedProfit(double currentInventoryValuation) {
        if (currentInventoryValuation <= 0) return 0.0;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(d." + DatabaseHelper.COL_DETAIL_CANTIDAD + " * b." + DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO + "), "
                + "SUM(d." + DatabaseHelper.COL_DETAIL_CANTIDAD + " * (d." + DatabaseHelper.COL_DETAIL_PRECIO_UNITARIO + " - b." + DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO + ")) "
                + "FROM " + DatabaseHelper.TABLE_SALE_DETAILS + " d "
                + "INNER JOIN " + DatabaseHelper.TABLE_BOX_PURCHASES + " b ON d." + DatabaseHelper.COL_DETAIL_CAJA_ID + " = b." + DatabaseHelper.COL_BOX_ID;

        Cursor cursor = db.rawQuery(query, null);
        double marginRatio = 0.10; // 10% por defecto si no hay historial
        if (cursor != null && cursor.moveToFirst()) {
            double totalCost = cursor.getDouble(0);
            double totalProfit = cursor.getDouble(1);
            if (totalCost > 0) {
                marginRatio = totalProfit / totalCost;
            }
            cursor.close();
        }
        return currentInventoryValuation * marginRatio;
    }

    private Sale mapCursorToSale(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SALE_ID));
        long clienteId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SALE_CLIENTE_ID));
        String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SALE_FECHA));
        double total = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SALE_TOTAL_VENTA));
        String estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SALE_ESTADO_PAGO));

        double pagado = 0.0;
        int colPagado = cursor.getColumnIndex(DatabaseHelper.COL_SALE_MONTO_PAGADO);
        if (colPagado != -1) {
            pagado = cursor.getDouble(colPagado);
        } else if (Sale.ESTADO_PAGADO.equalsIgnoreCase(estado)) {
            pagado = total;
        }

        Sale sale = new Sale(id, clienteId, fecha, total, pagado, estado);
        int colNombre = cursor.getColumnIndex(DatabaseHelper.COL_CLIENT_NOMBRE);
        if (colNombre != -1) {
            sale.setClienteNombre(cursor.getString(colNombre));
        }
        int colTel = cursor.getColumnIndex(DatabaseHelper.COL_CLIENT_TELEFONO);
        if (colTel != -1) {
            sale.setClienteTelefono(cursor.getString(colTel));
        }
        return sale;
    }
}
