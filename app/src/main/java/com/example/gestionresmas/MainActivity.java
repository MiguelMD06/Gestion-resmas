package com.example.gestionresmas;

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

import com.example.gestionresmas.database.BoxPurchaseDao;
import com.example.gestionresmas.database.DatabaseHelper;
import com.example.gestionresmas.database.SaleDao;
import com.example.gestionresmas.models.Payment;
import com.example.gestionresmas.models.Sale;
import com.example.gestionresmas.models.SaleDetail;
import com.example.gestionresmas.ui.boxes.BoxPurchasesActivity;
import com.example.gestionresmas.ui.clients.ClientsActivity;
import com.example.gestionresmas.ui.debt.DebtLedgerActivity;
import com.example.gestionresmas.ui.sales.NewSaleActivity;
import com.example.gestionresmas.ui.sales.SalesAdapter;
import com.example.gestionresmas.ui.sales.SalesHistoryActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SalesAdapter.OnSaleActionListener {

    private BoxPurchaseDao boxDao;
    private SaleDao saleDao;
    private SalesAdapter recentSalesAdapter;

    private TextView tvStockCarta;
    private TextView tvStockOficio;
    private TextView tvValorInventario;
    private TextView tvCajasActivas;
    private TextView tvTotalDeuda;
    private TextView tvCapitalRecuperado;
    private TextView tvGananciaReal;
    private TextView tvGananciaProyectada;
    private TextView tvSubtextoGananciaProyectada;
    private TextView tvEmptySales;
    private RecyclerView rvRecentSales;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar DB Helper y DAOs
        DatabaseHelper.getInstance(this);
        boxDao = new BoxPurchaseDao(this);
        saleDao = new SaleDao(this);

        initViews();
        setupNavigation();
    }

    private void initViews() {
        tvStockCarta = findViewById(R.id.tvStockCarta);
        tvStockOficio = findViewById(R.id.tvStockOficio);
        tvValorInventario = findViewById(R.id.tvValorInventario);
        tvCajasActivas = findViewById(R.id.tvCajasActivas);
        tvTotalDeuda = findViewById(R.id.tvTotalDeuda);
        tvCapitalRecuperado = findViewById(R.id.tvCapitalRecuperado);
        tvGananciaReal = findViewById(R.id.tvGananciaReal);
        tvGananciaProyectada = findViewById(R.id.tvGananciaProyectada);
        tvSubtextoGananciaProyectada = findViewById(R.id.tvSubtextoGananciaProyectada);
        tvEmptySales = findViewById(R.id.tvEmptySales);
        rvRecentSales = findViewById(R.id.rvRecentSales);

        rvRecentSales.setLayoutManager(new LinearLayoutManager(this));
        recentSalesAdapter = new SalesAdapter(this);
        recentSalesAdapter.setListener(this);
        rvRecentSales.setAdapter(recentSalesAdapter);
    }

    private void setupNavigation() {
        MaterialCardView cardNewSale = findViewById(R.id.cardNewSale);
        MaterialCardView cardBoxes = findViewById(R.id.cardBoxes);
        MaterialCardView cardClients = findViewById(R.id.cardClients);
        MaterialCardView cardDebtLedger = findViewById(R.id.cardDebtLedger);
        MaterialCardView cardDebtSummary = findViewById(R.id.cardDebtSummary);
        MaterialCardView cardHistory = findViewById(R.id.cardHistory);
        TextView tvViewAllSales = findViewById(R.id.tvViewAllSales);

        cardNewSale.setOnClickListener(v -> startActivity(new Intent(this, NewSaleActivity.class)));
        cardBoxes.setOnClickListener(v -> startActivity(new Intent(this, BoxPurchasesActivity.class)));
        cardClients.setOnClickListener(v -> startActivity(new Intent(this, ClientsActivity.class)));

        View.OnClickListener openDebts = v -> startActivity(new Intent(this, DebtLedgerActivity.class));
        cardDebtLedger.setOnClickListener(openDebts);
        cardDebtSummary.setOnClickListener(openDebts);

        View.OnClickListener openHistory = v -> startActivity(new Intent(this, SalesHistoryActivity.class));
        cardHistory.setOnClickListener(openHistory);
        tvViewAllSales.setOnClickListener(openHistory);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
    }

    /**
     * Actualiza las estadísticas del Dashboard según reglas del SKILL.md:
     * - Stock por tamaño (Carta y Oficio) sumando resmas_disponibles de cajas activas.
     * - Valor total del inventario.
     * - Total de cuentas por cobrar (ventas con estado 'DEBE').
     * - Últimas ventas registradas.
     */
    private void refreshDashboard() {
        // 1. Stock por tamaño
        int stockCarta = boxDao.getStockByType("CARTA");
        int stockOficio = boxDao.getStockByType("OFICIO");
        tvStockCarta.setText(String.valueOf(stockCarta));
        tvStockOficio.setText(String.valueOf(stockOficio));

        // 2. Valuación de inventario
        double inventarioValor = boxDao.getTotalInventoryValuation();
        tvValorInventario.setText(currencyFormat.format(inventarioValor));

        int cajasActivas = boxDao.getActiveBoxesCount();
        tvCajasActivas.setText(cajasActivas + (cajasActivas == 1 ? " lote activo" : " lotes activos"));

        // 3. Deuda total pendiente
        double totalDeuda = saleDao.getTotalDebtAmount();
        tvTotalDeuda.setText(currencyFormat.format(totalDeuda));

        // 4. Métricas Financieras: Capital Recuperado, Ganancia Real y Proyectada
        double capitalRecuperado = saleDao.getRecoveredCapital();
        tvCapitalRecuperado.setText(currencyFormat.format(capitalRecuperado));

        double gananciaReal = saleDao.getRealProfit();
        tvGananciaReal.setText(currencyFormat.format(gananciaReal));

        double gananciaProyectada = saleDao.getProjectedProfit(inventarioValor);
        tvGananciaProyectada.setText(currencyFormat.format(gananciaProyectada));

        // 5. Últimas 5 ventas
        List<Sale> allSales = saleDao.getAllSales();
        List<Sale> recent = new ArrayList<>();
        int max = Math.min(allSales.size(), 5);
        for (int i = 0; i < max; i++) {
            recent.add(allSales.get(i));
        }

        recentSalesAdapter.setSales(recent);
        tvEmptySales.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
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
                        refreshDashboard();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}