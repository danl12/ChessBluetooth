package com.danl.chessbluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.danl.chessbluetooth.bluetooth.BluetoothGameActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button playButton = findViewById(R.id.play_button);
        Button settingsButton = findViewById(R.id.settings_button);

        playButton.setOnClickListener(v -> showPlayDialog());
        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void showPlayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(new String[]{"Играть с компьютером", "Играть по Bluetooth"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    startActivity(new Intent(this, ComputerGameActivity.class));
                    break;
                case 1:
                    startActivity(new Intent(this, BluetoothGameActivity.class));
                    break;
            }
        });
        showDialog(builder.create());
    }
}
