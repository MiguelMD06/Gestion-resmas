package com.example.gestionresmas.ui.boxes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.models.BoxPurchase;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BoxPurchaseAdapter extends RecyclerView.Adapter<BoxPurchaseAdapter.BoxViewHolder> {

    public interface OnBoxClickListener {
        void onBoxClick(BoxPurchase box);
    }

    private final Context context;
    private List<BoxPurchase> boxes = new ArrayList<>();
    private final NumberFormat currencyFormat;
    private OnBoxClickListener listener;

    public BoxPurchaseAdapter(Context context) {
        this.context = context;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
    }

    public void setBoxes(List<BoxPurchase> boxes) {
        this.boxes = boxes != null ? boxes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnBoxClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BoxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_box_purchase, parent, false);
        return new BoxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BoxViewHolder holder, int position) {
        BoxPurchase box = boxes.get(position);

        holder.tvLotId.setText("Lote #" + box.getId());
        holder.tvBoxTypeBadge.setText(box.getTipoResma());

        if ("CARTA".equalsIgnoreCase(box.getTipoResma())) {
            holder.tvBoxTypeBadge.setBackgroundResource(R.drawable.bg_badge_primary);
            holder.tvBoxTypeBadge.setTextColor(context.getColor(R.color.on_primary_container));
        } else {
            holder.tvBoxTypeBadge.setBackgroundResource(R.drawable.bg_badge_secondary);
            holder.tvBoxTypeBadge.setTextColor(context.getColor(R.color.on_secondary_container));
        }

        holder.tvUnitCost.setText(currencyFormat.format(box.getPrecioCostoUnitario()));
        holder.tvBoxPrice.setText(currencyFormat.format(box.getPrecioCompraCaja()));
        holder.tvPurchaseDate.setText(box.getFechaCompra());

        holder.tvStockRemaining.setText("Stock: " + box.getResmasDisponibles() + " / " + box.getCantidadResmasTotal() + " resmas");

        // Progress bar
        int progress = 0;
        if (box.getCantidadResmasTotal() > 0) {
            progress = (box.getResmasDisponibles() * 100) / box.getCantidadResmasTotal();
        }
        holder.progressStock.setProgress(progress);

        if (box.getResmasDisponibles() > 0) {
            holder.tvLotStatusBadge.setText("Disponible");
            holder.tvLotStatusBadge.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvLotStatusBadge.setTextColor(context.getColor(R.color.on_success_container));
        } else {
            holder.tvLotStatusBadge.setText("Agotado");
            holder.tvLotStatusBadge.setBackgroundResource(R.drawable.bg_badge_danger);
            holder.tvLotStatusBadge.setTextColor(context.getColor(R.color.on_danger_container));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBoxClick(box);
            }
        });
    }

    @Override
    public int getItemCount() {
        return boxes.size();
    }

    static class BoxViewHolder extends RecyclerView.ViewHolder {
        TextView tvLotId, tvBoxTypeBadge, tvLotStatusBadge;
        TextView tvUnitCost, tvBoxPrice, tvStockRemaining, tvPurchaseDate;
        LinearProgressIndicator progressStock;

        public BoxViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLotId = itemView.findViewById(R.id.tvLotId);
            tvBoxTypeBadge = itemView.findViewById(R.id.tvBoxTypeBadge);
            tvLotStatusBadge = itemView.findViewById(R.id.tvLotStatusBadge);
            tvUnitCost = itemView.findViewById(R.id.tvUnitCost);
            tvBoxPrice = itemView.findViewById(R.id.tvBoxPrice);
            tvStockRemaining = itemView.findViewById(R.id.tvStockRemaining);
            tvPurchaseDate = itemView.findViewById(R.id.tvPurchaseDate);
            progressStock = itemView.findViewById(R.id.progressStock);
        }
    }
}
