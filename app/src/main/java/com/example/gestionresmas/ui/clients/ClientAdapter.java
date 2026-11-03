package com.example.gestionresmas.ui.clients;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.database.SaleDao;
import com.example.gestionresmas.models.Client;
import com.example.gestionresmas.models.Sale;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ClientViewHolder> {

    public interface OnClientActionListener {
        void onEdit(Client client);
        void onViewHistory(Client client);
        void onDelete(Client client);
    }

    private final Context context;
    private List<Client> clientList = new ArrayList<>();
    private final SaleDao saleDao;
    private final NumberFormat currencyFormat;
    private OnClientActionListener listener;

    public ClientAdapter(Context context) {
        this.context = context;
        this.saleDao = new SaleDao(context);
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
    }

    public void setClients(List<Client> clients) {
        this.clientList = clients != null ? clients : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnClientActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client, parent, false);
        return new ClientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        Client client = clientList.get(position);
        holder.tvClientName.setText(client.getNombre());
        holder.tvClientMargin.setVisibility(View.GONE);

        if (client.getTelefono() != null && !client.getTelefono().isEmpty()) {
            holder.tvClientPhone.setText(client.getTelefono());
            holder.tvClientPhone.setVisibility(View.VISIBLE);
        } else {
            holder.tvClientPhone.setText("Sin teléfono");
        }

        // Consultar si este cliente tiene deuda pendiente
        List<Sale> debtSales = saleDao.getDebtSalesByClient(client.getId());
        double totalDeuda = 0.0;
        for (Sale s : debtSales) {
            totalDeuda += s.getTotalVenta();
        }

        if (totalDeuda > 0) {
            holder.tvClientDebtTag.setText("Debe: " + currencyFormat.format(totalDeuda));
            holder.tvClientDebtTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvClientDebtTag.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onViewHistory(client);
        });

        holder.btnEditClient.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(client);
        });

        holder.btnViewClientHistory.setOnClickListener(v -> {
            if (listener != null) listener.onViewHistory(client);
        });

        holder.btnDeleteClient.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(client);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(client);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return clientList.size();
    }

    static class ClientViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName, tvClientMargin, tvClientPhone, tvClientDebtTag;
        MaterialButton btnViewClientHistory, btnEditClient, btnDeleteClient;

        public ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvClientMargin = itemView.findViewById(R.id.tvClientMargin);
            tvClientPhone = itemView.findViewById(R.id.tvClientPhone);
            tvClientDebtTag = itemView.findViewById(R.id.tvClientDebtTag);
            btnViewClientHistory = itemView.findViewById(R.id.btnViewClientHistory);
            btnEditClient = itemView.findViewById(R.id.btnEditClient);
            btnDeleteClient = itemView.findViewById(R.id.btnDeleteClient);
        }
    }
}
