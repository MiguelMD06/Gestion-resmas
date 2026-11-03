package com.example.gestionresmas.ui.sales;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.database.SaleDao;
import com.example.gestionresmas.models.Payment;
import com.example.gestionresmas.models.Sale;
import com.example.gestionresmas.models.SaleDetail;
import com.example.gestionresmas.ui.UiUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesHistoryActivity extends AppCompatActivity implements SalesAdapter.OnSaleActionListener {

    private SaleDao saleDao;
    private SalesAdapter adapter;
    private List<Sale> allSales = new ArrayList<>();

    private RecyclerView rvSalesHistory;
    private TextView tvEmptySalesHistory;
    private ChipGroup chipGroupSalesFilter;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_history);

        saleDao = new SaleDao(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvSalesHistory = findViewById(R.id.rvSalesHistory);
        tvEmptySalesHistory = findViewById(R.id.tvEmptySalesHistory);
        chipGroupSalesFilter = findViewById(R.id.chipGroupSalesFilter);

        rvSalesHistory.setLayoutManager(UiUtils.createAdaptiveLayoutManager(this, 340));
        adapter = new SalesAdapter(this);
        adapter.setShowPayButton(true);
        adapter.setListener(this);
        rvSalesHistory.setAdapter(adapter);

        UiUtils.setupEdgeToEdge(this, findViewById(R.id.rootSalesHistory), findViewById(R.id.appBarLayout), rvSalesHistory, null);

        chipGroupSalesFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            applyFilters();
        });

        if (savedInstanceState != null) {
            int savedChipId = savedInstanceState.getInt("selected_sales_chip", View.NO_ID);
            if (savedChipId != View.NO_ID) {
                chipGroupSalesFilter.check(savedChipId);
            }
        }

        loadSales();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (chipGroupSalesFilter != null) {
            outState.putInt("selected_sales_chip", chipGroupSalesFilter.getCheckedChipId());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSales();
    }

    private void loadSales() {
        allSales = saleDao.getAllSales();
        applyFilters();
    }

    private void applyFilters() {
        int checkedId = chipGroupSalesFilter.getCheckedChipId();
        List<Sale> filtered = new ArrayList<>();

        if (checkedId == R.id.chipPaidSales) {
            for (Sale s : allSales) {
                if (s.isPagado()) filtered.add(s);
            }
        } else if (checkedId == R.id.chipDebtSales) {
            for (Sale s : allSales) {
                if (s.isDebe()) filtered.add(s);
            }
        } else {
            filtered.addAll(allSales);
        }

        adapter.setSales(filtered);
        tvEmptySalesHistory.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
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
                        loadSales();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
