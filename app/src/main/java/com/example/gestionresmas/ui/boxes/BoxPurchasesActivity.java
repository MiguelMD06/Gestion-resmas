package com.example.gestionresmas.ui.boxes;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.database.BoxPurchaseDao;
import com.example.gestionresmas.models.BoxPurchase;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoxPurchasesActivity extends AppCompatActivity {

    private BoxPurchaseDao boxDao;
    private BoxPurchaseAdapter adapter;
    private List<BoxPurchase> allBoxes = new ArrayList<>();

    private RecyclerView rvBoxes;
    private TextView tvEmptyBoxes;
    private ChipGroup chipGroupFilters;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_purchases);

        boxDao = new BoxPurchaseDao(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvBoxes = findViewById(R.id.rvBoxes);
        tvEmptyBoxes = findViewById(R.id.tvEmptyBoxes);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        ExtendedFloatingActionButton fabAddBox = findViewById(R.id.fabAddBox);

        rvBoxes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BoxPurchaseAdapter(this);
        rvBoxes.setAdapter(adapter);

        fabAddBox.setOnClickListener(v -> showAddBoxDialog());

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            applyFilters();
        });

        loadBoxes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBoxes();
    }

    private void loadBoxes() {
        allBoxes = boxDao.getAllBoxes();
        applyFilters();
    }

    private void applyFilters() {
        int checkedId = chipGroupFilters.getCheckedChipId();
        List<BoxPurchase> filtered = new ArrayList<>();

        if (checkedId == R.id.chipActive) {
            for (BoxPurchase b : allBoxes) {
                if (b.getResmasDisponibles() > 0) filtered.add(b);
            }
        } else if (checkedId == R.id.chipCarta) {
            for (BoxPurchase b : allBoxes) {
                if ("CARTA".equalsIgnoreCase(b.getTipoResma())) filtered.add(b);
            }
        } else if (checkedId == R.id.chipOficio) {
            for (BoxPurchase b : allBoxes) {
                if ("OFICIO".equalsIgnoreCase(b.getTipoResma())) filtered.add(b);
            }
        } else {
            filtered.addAll(allBoxes);
        }

        adapter.setBoxes(filtered);
        tvEmptyBoxes.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddBoxDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_box, null);
        RadioGroup rgPaperType = dialogView.findViewById(R.id.rgPaperType);
        RadioButton rbCarta = dialogView.findViewById(R.id.rbCarta);
        EditText etBoxPrice = dialogView.findViewById(R.id.etBoxPrice);
        EditText etReamsQuantity = dialogView.findViewById(R.id.etReamsQuantity);
        EditText etPurchaseDate = dialogView.findViewById(R.id.etPurchaseDate);
        TextView tvCalculatedUnitCost = dialogView.findViewById(R.id.tvCalculatedUnitCost);

        String today = dateFormat.format(new Date());
        etPurchaseDate.setText(today);

        final Calendar calendar = Calendar.getInstance();
        etPurchaseDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                etPurchaseDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double price = Double.parseDouble(etBoxPrice.getText().toString());
                    int qty = Integer.parseInt(etReamsQuantity.getText().toString());
                    if (qty > 0) {
                        double unitCost = price / qty;
                        tvCalculatedUnitCost.setText(currencyFormat.format(unitCost));
                    } else {
                        tvCalculatedUnitCost.setText("$0");
                    }
                } catch (Exception e) {
                    tvCalculatedUnitCost.setText("$0");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etBoxPrice.addTextChangedListener(watcher);
        etReamsQuantity.addTextChangedListener(watcher);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String priceStr = etBoxPrice.getText().toString().trim();
                    String qtyStr = etReamsQuantity.getText().toString().trim();
                    String dateStr = etPurchaseDate.getText().toString().trim();
                    String paperType = rbCarta.isChecked() ? "CARTA" : "OFICIO";

                    if (priceStr.isEmpty() || qtyStr.isEmpty()) {
                        Toast.makeText(this, "Complete el precio y la cantidad de resmas", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price = 0.0;
                    int qty = 0;
                    try {
                        price = Double.parseDouble(priceStr);
                        qty = Integer.parseInt(qtyStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Valores numéricos inválidos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (qty <= 0) {
                        Toast.makeText(this, "La cantidad de resmas debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BoxPurchase newBox = new BoxPurchase(paperType, dateStr, price, qty, qty);
                    long id = boxDao.insert(newBox);
                    if (id != -1) {
                        Toast.makeText(this, "Lote #" + id + " (" + paperType + ") registrado con éxito", Toast.LENGTH_SHORT).show();
                        loadBoxes();
                    } else {
                        Toast.makeText(this, "Error al registrar la compra de caja", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
