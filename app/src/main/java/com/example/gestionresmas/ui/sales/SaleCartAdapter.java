package com.example.gestionresmas.ui.sales;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionresmas.R;
import com.example.gestionresmas.models.SaleDetail;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SaleCartAdapter extends RecyclerView.Adapter<SaleCartAdapter.CartViewHolder> {

    public interface OnRemoveItemListener {
        void onRemoveItem(int position, SaleDetail detail);
    }

    private final Context context;
    private List<SaleDetail> items = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
    private OnRemoveItemListener listener;

    public SaleCartAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<SaleDetail> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnRemoveItemListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        SaleDetail item = items.get(position);
        holder.tvCartItemTitle.setText(item.getCantidad() + " resmas " + item.getTipoResma() + " (Lote #" + item.getCajaId() + ")");
        holder.tvCartItemPricing.setText(item.getCantidad() + " x " + currencyFormat.format(item.getPrecioUnitarioAplicado()) + " c/u (+" + String.format(Locale.getDefault(), "%.1f", item.getMargenAplicado()) + "% margen)");
        holder.tvCartItemSubtotal.setText(currencyFormat.format(item.getSubtotal()));

        holder.btnRemoveCartItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveItem(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvCartItemTitle, tvCartItemPricing, tvCartItemSubtotal;
        ImageButton btnRemoveCartItem;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCartItemTitle = itemView.findViewById(R.id.tvCartItemTitle);
            tvCartItemPricing = itemView.findViewById(R.id.tvCartItemPricing);
            tvCartItemSubtotal = itemView.findViewById(R.id.tvCartItemSubtotal);
            btnRemoveCartItem = itemView.findViewById(R.id.btnRemoveCartItem);
        }
    }
}
