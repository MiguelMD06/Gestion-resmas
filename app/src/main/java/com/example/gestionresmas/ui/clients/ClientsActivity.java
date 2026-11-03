package com.example.gestionresmas.ui.clients;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.database.ClientDao;
import com.example.gestionresmas.database.SaleDao;
import com.example.gestionresmas.models.Client;
import com.example.gestionresmas.models.Sale;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ClientsActivity extends AppCompatActivity implements ClientAdapter.OnClientActionListener {

    private ClientDao clientDao;
    private SaleDao saleDao;
    private ClientAdapter adapter;
    private List<Client> fullClientList = new ArrayList<>();

    private RecyclerView rvClients;
    private TextView tvEmptyClients;
    private EditText etSearchClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clients);

        clientDao = new ClientDao(this);
        saleDao = new SaleDao(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvClients = findViewById(R.id.rvClients);
        tvEmptyClients = findViewById(R.id.tvEmptyClients);
        etSearchClient = findViewById(R.id.etSearchClient);
        ExtendedFloatingActionButton fabAddClient = findViewById(R.id.fabAddClient);

        rvClients.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClientAdapter(this);
        adapter.setListener(this);
        rvClients.setAdapter(adapter);

        fabAddClient.setOnClickListener(v -> showAddEditClientDialog(null));

        etSearchClient.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClients(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadClients();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClients();
    }

    private void loadClients() {
        fullClientList = clientDao.getAllClients();
        filterClients(etSearchClient.getText().toString());
    }

    private void filterClients(String query) {
        List<Client> filtered = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(fullClientList);
        } else {
            String lower = query.toLowerCase().trim();
            for (Client c : fullClientList) {
                if (c.getNombre().toLowerCase().contains(lower) ||
                        c.getTelefono().toLowerCase().contains(lower)) {
                    filtered.add(c);
                }
            }
        }
        adapter.setClients(filtered);
        tvEmptyClients.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddEditClientDialog(Client clientToEdit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_client, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etName = dialogView.findViewById(R.id.etClientName);
        EditText etPhone = dialogView.findViewById(R.id.etClientPhone);
        View tilMargin = dialogView.findViewById(R.id.tilClientMargin);
        if (tilMargin != null) {
            tilMargin.setVisibility(View.GONE);
        }

        boolean isEditing = clientToEdit != null;
        if (isEditing) {
            tvTitle.setText(R.string.edit_client);
            etName.setText(clientToEdit.getNombre());
            etPhone.setText(clientToEdit.getTelefono());
        } else {
            tvTitle.setText(R.string.add_client);
        }

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "El nombre del cliente es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isEditing) {
                        clientToEdit.setNombre(name);
                        clientToEdit.setTelefono(phone);
                        int rows = clientDao.update(clientToEdit);
                        if (rows > 0) {
                            Toast.makeText(this, "Cliente actualizado con éxito", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error al actualizar cliente", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Client newClient = new Client(name, phone);
                        long id = clientDao.insert(newClient);
                        if (id != -1) {
                            Toast.makeText(this, "Cliente registrado con éxito", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error al registrar cliente", Toast.LENGTH_SHORT).show();
                        }
                    }
                    loadClients();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onEdit(Client client) {
        showAddEditClientDialog(client);
    }

    @Override
    public void onViewHistory(Client client) {
        Intent intent = new Intent(this, ClientDetailActivity.class);
        intent.putExtra("client", client);
        startActivity(intent);
    }

    @Override
    public void onDelete(Client client) {
        List<Sale> clientSales = saleDao.getSalesByClient(client.getId());
        if (!clientSales.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("No se puede eliminar")
                    .setMessage("El cliente '" + client.getNombre() + "' tiene " + clientSales.size() +
                            " ventas asociadas en el historial. No puede eliminarse para preservar la trazabilidad.")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar cliente")
                .setMessage("¿Deseas eliminar al cliente '" + client.getNombre() + "'?")
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    clientDao.delete(client.getId());
                    Toast.makeText(this, "Cliente eliminado", Toast.LENGTH_SHORT).show();
                    loadClients();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
