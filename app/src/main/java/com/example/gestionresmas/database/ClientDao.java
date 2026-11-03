package com.example.gestionresmas.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestionresmas.models.Client;

import java.util.ArrayList;
import java.util.List;

public class ClientDao {
    private final DatabaseHelper dbHelper;

    public ClientDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insert(Client client) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CLIENT_NOMBRE, client.getNombre());
        values.put(DatabaseHelper.COL_CLIENT_TELEFONO, client.getTelefono());
        return db.insert(DatabaseHelper.TABLE_CLIENTS, null, values);
    }

    public int update(Client client) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CLIENT_NOMBRE, client.getNombre());
        values.put(DatabaseHelper.COL_CLIENT_TELEFONO, client.getTelefono());
        return db.update(DatabaseHelper.TABLE_CLIENTS, values,
                DatabaseHelper.COL_CLIENT_ID + " = ?",
                new String[]{String.valueOf(client.getId())});
    }

    public int delete(long clientId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_CLIENTS,
                DatabaseHelper.COL_CLIENT_ID + " = ?",
                new String[]{String.valueOf(clientId)});
    }

    public Client getClientById(long clientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CLIENTS, null,
                DatabaseHelper.COL_CLIENT_ID + " = ?",
                new String[]{String.valueOf(clientId)}, null, null, null);

        Client client = null;
        if (cursor != null && cursor.moveToFirst()) {
            client = mapCursorToClient(cursor);
            cursor.close();
        }
        return client;
    }

    public List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CLIENTS, null, null, null, null, null,
                DatabaseHelper.COL_CLIENT_NOMBRE + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                clients.add(mapCursorToClient(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return clients;
    }

    public int getClientsCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_CLIENTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private Client mapCursorToClient(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLIENT_ID));
        String nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLIENT_NOMBRE));
        String telefono = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLIENT_TELEFONO));
        return new Client(id, nombre, telefono);
    }
}
