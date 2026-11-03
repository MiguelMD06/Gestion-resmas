package com.example.gestionresmas.ui.sales;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.database.SaleDao;
import com.example.gestionresmas.models.Sale;
import com.example.gestionresmas.models.SaleDetail;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.SaleViewHolder> {

    public interface OnSaleActionListener {
        void onSaleClick(Sale sale);
        void onMarkAsPaid(Sale sale);
    }

    private final Context context;
    private List<Sale> sales = new ArrayList<>();
    private final SaleDao saleDao;
    private final NumberFormat currencyFormat;
    private OnSaleActionListener listener;
    private boolean showPayButton = false;

    public SalesAdapter(Context context) {
        this.context = context;
        this.saleDao = new SaleDao(context);
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales != null ? sales : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnSaleActionListener listener) {
        this.listener = listener;
    }

    public void setShowPayButton(boolean showPayButton) {
        this.showPayButton = showPayButton;
    }

    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false);
        return new SaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SaleViewHolder holder, int position) {
        Sale sale = sales.get(position);
        holder.tvSaleId.setText("#V-" + sale.getId());
        holder.tvClientName.setText(sale.getClienteNombre().isEmpty() ? "Cliente #" + sale.getClienteId() : sale.getClienteNombre());
        holder.tvSaleDate.setText(sale.getFecha());
        holder.tvSaleTotal.setText(currencyFormat.format(sale.getTotalVenta()));

        if (sale.isPagado()) {
            holder.tvPaymentStatus.setText(context.getString(R.string.paid));
            holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvPaymentStatus.setTextColor(context.getColor(R.color.on_success_container));
            holder.tvSaleDebtBreakdown.setVisibility(View.GONE);
            holder.btnMarkAsPaid.setVisibility(View.GONE);
        } else {
            if (sale.tieneAbonosParciales()) {
                holder.tvPaymentStatus.setText("ABONADO");
                holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_badge_warning);
                holder.tvPaymentStatus.setTextColor(context.getColor(R.color.on_warning_container));

                holder.tvSaleDebtBreakdown.setText("Abonado: " + currencyFormat.format(sale.getMontoPagado()) +
                        "  •  Resta: " + currencyFormat.format(sale.getSaldoPendiente()));
                holder.tvSaleDebtBreakdown.setVisibility(View.VISIBLE);
            } else {
                holder.tvPaymentStatus.setText(context.getString(R.string.debt));
                holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_badge_danger);
                holder.tvPaymentStatus.setTextColor(context.getColor(R.color.on_danger_container));

                holder.tvSaleDebtBreakdown.setText("Saldo pendiente: " + currencyFormat.format(sale.getSaldoPendiente()));
                holder.tvSaleDebtBreakdown.setVisibility(View.VISIBLE);
            }

            if (showPayButton) {
                holder.btnMarkAsPaid.setVisibility(View.VISIBLE);
                holder.btnMarkAsPaid.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMarkAsPaid(sale);
                    }
                });
            } else {
                holder.btnMarkAsPaid.setVisibility(View.GONE);
            }
        }

        // Cargar resumen de ítems
        List<SaleDetail> details = saleDao.getSaleDetails(sale.getId());
        if (details != null && !details.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < details.size(); i++) {
                SaleDetail d = details.get(i);
                if (i > 0) sb.append(" | ");
                sb.append(d.getCantidad()).append(" resmas ")
                        .append(d.getTipoResma().isEmpty() ? "Lote #" + d.getCajaId() : d.getTipoResma());
            }
            holder.tvSaleItemsSummary.setText(sb.toString());
            holder.tvSaleItemsSummary.setVisibility(View.VISIBLE);
        } else {
            holder.tvSaleItemsSummary.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSaleClick(sale);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sales.size();
    }

    static class SaleViewHolder extends RecyclerView.ViewHolder {
        TextView tvSaleId, tvClientName, tvPaymentStatus, tvSaleDate, tvSaleTotal, tvSaleItemsSummary, tvSaleDebtBreakdown;
        MaterialButton btnMarkAsPaid;

        public SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSaleId = itemView.findViewById(R.id.tvSaleId);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            tvSaleDate = itemView.findViewById(R.id.tvSaleDate);
            tvSaleTotal = itemView.findViewById(R.id.tvSaleTotal);
            tvSaleItemsSummary = itemView.findViewById(R.id.tvSaleItemsSummary);
            tvSaleDebtBreakdown = itemView.findViewById(R.id.tvSaleDebtBreakdown);
            btnMarkAsPaid = itemView.findViewById(R.id.btnMarkAsPaid);
        }
    }
}
