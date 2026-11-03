package com.example.gestionresmas.ui.debt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.models.ClientDebtSummary;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.DebtViewHolder> {

    public interface OnDebtClickListener {
        void onDebtClick(ClientDebtSummary debtSummary);
        void onRegisterPaymentClick(ClientDebtSummary debtSummary);
    }

    private final Context context;
    private List<ClientDebtSummary> debtList = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
    private OnDebtClickListener listener;

    public DebtAdapter(Context context) {
        this.context = context;
    }

    public void setDebts(List<ClientDebtSummary> debts) {
        this.debtList = debts != null ? debts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnDebtClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt_client, parent, false);
        return new DebtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        ClientDebtSummary item = debtList.get(position);

        holder.tvDebtClientName.setText(item.getClienteNombre());
        holder.tvDebtAmount.setText(currencyFormat.format(item.getTotalDeuda()));
        holder.tvDebtClientPhone.setText(item.getClienteTelefono().isEmpty() ? "Sin teléfono" : item.getClienteTelefono());
        holder.tvDebtSalesCount.setText(item.getCantidadVentasDebe() + (item.getCantidadVentasDebe() == 1 ? " venta pendiente" : " ventas pendientes"));

        if (item.getTotalAbonado() > 0.001) {
            holder.tvDebtPaidSummary.setText("Abonado: " + currencyFormat.format(item.getTotalAbonado()));
            holder.tvDebtPaidSummary.setVisibility(View.VISIBLE);
        } else {
            holder.tvDebtPaidSummary.setVisibility(View.GONE);
        }

        holder.btnRegisterClientPayment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRegisterPaymentClick(item);
            }
        });

        holder.btnViewClientDebts.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDebtClick(item);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDebtClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return debtList.size();
    }

    static class DebtViewHolder extends RecyclerView.ViewHolder {
        TextView tvDebtClientName, tvDebtAmount, tvDebtClientPhone, tvDebtSalesCount, tvDebtPaidSummary;
        MaterialButton btnRegisterClientPayment, btnViewClientDebts;

        public DebtViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDebtClientName = itemView.findViewById(R.id.tvDebtClientName);
            tvDebtAmount = itemView.findViewById(R.id.tvDebtAmount);
            tvDebtClientPhone = itemView.findViewById(R.id.tvDebtClientPhone);
            tvDebtSalesCount = itemView.findViewById(R.id.tvDebtSalesCount);
            tvDebtPaidSummary = itemView.findViewById(R.id.tvDebtPaidSummary);
            btnRegisterClientPayment = itemView.findViewById(R.id.btnRegisterClientPayment);
            btnViewClientDebts = itemView.findViewById(R.id.btnViewClientDebts);
        }
    }
}
