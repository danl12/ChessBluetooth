package com.danl.chessbluetooth;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PromotePieceListDialogFragment extends BaseDialogFragment {

    private static final String KEY_WHITE = "white";

    private boolean white;

    private Listener mListener;

    public static PromotePieceListDialogFragment newInstance(boolean white) {
        Bundle args = new Bundle();
        args.putBoolean(KEY_WHITE, white);

        PromotePieceListDialogFragment fragment = new PromotePieceListDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            white = args.getBoolean(KEY_WHITE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.fragment_list_dialog, null);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new PromotionPieceAdapter());

        builder.setTitle("Повысить пешку до?").setView(view);
        AlertDialog dialog = builder.create();
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (Listener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class PromotionPieceAdapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.promote_piece_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            switch (position) {
                case 0:
                    holder.mName.setText("Ферзь");
                    holder.mImage.setImageResource(white ? R.drawable.light_queen : R.drawable.dark_queen);
                    break;
                case 1:
                    holder.mName.setText("Ладья");
                    holder.mImage.setImageResource(white ? R.drawable.light_rook : R.drawable.dark_rook);
                    break;
                case 2:
                    holder.mName.setText("Слон");
                    holder.mImage.setImageResource(white ? R.drawable.light_bishop : R.drawable.dark_bishop);
                    break;
                case 3:
                    holder.mName.setText("Конь");
                    holder.mImage.setImageResource(white ? R.drawable.light_knight : R.drawable.dark_knight);
                    break;
            }

            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onPieceSelected(position);
                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView mName;
        ImageView mImage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mImage = itemView.findViewById(R.id.image);
        }
    }

    public interface Listener {
        void onPieceSelected(int piece);
    }
}
