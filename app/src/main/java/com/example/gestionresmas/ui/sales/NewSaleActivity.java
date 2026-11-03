package com.example.gestionresmas.ui.sales;

import android.app.DatePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.database.BoxPurchaseDao;
import com.example.gestionresmas.database.ClientDao;
import com.example.gestionresmas.database.SaleDao;
import com.example.gestionresmas.models.BoxPurchase;
import com.example.gestionresmas.models.Client;
import com.example.gestionresmas.models.Sale;
import com.example.gestionresmas.models.SaleDetail;
import com.example.gestionresmas.ui.UiUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewSaleActivity extends AppCompatActivity {

    public static final String EXTRA_SALE_ID = "extra_sale_id";

    private ClientDao clientDao;
    private BoxPurchaseDao boxDao;
    private SaleDao saleDao;

    private List<Client> clientsList = new ArrayList<>();
    private List<BoxPurchase> activeLotsList = new ArrayList<>();
    private final List<SaleDetail> cartItems = new ArrayList<>();
    private final List<SaleDetail> originalDetails = new ArrayList<>();
    private SaleCartAdapter cartAdapter;

    private MaterialToolbar toolbar;
    private Spinner spinnerClients;
    private Spinner spinnerLots;
    private EditText etUnitPrice;
    private EditText etMargin;
    private TextView tvFormulaPreview;
    private TextView tvDynamicUnitPrice;
    private TextInputLayout tilQuantity;
    private EditText etQuantity;
    private MaterialButton btnAddToCart;

    private RecyclerView rvCartItems;
    private TextView tvEmptyCart;
    private EditText etSaleDate;
    private RadioGroup rgPaymentStatus;
    private RadioButton rbPagado;
    private RadioButton rbDebe;
    private TextView tvTotalSaleAmount;
    private MaterialButton btnConfirmSale;

    private Client selectedClient;
    private BoxPurchase selectedLot;
    private double currentDynamicUnitPrice = 0.0;
    private boolean isUpdatingPrice = false;

    private long editSaleId = -1;
    private boolean isEditing = false;

    private boolean isRestoringState = false;
    private int targetRestoredLotPos = -1;
    private String restoredUnitPrice = null;
    private String restoredMargin = null;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sale);

        clientDao = new ClientDao(this);
        boxDao = new BoxPurchaseDao(this);
        saleDao = new SaleDao(this);

        if (getIntent().hasExtra(EXTRA_SALE_ID)) {
            editSaleId = getIntent().getLongExtra(EXTRA_SALE_ID, -1);
            isEditing = (editSaleId != -1);
        }

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (isEditing) {
            toolbar.setTitle("Editar Venta #V-" + editSaleId);
        }

        spinnerClients = findViewById(R.id.spinnerClients);
        spinnerLots = findViewById(R.id.spinnerLots);
        etUnitPrice = findViewById(R.id.etUnitPrice);
        etMargin = findViewById(R.id.etMargin);
        tvFormulaPreview = findViewById(R.id.tvFormulaPreview);
        tvDynamicUnitPrice = findViewById(R.id.tvDynamicUnitPrice);
        tilQuantity = findViewById(R.id.tilQuantity);
        etQuantity = findViewById(R.id.etQuantity);
        btnAddToCart = findViewById(R.id.btnAddToCart);

        rvCartItems = findViewById(R.id.rvCartItems);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        etSaleDate = findViewById(R.id.etSaleDate);
        rgPaymentStatus = findViewById(R.id.rgPaymentStatus);
        rbPagado = findViewById(R.id.rbPagado);
        rbDebe = findViewById(R.id.rbDebe);
        tvTotalSaleAmount = findViewById(R.id.tvTotalSaleAmount);
        btnConfirmSale = findViewById(R.id.btnConfirmSale);

        if (isEditing) {
            btnConfirmSale.setText("Guardar Cambios de Venta");
        }

        rvCartItems.setLayoutManager(UiUtils.createAdaptiveLayoutManager(this, 340));
        cartAdapter = new SaleCartAdapter(this);
        cartAdapter.setListener((position, detail) -> {
            cartItems.remove(position);
            cartAdapter.setItems(cartItems);
            updateCartSummary();
        });
        rvCartItems.setAdapter(cartAdapter);

        UiUtils.setupEdgeToEdge(this, findViewById(R.id.rootNewSale), findViewById(R.id.appBarLayout), findViewById(R.id.nestedScrollNewSale), null);

        // Fecha actual por defecto
        String today = dateFormat.format(new Date());
        etSaleDate.setText(today);

        final Calendar calendar = Calendar.getInstance();
        etSaleDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                etSaleDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        setupPriceAndMarginListeners();
        setupClientsSpinner();

        btnAddToCart.setOnClickListener(v -> addItemToCart());
        btnConfirmSale.setOnClickListener(v -> confirmSale());

        if (savedInstanceState != null) {
            if (isEditing) {
                // Siempre cargar originalDetails para el cálculo de stock efectivo y lotes originales
                List<SaleDetail> details = saleDao.getSaleDetails(editSaleId);
                originalDetails.clear();
                originalDetails.addAll(details);
            }

            List<SaleDetail> savedCart;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                @SuppressWarnings("unchecked")
                ArrayList<SaleDetail> list = (ArrayList<SaleDetail>) savedInstanceState.getSerializable("state_cart_items", ArrayList.class);
                savedCart = list;
            } else {
                @SuppressWarnings("unchecked")
                ArrayList<SaleDetail> list = (ArrayList<SaleDetail>) savedInstanceState.getSerializable("state_cart_items");
                savedCart = list;
            }

            cartItems.clear();
            if (savedCart != null) {
                cartItems.addAll(savedCart);
            }
            cartAdapter.setItems(cartItems);
            updateCartSummary();

            int savedClientPos = savedInstanceState.getInt("state_client_pos", -1);
            if (savedClientPos >= 0 && savedClientPos < clientsList.size()) {
                spinnerClients.setSelection(savedClientPos);
                selectedClient = clientsList.get(savedClientPos);
            }

            int savedLotPos = savedInstanceState.getInt("state_lot_pos", 0);
            targetRestoredLotPos = savedLotPos;
            isRestoringState = true;
            restoredUnitPrice = savedInstanceState.getString("state_unit_price", "");
            restoredMargin = savedInstanceState.getString("state_margin", "");

            setupLotsSpinner();

            if (savedLotPos >= 0 && savedLotPos < activeLotsList.size()) {
                spinnerLots.setSelection(savedLotPos);
                selectedLot = activeLotsList.get(savedLotPos);
            }

            String savedQuantity = savedInstanceState.getString("state_quantity", "1");
            etQuantity.setText(savedQuantity);

            String savedSaleDate = savedInstanceState.getString("state_sale_date", "");
            if (savedSaleDate != null && !savedSaleDate.isEmpty()) {
                etSaleDate.setText(savedSaleDate);
                try {
                    Date d = dateFormat.parse(savedSaleDate);
                    if (d != null) {
                        calendar.setTime(d);
                    }
                } catch (Exception ignored) {}
            }

            boolean savedIsPaid = savedInstanceState.getBoolean("state_is_paid", true);
            if (savedIsPaid) {
                rbPagado.setChecked(true);
            } else {
                rbDebe.setChecked(true);
            }

            isUpdatingPrice = true;
            try {
                if (restoredUnitPrice != null) {
                    etUnitPrice.setText(restoredUnitPrice);
                }
                if (restoredMargin != null) {
                    etMargin.setText(restoredMargin);
                }

                double unitPrice = 0.0;
                try {
                    if (restoredUnitPrice != null && !restoredUnitPrice.isEmpty()) {
                        unitPrice = Double.parseDouble(restoredUnitPrice);
                    }
                } catch (NumberFormatException ignored) {}
                currentDynamicUnitPrice = unitPrice;

                if (selectedLot != null) {
                    double unitCost = selectedLot.getPrecioCostoUnitario();
                    double utility = SaleDetail.calcularUtilidadUnitaria(unitPrice, unitCost);
                    double margin = 0.0;
                    try {
                        if (restoredMargin != null && !restoredMargin.isEmpty()) {
                            margin = Double.parseDouble(restoredMargin);
                        }
                    } catch (NumberFormatException ignored) {}

                    tvFormulaPreview.setText("Costo: " + currencyFormat.format(unitCost) +
                            " | Utilidad: " + currencyFormat.format(utility) +
                            " (" + String.format(Locale.getDefault(), "%.1f", margin) + "% margen)");
                    tvDynamicUnitPrice.setText(currencyFormat.format(unitPrice));
                }
            } finally {
                isUpdatingPrice = false;
            }
        } else {
            if (isEditing) {
                loadExistingSale();
            } else {
                setupLotsSpinner();
                updateCartSummary();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("state_cart_items", new ArrayList<>(cartItems));
        outState.putInt("state_client_pos", spinnerClients.getSelectedItemPosition());
        outState.putInt("state_lot_pos", spinnerLots.getSelectedItemPosition());
        outState.putString("state_unit_price", etUnitPrice.getText().toString());
        outState.putString("state_margin", etMargin.getText().toString());
        outState.putString("state_quantity", etQuantity.getText().toString());
        outState.putString("state_sale_date", etSaleDate.getText().toString());
        outState.putBoolean("state_is_paid", rbPagado.isChecked());
    }

    private void setupPriceAndMarginListeners() {
        // Al modificar el precio de venta unitario libremente:
        etUnitPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingPrice || selectedLot == null) return;
                isUpdatingPrice = true;
                try {
                    String priceStr = s.toString().trim();
                    if (!priceStr.isEmpty()) {
                        double price = Double.parseDouble(priceStr);
                        currentDynamicUnitPrice = price;
                        double unitCost = selectedLot.getPrecioCostoUnitario();
                        double margin = SaleDetail.calcularPorcentajeMargen(price, unitCost);
                        double utility = SaleDetail.calcularUtilidadUnitaria(price, unitCost);

                        etMargin.setText(String.format(Locale.US, "%.1f", margin));
                        tvFormulaPreview.setText("Costo: " + currencyFormat.format(unitCost) +
                                " | Utilidad: " + currencyFormat.format(utility) +
                                " (" + String.format(Locale.getDefault(), "%.1f", margin) + "% margen)");
                        tvDynamicUnitPrice.setText(currencyFormat.format(price));
                    }
                } catch (NumberFormatException ignored) {
                } finally {
                    isUpdatingPrice = false;
                }
            }
        });

        // Al modificar el porcentaje de margen:
        etMargin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingPrice || selectedLot == null) return;
                isUpdatingPrice = true;
                try {
                    String marginStr = s.toString().trim();
                    if (!marginStr.isEmpty()) {
                        double margin = Double.parseDouble(marginStr);
                        double unitCost = selectedLot.getPrecioCostoUnitario();
                        double price = SaleDetail.calcularPrecioVentaDesdeMargen(unitCost, margin);
                        double utility = SaleDetail.calcularUtilidadUnitaria(price, unitCost);
                        currentDynamicUnitPrice = price;

                        etUnitPrice.setText(String.format(Locale.US, "%.2f", price));
                        tvFormulaPreview.setText("Costo: " + currencyFormat.format(unitCost) +
                                " | Utilidad: " + currencyFormat.format(utility) +
                                " (" + String.format(Locale.getDefault(), "%.1f", margin) + "% margen)");
                        tvDynamicUnitPrice.setText(currencyFormat.format(price));
                    }
                } catch (NumberFormatException ignored) {
                } finally {
                    isUpdatingPrice = false;
                }
            }
        });
    }

    private void setupClientsSpinner() {
        clientsList = clientDao.getAllClients();
        if (clientsList.isEmpty()) {
            Toast.makeText(this, "Debe registrar al menos un cliente antes de realizar una venta", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayAdapter<Client> clientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, clientsList);
        spinnerClients.setAdapter(clientAdapter);

        spinnerClients.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedClient = clientsList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedClient = null;
            }
        });
    }

    private void setupLotsSpinner() {
        activeLotsList = boxDao.getActiveBoxes();

        // En modo edición, asegurarse de que los lotes de la venta original estén disponibles en el selector
        if (isEditing && !originalDetails.isEmpty()) {
            for (SaleDetail orig : originalDetails) {
                boolean found = false;
                for (BoxPurchase b : activeLotsList) {
                    if (b.getId() == orig.getCajaId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    BoxPurchase box = boxDao.getBoxById(orig.getCajaId());
                    if (box != null) {
                        activeLotsList.add(box);
                    }
                }
            }
        }

        if (activeLotsList.isEmpty()) {
            Toast.makeText(this, "No hay lotes con stock disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<BoxPurchase> lotsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, activeLotsList);
        spinnerLots.setAdapter(lotsAdapter);

        if (targetRestoredLotPos >= 0 && targetRestoredLotPos < activeLotsList.size()) {
            spinnerLots.setSelection(targetRestoredLotPos);
            selectedLot = activeLotsList.get(targetRestoredLotPos);
        }

        spinnerLots.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLot = activeLotsList.get(position);
                int stockEfectivo = getEffectiveAvailableStock(selectedLot);
                tilQuantity.setHelperText("Stock disponible: " + stockEfectivo + " resmas");

                if (isRestoringState) {
                    if (position != targetRestoredLotPos && targetRestoredLotPos >= 0 && targetRestoredLotPos < activeLotsList.size()) {
                        return;
                    }
                    isRestoringState = false;
                    isUpdatingPrice = true;
                    try {
                        if (restoredUnitPrice != null) {
                            etUnitPrice.setText(restoredUnitPrice);
                        }
                        if (restoredMargin != null) {
                            etMargin.setText(restoredMargin);
                        }
                        double unitPrice = 0.0;
                        try {
                            if (restoredUnitPrice != null && !restoredUnitPrice.isEmpty()) {
                                unitPrice = Double.parseDouble(restoredUnitPrice);
                            }
                        } catch (NumberFormatException ignored) {}
                        currentDynamicUnitPrice = unitPrice;

                        double unitCost = selectedLot.getPrecioCostoUnitario();
                        double utility = SaleDetail.calcularUtilidadUnitaria(unitPrice, unitCost);
                        double margin = 0.0;
                        try {
                            if (restoredMargin != null && !restoredMargin.isEmpty()) {
                                margin = Double.parseDouble(restoredMargin);
                            }
                        } catch (NumberFormatException ignored) {}

                        tvFormulaPreview.setText("Costo: " + currencyFormat.format(unitCost) +
                                " | Utilidad: " + currencyFormat.format(utility) +
                                " (" + String.format(Locale.getDefault(), "%.1f", margin) + "% margen)");
                        tvDynamicUnitPrice.setText(currencyFormat.format(unitPrice));
                    } finally {
                        isUpdatingPrice = false;
                    }
                } else {
                    recalculatePriceFromCurrentInputs();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedLot = null;
            }
        });
    }

    private int getEffectiveAvailableStock(BoxPurchase lot) {
        if (lot == null) return 0;
        int stock = lot.getResmasDisponibles();
        if (isEditing) {
            for (SaleDetail orig : originalDetails) {
                if (orig.getCajaId() == lot.getId()) {
                    stock += orig.getCantidad();
                }
            }
        }
        return stock;
    }

    private void recalculatePriceFromCurrentInputs() {
        if (selectedLot == null) {
            tvDynamicUnitPrice.setText("$0");
            currentDynamicUnitPrice = 0.0;
            return;
        }

        double unitCost = selectedLot.getPrecioCostoUnitario();
        String priceStr = etUnitPrice.getText().toString().trim();
        String marginStr = etMargin.getText().toString().trim();

        isUpdatingPrice = true;
        try {
            if (!marginStr.isEmpty()) {
                double margin = Double.parseDouble(marginStr);
                double price = SaleDetail.calcularPrecioVentaDesdeMargen(unitCost, margin);
                double utility = SaleDetail.calcularUtilidadUnitaria(price, unitCost);
                currentDynamicUnitPrice = price;

                etUnitPrice.setText(String.format(Locale.US, "%.2f", price));
                tvFormulaPreview.setText("Costo: " + currencyFormat.format(unitCost) +
                        " | Utilidad: " + currencyFormat.format(utility) +
                        " (" + String.format(Locale.getDefault(), "%.1f", margin) + "% margen)");
                tvDynamicUnitPrice.setText(currencyFormat.format(price));
            } else if (!priceStr.isEmpty()) {
                double price = Double.parseDouble(priceStr);
                currentDynamicUnitPrice = price;
                double margin = SaleDetail.calcularPorcentajeMargen(price, unitCost);
                double utility = SaleDetail.calcularUtilidadUnitaria(price, unitCost);

                etMargin.setText(String.format(Locale.US, "%.1f", margin));
                tvFormulaPreview.setText("Costo: " + currencyFormat.format(unitCost) +
                        " | Utilidad: " + currencyFormat.format(utility) +
                        " (" + String.format(Locale.getDefault(), "%.1f", margin) + "% margen)");
                tvDynamicUnitPrice.setText(currencyFormat.format(price));
            } else {
                double margin = 10.0;
                double price = SaleDetail.calcularPrecioVentaDesdeMargen(unitCost, margin);
                double utility = SaleDetail.calcularUtilidadUnitaria(price, unitCost);
                currentDynamicUnitPrice = price;

                etMargin.setText("10.0");
                etUnitPrice.setText(String.format(Locale.US, "%.2f", price));
                tvFormulaPreview.setText("Costo: " + currencyFormat.format(unitCost) +
                        " | Utilidad: " + currencyFormat.format(utility) + " (10.0% margen)");
                tvDynamicUnitPrice.setText(currencyFormat.format(price));
            }
        } catch (NumberFormatException ignored) {
        } finally {
            isUpdatingPrice = false;
        }
    }

    private void loadExistingSale() {
        Sale sale = saleDao.getSaleById(editSaleId);
        if (sale == null) {
            Toast.makeText(this, "No se encontró la venta a editar", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etSaleDate.setText(sale.getFecha());
        if (Sale.ESTADO_PAGADO.equalsIgnoreCase(sale.getEstadoPago())) {
            rbPagado.setChecked(true);
        } else {
            rbDebe.setChecked(true);
        }

        for (int i = 0; i < clientsList.size(); i++) {
            if (clientsList.get(i).getId() == sale.getClienteId()) {
                spinnerClients.setSelection(i);
                selectedClient = clientsList.get(i);
                break;
            }
        }

        List<SaleDetail> details = saleDao.getSaleDetails(editSaleId);
        originalDetails.clear();
        originalDetails.addAll(details);

        cartItems.clear();
        cartItems.addAll(details);
        cartAdapter.setItems(cartItems);
        updateCartSummary();

        // Refrescar selector de lotes con las cajas originales incluidas
        setupLotsSpinner();
    }

    private void addItemToCart() {
        if (selectedClient == null) {
            Toast.makeText(this, "Seleccione un cliente", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedLot == null) {
            Toast.makeText(this, "Seleccione un lote de resmas", Toast.LENGTH_SHORT).show();
            return;
        }

        String priceStr = etUnitPrice.getText().toString().trim();
        if (priceStr.isEmpty()) {
            Toast.makeText(this, "Ingrese el precio unitario de venta", Toast.LENGTH_SHORT).show();
            return;
        }
        double unitPrice;
        try {
            unitPrice = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio unitario inválido", Toast.LENGTH_SHORT).show();
            return;
        }
        if (unitPrice <= 0) {
            Toast.makeText(this, "El precio unitario debe ser mayor a 0", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etQuantity.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Ingrese la cantidad a vender", Toast.LENGTH_SHORT).show();
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Cantidad inválida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (quantity <= 0) {
            Toast.makeText(this, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
            return;
        }

        int alreadyInCart = 0;
        for (SaleDetail d : cartItems) {
            if (d.getCajaId() == selectedLot.getId()) {
                alreadyInCart += d.getCantidad();
            }
        }

        int maxStockAllowed = getEffectiveAvailableStock(selectedLot);
        if (quantity + alreadyInCart > maxStockAllowed) {
            Toast.makeText(this, "Stock insuficiente en el lote #" + selectedLot.getId() +
                    ". Disponible: " + (maxStockAllowed - alreadyInCart), Toast.LENGTH_LONG).show();
            return;
        }

        double unitCost = selectedLot.getPrecioCostoUnitario();
        double margin = SaleDetail.calcularPorcentajeMargen(unitPrice, unitCost);

        SaleDetail detail = new SaleDetail(selectedLot.getId(), quantity, unitPrice);
        detail.setTipoResma(selectedLot.getTipoResma());
        detail.setCostoUnitarioCaja(unitCost);
        detail.setMargenAplicado(margin);

        cartItems.add(detail);
        cartAdapter.setItems(cartItems);
        updateCartSummary();

        etQuantity.setText("1");
        Toast.makeText(this, "Ítem agregado a la venta", Toast.LENGTH_SHORT).show();
    }

    private void updateCartSummary() {
        double total = 0.0;
        for (SaleDetail d : cartItems) {
            total += d.getSubtotal();
        }
        tvTotalSaleAmount.setText(currencyFormat.format(total));
        tvEmptyCart.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
        btnConfirmSale.setEnabled(!cartItems.isEmpty());
    }

    private void confirmSale() {
        if (selectedClient == null) {
            Toast.makeText(this, "Seleccione un cliente", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Agregue al menos un producto a la venta", Toast.LENGTH_SHORT).show();
            return;
        }

        String saleDate = etSaleDate.getText().toString().trim();
        if (saleDate.isEmpty()) {
            saleDate = dateFormat.format(new Date());
        }

        String paymentStatus = rbPagado.isChecked() ? Sale.ESTADO_PAGADO : Sale.ESTADO_DEBE;

        double total = 0.0;
        for (SaleDetail d : cartItems) {
            total += d.getSubtotal();
        }

        Sale sale = new Sale(selectedClient.getId(), saleDate, total, paymentStatus);

        final String finalSaleDate = saleDate;
        final double finalTotal = total;
        final String finalPaymentStatus = paymentStatus;

        String dialogTitle = isEditing ? "Confirmar Edición de Venta" : "Confirmar Venta";
        String dialogMessage = "¿Deseas " + (isEditing ? "actualizar los cambios de esta venta para " : "registrar esta venta para ") +
                selectedClient.getNombre() + "?\n\nTotal: " + currencyFormat.format(finalTotal) +
                "\nEstado: " + finalPaymentStatus;

        new MaterialAlertDialogBuilder(this)
                .setTitle(dialogTitle)
                .setMessage(dialogMessage)
                .setPositiveButton("Confirmar y Guardar", (dialog, which) -> {
                    try {
                        if (isEditing) {
                            sale.setId(editSaleId);
                            boolean ok = saleDao.updateSaleWithDetails(sale, cartItems);
                            if (ok) {
                                Toast.makeText(this, "¡Venta #V-" + editSaleId + " actualizada con éxito!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Error al actualizar la venta", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            long saleId = saleDao.insertSaleWithDetails(sale, cartItems);
                            if (saleId != -1) {
                                Toast.makeText(this, "¡Venta #V-" + saleId + " registrada con éxito!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Error al registrar la venta", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Error en la transacción")
                                .setMessage(e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}

