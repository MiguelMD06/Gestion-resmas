package com.example.gestionresmas.ui.clients;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.database.ClientDao;
import com.example.gestionresmas.database.SaleDao;
import com.example.gestionresmas.models.Client;
import com.example.gestionresmas.models.Payment;
import com.example.gestionresmas.models.Sale;
import com.example.gestionresmas.models.SaleDetail;
import com.example.gestionresmas.ui.UiUtils;
import com.example.gestionresmas.ui.sales.NewSaleActivity;
import com.example.gestionresmas.ui.sales.SalesAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientDetailActivity extends AppCompatActivity implements SalesAdapter.OnSaleActionListener {

    private Client client;
    private ClientDao clientDao;
    private SaleDao saleDao;
    private SalesAdapter salesAdapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    private TextView tvDetailClientName;
    private TextView tvDetailClientPhone;
    private TextView tvDetailClientMargin;
    private TextView tvDetailClientDebt;
    private LinearLayout layoutDebtBanner;
    private LinearLayout layoutDebtActions;
    private MaterialButton btnClientPaymentHistory;
    private MaterialButton btnClientGeneralPayment;
    private TextView tvEmptyHistory;
    private RecyclerView rvClientSales;
    private double currentClientPendingDebt = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_detail);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            client = getIntent().getSerializableExtra("client", Client.class);
        } else {
            //noinspection deprecation
            client = (Client) getIntent().getSerializableExtra("client");
        }
        if (client == null) {
            finish();
            return;
        }

        clientDao = new ClientDao(this);
        saleDao = new SaleDao(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvDetailClientName = findViewById(R.id.tvDetailClientName);
        tvDetailClientPhone = findViewById(R.id.tvDetailClientPhone);
        tvDetailClientMargin = findViewById(R.id.tvDetailClientMargin);
        tvDetailClientDebt = findViewById(R.id.tvDetailClientDebt);
        layoutDebtBanner = findViewById(R.id.layoutDebtBanner);
        layoutDebtActions = findViewById(R.id.layoutDebtActions);
        btnClientPaymentHistory = findViewById(R.id.btnClientPaymentHistory);
        btnClientGeneralPayment = findViewById(R.id.btnClientGeneralPayment);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        rvClientSales = findViewById(R.id.rvClientSales);

        btnClientGeneralPayment.setOnClickListener(v -> showRegisterGeneralPaymentDialog());
        btnClientPaymentHistory.setOnClickListener(v -> showPaymentHistoryDialog());

        rvClientSales.setLayoutManager(UiUtils.createAdaptiveLayoutManager(this, 340));
        UiUtils.setupEdgeToEdge(this, findViewById(R.id.rootClientDetail), findViewById(R.id.appBarLayout), rvClientSales, null);
        salesAdapter = new SalesAdapter(this);
        salesAdapter.setShowPayButton(true);
        salesAdapter.setListener(this);
        rvClientSales.setAdapter(salesAdapter);

        displayClientInfo();
        loadClientSales();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClientSales();
    }

    private void displayClientInfo() {
        tvDetailClientName.setText(client.getNombre());
        tvDetailClientPhone.setText(client.getTelefono().isEmpty() ? "Sin teléfono" : "Teléfono: " + client.getTelefono());
        tvDetailClientMargin.setVisibility(View.GONE);
    }

    private void loadClientSales() {
        List<Sale> sales = saleDao.getSalesByClient(client.getId());
        salesAdapter.setSales(sales);

        tvEmptyHistory.setVisibility(sales.isEmpty() ? View.VISIBLE : View.GONE);

        // Calcular deuda total de este cliente según saldos pendientes reales
        currentClientPendingDebt = 0.0;
        for (Sale s : sales) {
            if (s.isDebe()) {
                currentClientPendingDebt += s.getSaldoPendiente();
            }
        }

        List<Payment> paymentHistory = saleDao.getPaymentsByClient(client.getId());

        if (currentClientPendingDebt > 0.001) {
            layoutDebtBanner.setVisibility(View.VISIBLE);
            tvDetailClientDebt.setText("Deuda total pendiente: " + currencyFormat.format(currentClientPendingDebt));
            layoutDebtActions.setVisibility(View.VISIBLE);
            btnClientGeneralPayment.setVisibility(View.VISIBLE);
            btnClientPaymentHistory.setVisibility(paymentHistory.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            layoutDebtBanner.setVisibility(View.GONE);
            if (!paymentHistory.isEmpty()) {
                layoutDebtActions.setVisibility(View.VISIBLE);
                btnClientGeneralPayment.setVisibility(View.GONE);
                btnClientPaymentHistory.setVisibility(View.VISIBLE);
            } else {
                layoutDebtActions.setVisibility(View.GONE);
            }
        }
    }

    private void showPaymentHistoryDialog() {
        List<Payment> payments = saleDao.getPaymentsByClient(client.getId());
        if (payments.isEmpty()) {
            Toast.makeText(this, "No hay abonos registrados para este cliente", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        double totalAbonado = 0.0;
        for (Payment p : payments) {
            totalAbonado += p.getMonto();
            sb.append("• ").append(p.getFecha()).append(" — ").append(currencyFormat.format(p.getMonto())).append("\n");
            sb.append("   Venta #V-").append(p.getVentaId());
            if (!p.getNota().isEmpty()) {
                sb.append(" (").append(p.getNota()).append(")");
            }
            sb.append("\n\n");
        }
        sb.append("Total Abonado Histórico: ").append(currencyFormat.format(totalAbonado));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Historial de Abonos - " + client.getNombre())
                .setMessage(sb.toString().trim())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void showRegisterGeneralPaymentDialog() {
        if (currentClientPendingDebt <= 0.001) {
            Toast.makeText(this, "El cliente no tiene deudas pendientes", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register_payment, null);

        TextView tvTarget = dialogView.findViewById(R.id.tvPaymentTargetInfo);
        TextView tvCurrentDebt = dialogView.findViewById(R.id.tvCurrentDebtAmount);
        EditText etAmount = dialogView.findViewById(R.id.etPaymentAmount);
        MaterialButton btnFull = dialogView.findViewById(R.id.btnPayFullBalance);
        TextView tvRemaining = dialogView.findViewById(R.id.tvRemainingBalancePreview);
        EditText etNote = dialogView.findViewById(R.id.etPaymentNote);

        final double totalDeuda = currentClientPendingDebt;
        tvTarget.setText("Cliente: " + client.getNombre());
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
                .setTitle("Registrar Abono a Deuda")
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

                        saleDao.registerClientGeneralPayment(client.getId(), amount, today, note);
                        Toast.makeText(this, "Abono de " + currencyFormat.format(amount) + " registrado con éxito", Toast.LENGTH_SHORT).show();
                        loadClientSales();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRegisterSalePaymentDialog(Sale sale) {
        double saldoPendiente = sale.getSaldoPendiente();
        if (saldoPendiente <= 0.001) {
            Toast.makeText(this, "Esta venta ya se encuentra pagada", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register_payment, null);

        TextView tvTarget = dialogView.findViewById(R.id.tvPaymentTargetInfo);
        TextView tvCurrentDebt = dialogView.findViewById(R.id.tvCurrentDebtAmount);
        EditText etAmount = dialogView.findViewById(R.id.etPaymentAmount);
        MaterialButton btnFull = dialogView.findViewById(R.id.btnPayFullBalance);
        TextView tvRemaining = dialogView.findViewById(R.id.tvRemainingBalancePreview);
        EditText etNote = dialogView.findViewById(R.id.etPaymentNote);

        tvTarget.setText("Venta #V-" + sale.getId() + " - " + sale.getClienteNombre());
        tvCurrentDebt.setText(currencyFormat.format(saldoPendiente));
        tvRemaining.setText("Saldo restante después del abono: " + currencyFormat.format(saldoPendiente));

        btnFull.setOnClickListener(v -> {
            etAmount.setText(String.format(Locale.US, "%.0f", saldoPendiente));
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
                        double remaining = Math.max(0.0, saldoPendiente - entered);
                        tvRemaining.setText("Saldo restante después del abono: " + currencyFormat.format(remaining));
                    } else {
                        tvRemaining.setText("Saldo restante después del abono: " + currencyFormat.format(saldoPendiente));
                    }
                } catch (NumberFormatException e) {
                    tvRemaining.setText("Saldo restante después del abono: " + currencyFormat.format(saldoPendiente));
                }
            }
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Abonar / Liquidar Venta #V-" + sale.getId())
                .setView(dialogView)
                .setPositiveButton("Registrar Pago", (dialog, which) -> {
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
                        if (amount > saldoPendiente + 0.01) {
                            Toast.makeText(this, "El abono no puede superar el saldo pendiente (" + currencyFormat.format(saldoPendiente) + ")", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        String note = etNote.getText().toString().trim();

                        saleDao.registerPaymentForSale(sale.getId(), amount, today, note);
                        Toast.makeText(this, "Abono de " + currencyFormat.format(amount) + " aplicado a la venta", Toast.LENGTH_SHORT).show();
                        loadClientSales();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onSaleClick(Sale sale) {
        List<SaleDetail> details = saleDao.getSaleDetails(sale.getId());
        List<Payment> payments = saleDao.getPaymentsBySale(sale.getId());

        StringBuilder message = new StringBuilder();
        message.append("Cliente: ").append(sale.getClienteNombre()).append("\n");
        message.append("Fecha: ").append(sale.getFecha()).append("\n");
        message.append("Estado: ").append(sale.getEstadoPago()).append("\n\n");
        message.append("Ítems vendidos:\n");

        for (SaleDetail d : details) {
            message.append("• ").append(d.getCantidad()).append(" resmas ")
                    .append(d.getTipoResma().isEmpty() ? "Lote #" + d.getCajaId() : d.getTipoResma())
                    .append(" (Lote #").append(d.getCajaId()).append(")\n")
                    .append("  Precio unitario: ").append(currencyFormat.format(d.getPrecioUnitarioAplicado()))
                    .append(" | Subtotal: ").append(currencyFormat.format(d.getSubtotal())).append("\n");
        }

        message.append("\nTotal Venta: ").append(currencyFormat.format(sale.getTotalVenta()));
        if (sale.getMontoPagado() > 0.001) {
            message.append("\nTotal Abonado: ").append(currencyFormat.format(sale.getMontoPagado()));
        }
        if (sale.isDebe()) {
            message.append("\nSaldo Restante: ").append(currencyFormat.format(sale.getSaldoPendiente()));
        }

        if (!payments.isEmpty()) {
            message.append("\n\nHistorial de Abonos Recibidos:\n");
            for (Payment p : payments) {
                message.append("• ").append(p.getFecha()).append(": ")
                        .append(currencyFormat.format(p.getMonto()));
                if (!p.getNota().isEmpty()) {
                    message.append(" (").append(p.getNota()).append(")");
                }
                message.append("\n");
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Detalle de Venta #V-" + sale.getId())
                .setMessage(message.toString())
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Editar Venta", (dialog, which) -> {
                    Intent intent = new Intent(this, NewSaleActivity.class);
                    intent.putExtra("extra_sale_id", sale.getId());
                    startActivity(intent);
                })
                .show();
    }

    @Override
    public void onMarkAsPaid(Sale sale) {
        showRegisterSalePaymentDialog(sale);
    }
}
