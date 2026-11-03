package com.example.gestionresmas.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gestion_resmas.db";
    private static final int DATABASE_VERSION = 4;

    private static DatabaseHelper instance;

    // Tabla Clientes
    public static final String TABLE_CLIENTS = "clients";
    public static final String COL_CLIENT_ID = "id";
    public static final String COL_CLIENT_NOMBRE = "nombre";
    public static final String COL_CLIENT_TELEFONO = "telefono";

    // Tabla Compras de Cajas (box_purchases)
    public static final String TABLE_BOX_PURCHASES = "box_purchases";
    public static final String COL_BOX_ID = "id";
    public static final String COL_BOX_TIPO_RESMA = "tipo_resma";
    public static final String COL_BOX_FECHA_COMPRA = "fecha_compra";
    public static final String COL_BOX_PRECIO_COMPRA = "precio_compra_caja";
    public static final String COL_BOX_CANTIDAD_TOTAL = "cantidad_resmas_total";
    public static final String COL_BOX_RESMAS_DISPONIBLES = "resmas_disponibles";
    public static final String COL_BOX_PRECIO_COSTO_UNITARIO = "precio_costo_unitario";

    // Tabla Ventas (sales)
    public static final String TABLE_SALES = "sales";
    public static final String COL_SALE_ID = "id";
    public static final String COL_SALE_CLIENTE_ID = "cliente_id";
    public static final String COL_SALE_FECHA = "fecha";
    public static final String COL_SALE_TOTAL_VENTA = "total_venta";
    public static final String COL_SALE_MONTO_PAGADO = "monto_pagado";
    public static final String COL_SALE_ESTADO_PAGO = "estado_pago";

    // Tabla Detalles de Venta (sale_details)
    public static final String TABLE_SALE_DETAILS = "sale_details";
    public static final String COL_DETAIL_ID = "id";
    public static final String COL_DETAIL_VENTA_ID = "venta_id";
    public static final String COL_DETAIL_CAJA_ID = "caja_id";
    public static final String COL_DETAIL_CANTIDAD = "cantidad";
    public static final String COL_DETAIL_PRECIO_UNITARIO = "precio_unitario_aplicado";
    public static final String COL_DETAIL_SUBTOTAL = "subtotal";

    // Tabla Abonos / Pagos (debt_payments)
    public static final String TABLE_DEBT_PAYMENTS = "debt_payments";
    public static final String COL_PAYMENT_ID = "id";
    public static final String COL_PAYMENT_CLIENTE_ID = "cliente_id";
    public static final String COL_PAYMENT_VENTA_ID = "venta_id";
    public static final String COL_PAYMENT_FECHA = "fecha";
    public static final String COL_PAYMENT_MONTO = "monto";
    public static final String COL_PAYMENT_NOTA = "nota";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla clients
        String createClients = "CREATE TABLE " + TABLE_CLIENTS + " ("
                + COL_CLIENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CLIENT_NOMBRE + " TEXT NOT NULL, "
                + COL_CLIENT_TELEFONO + " TEXT"
                + ");";
        db.execSQL(createClients);

        // Crear tabla box_purchases
        String createBoxPurchases = "CREATE TABLE " + TABLE_BOX_PURCHASES + " ("
                + COL_BOX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_BOX_TIPO_RESMA + " TEXT NOT NULL, "
                + COL_BOX_FECHA_COMPRA + " TEXT NOT NULL, "
                + COL_BOX_PRECIO_COMPRA + " REAL NOT NULL, "
                + COL_BOX_CANTIDAD_TOTAL + " INTEGER NOT NULL, "
                + COL_BOX_RESMAS_DISPONIBLES + " INTEGER NOT NULL, "
                + COL_BOX_PRECIO_COSTO_UNITARIO + " REAL NOT NULL"
                + ");";
        db.execSQL(createBoxPurchases);

        // Crear tabla sales
        String createSales = "CREATE TABLE " + TABLE_SALES + " ("
                + COL_SALE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_SALE_CLIENTE_ID + " INTEGER NOT NULL, "
                + COL_SALE_FECHA + " TEXT NOT NULL, "
                + COL_SALE_TOTAL_VENTA + " REAL NOT NULL, "
                + COL_SALE_MONTO_PAGADO + " REAL NOT NULL DEFAULT 0.0, "
                + COL_SALE_ESTADO_PAGO + " TEXT NOT NULL, "
                + "FOREIGN KEY (" + COL_SALE_CLIENTE_ID + ") REFERENCES " + TABLE_CLIENTS + "(" + COL_CLIENT_ID + ") ON DELETE RESTRICT"
                + ");";
        db.execSQL(createSales);

        // Crear tabla sale_details
        String createSaleDetails = "CREATE TABLE " + TABLE_SALE_DETAILS + " ("
                + COL_DETAIL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_DETAIL_VENTA_ID + " INTEGER NOT NULL, "
                + COL_DETAIL_CAJA_ID + " INTEGER NOT NULL, "
                + COL_DETAIL_CANTIDAD + " INTEGER NOT NULL, "
                + COL_DETAIL_PRECIO_UNITARIO + " REAL NOT NULL, "
                + COL_DETAIL_SUBTOTAL + " REAL NOT NULL, "
                + "FOREIGN KEY (" + COL_DETAIL_VENTA_ID + ") REFERENCES " + TABLE_SALES + "(" + COL_SALE_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + COL_DETAIL_CAJA_ID + ") REFERENCES " + TABLE_BOX_PURCHASES + "(" + COL_BOX_ID + ") ON DELETE RESTRICT"
                + ");";
        db.execSQL(createSaleDetails);

        // Crear tabla debt_payments
        String createDebtPayments = "CREATE TABLE " + TABLE_DEBT_PAYMENTS + " ("
                + COL_PAYMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PAYMENT_CLIENTE_ID + " INTEGER NOT NULL, "
                + COL_PAYMENT_VENTA_ID + " INTEGER NOT NULL, "
                + COL_PAYMENT_FECHA + " TEXT NOT NULL, "
                + COL_PAYMENT_MONTO + " REAL NOT NULL, "
                + COL_PAYMENT_NOTA + " TEXT, "
                + "FOREIGN KEY (" + COL_PAYMENT_CLIENTE_ID + ") REFERENCES " + TABLE_CLIENTS + "(" + COL_CLIENT_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + COL_PAYMENT_VENTA_ID + ") REFERENCES " + TABLE_SALES + "(" + COL_SALE_ID + ") ON DELETE CASCADE"
                + ");";
        db.execSQL(createDebtPayments);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_SALES + " ADD COLUMN " + COL_SALE_MONTO_PAGADO + " REAL NOT NULL DEFAULT 0.0;");
            db.execSQL("UPDATE " + TABLE_SALES + " SET " + COL_SALE_MONTO_PAGADO + " = " + COL_SALE_TOTAL_VENTA
                    + " WHERE " + COL_SALE_ESTADO_PAGO + " = 'PAGADO';");
            String createDebtPayments = "CREATE TABLE IF NOT EXISTS " + TABLE_DEBT_PAYMENTS + " ("
                    + COL_PAYMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_PAYMENT_CLIENTE_ID + " INTEGER NOT NULL, "
                    + COL_PAYMENT_VENTA_ID + " INTEGER NOT NULL, "
                    + COL_PAYMENT_FECHA + " TEXT NOT NULL, "
                    + COL_PAYMENT_MONTO + " REAL NOT NULL, "
                    + COL_PAYMENT_NOTA + " TEXT, "
                    + "FOREIGN KEY (" + COL_PAYMENT_CLIENTE_ID + ") REFERENCES " + TABLE_CLIENTS + "(" + COL_CLIENT_ID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY (" + COL_PAYMENT_VENTA_ID + ") REFERENCES " + TABLE_SALES + "(" + COL_SALE_ID + ") ON DELETE CASCADE"
                    + ");";
            db.execSQL(createDebtPayments);
            return;
        }

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEBT_PAYMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALE_DETAILS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOX_PURCHASES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLIENTS);
        onCreate(db);
    }
}
