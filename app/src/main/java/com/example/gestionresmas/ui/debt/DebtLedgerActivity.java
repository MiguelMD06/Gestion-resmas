package com.example.gestionresmas.ui.debt;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.database.ClientDao;
import com.example.gestionresmas.database.SaleDao;
import com.example.gestionresmas.models.Client;
import com.example.gestionresmas.models.ClientDebtSummary;
import com.example.gestionresmas.ui.UiUtils;
import com.example.gestionresmas.ui.clients.ClientDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DebtLedgerActivity extends AppCompatActivity implements DebtAdapter.OnDebtClickListener {

    private SaleDao saleDao;
    private ClientDao clientDao;
    private DebtAdapter adapter;

    private TextView tvTotalLedgerDebt;
    private TextView tvDebtorsCount;
    private TextView tvEmptyDebts;
    private RecyclerView rvDebtors;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debt_ledger);

        saleDao = new SaleDao(this);
        clientDao = new ClientDao(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTotalLedgerDebt = findViewById(R.id.tvTotalLedgerDebt);
        tvDebtorsCount = findViewById(R.id.tvDebtorsCount);
        tvEmptyDebts = findViewById(R.id.tvEmptyDebts);
        rvDebtors = findViewById(R.id.rvDebtors);

        rvDebtors.setLayoutManager(UiUtils.createAdaptiveLayoutManager(this, 340));
        UiUtils.setupEdgeToEdge(this, findViewById(R.id.rootDebtLedger), findViewById(R.id.appBarLayout), rvDebtors, null);
        adapter = new DebtAdapter(this);
        adapter.setListener(this);
        rvDebtors.setAdapter(adapter);

        loadDebtData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDebtData();
    }

    private void loadDebtData() {
        List<ClientDebtSummary> debtSummaries = saleDao.getClientDebtSummaries();
        adapter.setDebts(debtSummaries);

        double totalDebt = saleDao.getTotalDebtAmount();
        tvTotalLedgerDebt.setText(currencyFormat.format(totalDebt));
        tvDebtorsCount.setText(debtSummaries.size() + (debtSummaries.size() == 1 ? " cliente con saldo pendiente" : " clientes con saldo pendiente"));

        tvEmptyDebts.setVisibility(debtSummaries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDebtClick(ClientDebtSummary debtSummary) {
        Client client = clientDao.getClientById(debtSummary.getClienteId());
        if (client != null) {
            Intent intent = new Intent(this, ClientDetailActivity.class);
            intent.putExtra("client", client);
            startActivity(intent);
        }
    }

    @Override
    public void onRegisterPaymentClick(ClientDebtSummary debtSummary) {
        showRegisterGeneralPaymentDialog(debtSummary);
    }

    private void showRegisterGeneralPaymentDialog(ClientDebtSummary debtSummary) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register_payment, null);

        TextView tvTarget = dialogView.findViewById(R.id.tvPaymentTargetInfo);
        TextView tvCurrentDebt = dialogView.findViewById(R.id.tvCurrentDebtAmount);
        EditText etAmount = dialogView.findViewById(R.id.etPaymentAmount);
        MaterialButton btnFull = dialogView.findViewById(R.id.btnPayFullBalance);
        TextView tvRemaining = dialogView.findViewById(R.id.tvRemainingBalancePreview);
        EditText etNote = dialogView.findViewById(R.id.etPaymentNote);

        double totalDeuda = debtSummary.getTotalDeuda();
        tvTarget.setText("Cliente: " + debtSummary.getClienteNombre());
        tvCurrentDebt.setText(currencyFormat.format(totalDeuda));
        tvRemaining.setText("Saldo restante después del abono: " + currencyFormat.format(totalDeuda));

        btnFull.setOnClickListener(v -> {
            etAmount.setText(String.format(Locale.US, "%.0f", totalDeuda));
            etAmount.setSelection(etAmount.getText().length());
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String text = s.toString().trim();
                    if (!text.isEmpty()) {
                        double entered = Double.parseDouble(text);
                        double remaining = Math.max(0.0, totalDeuda - entered);
                        tvRemaining.setText("Saldo restante después del abono: " + currencyFormat.format(remaining));
                    } else {
                        tvRemaining.setText("Saldo restante después del abono: " + currencyFormat.format(totalDeuda));
                    }
                } catch (NumberFormatException e) {
                    tvRemaining.setText("Saldo restante después del abono: " + currencyFormat.format(totalDeuda));
                }
            }
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Registrar Abono a Cliente")
                .setView(dialogView)
                .setPositiveButton("Registrar Abono", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Debe ingresar un monto", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double amount = Double.parseDouble(amountStr);
                        if (amount <= 0) {
                            Toast.makeText(this, "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (amount > totalDeuda + 0.01) {
                            Toast.makeText(this, "El abono no puede superar la deuda total (" + currencyFormat.format(totalDeuda) + ")", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        String note = etNote.getText().toString().trim();

                        saleDao.registerClientGeneralPayment(debtSummary.getClienteId(), amount, today, note);
                        Toast.makeText(this, "Abono de " + currencyFormat.format(amount) + " registrado con éxito", Toast.LENGTH_SHORT).show();
                        loadDebtData();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
