package com.danl.chessbluetooth.bluetooth;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danl.chessbluetooth.BaseDialogFragment;
import com.danl.chessbluetooth.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceListDialogFragment extends BaseDialogFragment {

    private Listener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.fragment_list_dialog, null);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new DeviceAdapter(new ArrayList<>(BluetoothAdapter.getDefaultAdapter().getBondedDevices())));

        builder.setTitle("Сопряженные устройства").setView(view);
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

    private class DeviceAdapter extends RecyclerView.Adapter<ViewHolder> {

        List<BluetoothDevice> mBluetoothDevices;

        DeviceAdapter(List<BluetoothDevice> bluetoothDevices) {
            mBluetoothDevices = bluetoothDevices;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDevice bluetoothDevice = mBluetoothDevices.get(position);

            holder.mName.setText(bluetoothDevice.getName());
            holder.mAddress.setText(bluetoothDevice.getAddress());
            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDeviceSelected(bluetoothDevice);
                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mBluetoothDevices.size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView mName;
        TextView mAddress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mAddress = itemView.findViewById(R.id.address);
        }
    }

    public interface Listener {
        void onDeviceSelected(BluetoothDevice device);
    }
}
