package com.danl.chessbluetooth.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.danl.chessbluetooth.BaseActivity;
import com.danl.chessbluetooth.BoardView;
import com.danl.chessbluetooth.PromotePieceListDialogFragment;
import com.danl.chessbluetooth.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cuckoochess.chess.Game;
import cuckoochess.chess.Move;
import cuckoochess.chess.Position;
import cuckoochess.chess.TextIO;

public class BluetoothGameActivity extends BaseActivity implements GUIInterface, DeviceListDialogFragment.Listener, PromotePieceListDialogFragment.Listener {

    private static final int REQUEST_ENABLE_BT = 0;

    private ChessController mChessController;

    private BluetoothAdapter mBluetoothAdapter;

    private BoardView mBoardView;
    private BluetoothGameService mGameService;
    private ImageButton mRestartButton;
    private ImageButton mUndoButton;
    private ImageButton mRedoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mBoardView = findViewById(R.id.board_view);
        mRestartButton = findViewById(R.id.restart);
        mUndoButton = findViewById(R.id.undo);
        mRedoButton = findViewById(R.id.redo);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не доступен.", Toast.LENGTH_LONG).show();
            finish();
        } else if (mBluetoothAdapter.isEnabled()) {
            setupGame();
        } else {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mGameService.stop();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGame() {
        mChessController = new ChessController(this);
        mGameService = new BluetoothGameService(new BluetoothGameHandler(this));

        mBoardView.setOnTouchListener((v, event) -> {
            if (mGameService.getState() != BluetoothGameService.STATE_CONNECTED
                    || !mChessController.isPlayerTurn()
                    || mChessController.getGame().getGameState() != Game.GameState.ALIVE) {
                return false;
            }
            Move move = mBoardView.move(event);
            if (move != null && mChessController.humanMove(move)) {
                sendMove(move);
            }
            return true;
        });
        mRestartButton.setOnClickListener(v -> {
            if (mGameService.getState() == BluetoothGameService.STATE_CONNECTED && mChessController.getGame().getLastMove() != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Вы уверены что хотите начать заново?").setPositiveButton("Да",
                        (dialog, which) -> mGameService.write(BluetoothGameService.MESSAGE_REQUEST_RESTART)).setNegativeButton("Нет", null);
                showDialog(builder.create());
            }
        });
        mUndoButton.setOnClickListener(v -> {
            if (mGameService.getState() == BluetoothGameService.STATE_CONNECTED) {
                if (!mChessController.isPlayerTurn() && mChessController.getGame().getLastMove() != null) {
                    mGameService.write(BluetoothGameService.MESSAGE_REQUEST_UNDO);
                }
            }
        });
        mRedoButton.setOnClickListener(v -> {
            if (mGameService.getState() == BluetoothGameService.STATE_CONNECTED) {
                if (mChessController.isPlayerTurn() && mChessController.getGame().getGameState() == Game.GameState.ALIVE) {
                    mChessController.redoMove();
                    mGameService.write(BluetoothGameService.MESSAGE_REDO);
                }
            }
        });
        mGameService.start();
    }

    private void setStatus(int resId) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(resId);
        }
    }

    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subTitle);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                setupGame();
            } else {
                Toast.makeText(this, "Bluetooth не был включен.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.device_list:
                if (mBluetoothAdapter.getBondedDevices().isEmpty()) {
                    Toast.makeText(this, "Нет сопряженных устройств", Toast.LENGTH_SHORT).show();
                } else {
                    new DeviceListDialogFragment().showNow(getSupportFragmentManager(), null);
                }
                return true;
            case android.R.id.home:
                if (mGameService.getState() == BluetoothGameService.STATE_CONNECTED) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Вы уверены что хотите разорвать соединение и выйти?")
                            .setNegativeButton("Нет", null)
                            .setPositiveButton("Да", (dialog, which) -> finish());
                    showDialog(builder.create());
                } else {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setPosition(Position pos) {
        mBoardView.setPosition(pos);
    }

    @Override
    public void setSelection(int sq) {
        mBoardView.setSelectedSquare(sq);
    }

    @Override
    public void setStatusString(String str) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(str);
        }
    }

    @Override
    public void requestPromotePiece() {
        PromotePieceListDialogFragment.newInstance(mChessController.isServer()).showNow(getSupportFragmentManager(), null);
    }

    @Override
    public void setLastMove(Move move) {
        mBoardView.setLastMove(move);
    }

    @Override
    public void setGameStateString(String gameStateString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(gameStateString).setPositiveButton("ОК", null);
        showDialog(builder.create());
    }

    @Override
    public void onDeviceSelected(BluetoothDevice item) {
        if (mGameService.getState() == BluetoothGameService.STATE_CONNECTED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Вы уверены что хотите разорвать текущее соединение?")
                    .setNegativeButton("Нет", null)
                    .setPositiveButton("Да", (dialog, which) -> mGameService.connect(item));
            showDialog(builder.create());
        } else {
            mGameService.connect(item);
        }
    }

    @Override
    public void onPieceSelected(int item) {
        sendMove(mChessController.reportPromotePiece(item));
    }

    private void sendMove(Move move) {
        mGameService.write(BluetoothGameService.MESSAGE_MOVE, TextIO.moveToUCIString(move).getBytes());
    }

    private void sendPosHistory(List<String> posHistory) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : posHistory) {
            stringBuilder.append(string).append(";");
        }
        mGameService.write(BluetoothGameService.MESSAGE_POS_HISTORY, stringBuilder.toString().getBytes());
    }

    private AlertDialog undoDialog = null, restartDialog = null;

    private void showConfirmUndoDialog() {
        if (undoDialog == null) {
            undoDialog = showDialog(new AlertDialog.Builder(this).setMessage("Противник хочет отменить ход").setPositiveButton("Да", (dialog, which) -> {
                mChessController.takeBackMove();
                mGameService.write(BluetoothGameService.MESSAGE_UNDO);
            }).setNegativeButton("Нет", null).create());
        } else {
            if (!undoDialog.isShowing() && (restartDialog == null || !restartDialog.isShowing())) {
                showDialog(undoDialog);
            }
        }
    }

    private void showConfirmRestartDialog() {
        if (restartDialog == null) {
            restartDialog = showDialog(new AlertDialog.Builder(this).setMessage("Противник хочет начать заново").setPositiveButton("Да", (dialog, which) -> {
                mChessController.newGame();
                mGameService.write(BluetoothGameService.MESSAGE_RESTART);
            }).setNegativeButton("Нет", null).create());
        } else {
            if (!restartDialog.isShowing() && (undoDialog == null || !undoDialog.isShowing())) {
                showDialog(restartDialog);
            }
        }
    }

    private static class BluetoothGameHandler extends Handler {

        private WeakReference<BluetoothGameActivity> mActivity;
        private String mConnectedDeviceName = null;

        BluetoothGameHandler(BluetoothGameActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            BluetoothGameActivity activity = mActivity.get();
            if (activity == null) {
                return;
            }

            switch (msg.what) {
                case BluetoothGameService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothGameService.STATE_CONNECTED:
                            activity.setStatus(activity.getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothGameService.STATE_CONNECTING:
                            activity.setStatus(R.string.title_connecting);
                            break;
                        case BluetoothGameService.STATE_LISTEN:
                        case BluetoothGameService.STATE_NONE:
                            activity.setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case BluetoothGameService.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(BluetoothGameService.DEVICE_NAME);
                    Toast.makeText(activity, "Подключен к "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothGameService.MESSAGE_TOAST:
                    Toast.makeText(activity, msg.getData().getString(BluetoothGameService.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothGameService.MESSAGE_MOVE:
                    activity.mChessController.humanMove(TextIO.uciStringToMove((String) msg.obj));
                    break;
                case BluetoothGameService.MESSAGE_REDO:
                    activity.mChessController.redoMove();
                    break;
                case BluetoothGameService.MESSAGE_UNDO:
                    activity.mChessController.takeBackMove();
                    break;
                case BluetoothGameService.MESSAGE_RESTART:
                    activity.mChessController.newGame();
                    break;
                case BluetoothGameService.MESSAGE_REQUEST_UNDO:
                    activity.showConfirmUndoDialog();
                    break;
                case BluetoothGameService.MESSAGE_REQUEST_RESTART:
                    activity.showConfirmRestartDialog();
                    break;
                case BluetoothGameService.MESSAGE_POS_HISTORY:
                    String posHistStr = (String) msg.obj;
                    activity.mChessController.setPosHistory(Arrays.asList(posHistStr.split(";")));
                    activity.mChessController.startGame();
                    break;
                case BluetoothGameService.MESSAGE_SERVER:
                    boolean server = (boolean) msg.obj;
                    activity.mBoardView.setFlipped(!server);
                    activity.mChessController.newGame(server);
                    if (server) {
                        List<String> posHistory = new ArrayList<>();
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                        posHistory.add(preferences.getString("bluetooth_startFEN", ""));
                        posHistory.add(preferences.getString("bluetooth_moves", ""));
                        posHistory.add(preferences.getString("bluetooth_num_undo", "0"));

                        activity.sendPosHistory(posHistory);
                        activity.mChessController.setPosHistory(posHistory);
                        activity.mChessController.startGame();
                    }
                    break;
                case BluetoothGameService.MESSAGE_SAVE_POS_HISTORY:
                    if (activity.mChessController.isServer()) {
                        List<String> posHistory = activity.mChessController.getPosHistory();
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
                        editor.putString("bluetooth_startFEN", posHistory.get(0));
                        editor.putString("bluetooth_moves", posHistory.get(1));
                        editor.putString("bluetooth_num_undo", posHistory.get(2));
                        editor.apply();
                    }
                    break;
            }
        }
    }
}
