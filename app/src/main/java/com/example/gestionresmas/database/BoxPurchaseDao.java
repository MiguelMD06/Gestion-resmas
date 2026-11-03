package com.example.gestionresmas.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestionresmas.models.BoxPurchase;

import java.util.ArrayList;
import java.util.List;

public class BoxPurchaseDao {
    private final DatabaseHelper dbHelper;

    public BoxPurchaseDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insert(BoxPurchase box) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_BOX_TIPO_RESMA, box.getTipoResma());
        values.put(DatabaseHelper.COL_BOX_FECHA_COMPRA, box.getFechaCompra());
        values.put(DatabaseHelper.COL_BOX_PRECIO_COMPRA, box.getPrecioCompraCaja());
        values.put(DatabaseHelper.COL_BOX_CANTIDAD_TOTAL, box.getCantidadResmasTotal());
        values.put(DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES, box.getResmasDisponibles());

        double costoUnitario = box.getCantidadResmasTotal() > 0 ?
                box.getPrecioCompraCaja() / box.getCantidadResmasTotal() : 0.0;
        values.put(DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO, costoUnitario);

        return db.insert(DatabaseHelper.TABLE_BOX_PURCHASES, null, values);
    }

    public int update(BoxPurchase box) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_BOX_TIPO_RESMA, box.getTipoResma());
        values.put(DatabaseHelper.COL_BOX_FECHA_COMPRA, box.getFechaCompra());
        values.put(DatabaseHelper.COL_BOX_PRECIO_COMPRA, box.getPrecioCompraCaja());
        values.put(DatabaseHelper.COL_BOX_CANTIDAD_TOTAL, box.getCantidadResmasTotal());
        values.put(DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES, box.getResmasDisponibles());

        double costoUnitario = box.getCantidadResmasTotal() > 0 ?
                box.getPrecioCompraCaja() / box.getCantidadResmasTotal() : 0.0;
        values.put(DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO, costoUnitario);

        return db.update(DatabaseHelper.TABLE_BOX_PURCHASES, values,
                DatabaseHelper.COL_BOX_ID + " = ?",
                new String[]{String.valueOf(box.getId())});
    }

    public int delete(long boxId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_BOX_PURCHASES,
                DatabaseHelper.COL_BOX_ID + " = ?",
                new String[]{String.valueOf(boxId)});
    }

    public BoxPurchase getBoxById(long boxId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_BOX_PURCHASES, null,
                DatabaseHelper.COL_BOX_ID + " = ?",
                new String[]{String.valueOf(boxId)}, null, null, null);

        BoxPurchase box = null;
        if (cursor != null && cursor.moveToFirst()) {
            box = mapCursorToBox(cursor);
            cursor.close();
        }
        return box;
    }

    public List<BoxPurchase> getAllBoxes() {
        List<BoxPurchase> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_BOX_PURCHASES, null, null, null, null, null,
                DatabaseHelper.COL_BOX_ID + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(mapCursorToBox(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<BoxPurchase> getActiveBoxes() {
        List<BoxPurchase> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_BOX_PURCHASES, null,
                DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + " > 0", null, null, null,
                DatabaseHelper.COL_BOX_ID + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(mapCursorToBox(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<BoxPurchase> getActiveBoxesByType(String tipoResma) {
        List<BoxPurchase> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_BOX_PURCHASES, null,
                DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + " > 0 AND " + DatabaseHelper.COL_BOX_TIPO_RESMA + " = ?",
                new String[]{tipoResma}, null, null, DatabaseHelper.COL_BOX_ID + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(mapCursorToBox(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public int getStockByType(String tipoResma) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + ") " +
                        "FROM " + DatabaseHelper.TABLE_BOX_PURCHASES + " " +
                        "WHERE " + DatabaseHelper.COL_BOX_TIPO_RESMA + " = ? AND " + DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + " > 0",
                new String[]{tipoResma});

        int stock = 0;
        if (cursor != null && cursor.moveToFirst()) {
            stock = cursor.getInt(0);
            cursor.close();
        }
        return stock;
    }

    public double getTotalInventoryValuation() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + " * " +
                DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO + ") " +
                "FROM " + DatabaseHelper.TABLE_BOX_PURCHASES + " " +
                "WHERE " + DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + " > 0", null);

        double valuation = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            valuation = cursor.getDouble(0);
            cursor.close();
        }
        return valuation;
    }

    public int getActiveBoxesCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_BOX_PURCHASES +
                " WHERE " + DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES + " > 0", null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private BoxPurchase mapCursorToBox(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_ID));
        String tipoResma = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_TIPO_RESMA));
        String fechaCompra = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_FECHA_COMPRA));
        double precioCompra = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_PRECIO_COMPRA));
        int cantidadTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_CANTIDAD_TOTAL));
        int resmasDisponibles = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_RESMAS_DISPONIBLES));
        double costoUnitario = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOX_PRECIO_COSTO_UNITARIO));

        return new BoxPurchase(id, tipoResma, fechaCompra, precioCompra, cantidadTotal, resmasDisponibles, costoUnitario);
    }
}
