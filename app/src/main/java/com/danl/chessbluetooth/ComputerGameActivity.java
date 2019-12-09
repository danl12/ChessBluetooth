package com.danl.chessbluetooth;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import cuckoochess.chess.Game;
import cuckoochess.chess.Move;
import cuckoochess.chess.Position;
import cuckoochess.guibase.ChessController;
import cuckoochess.guibase.GUIInterface;

public class ComputerGameActivity extends BaseActivity implements GUIInterface {

    private int mTimeLimit;
    private int mStrength;

    private ChessController mChessController;

    private BoardView mBoardView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mBoardView = findViewById(R.id.board_view);
        ImageView restartButton = findViewById(R.id.restart);
        ImageView undoButton = findViewById(R.id.undo);
        ImageView redoButton = findViewById(R.id.redo);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mTimeLimit = Math.max(25, preferences.getInt("computer_time_limit", 0) * 1000);
        mStrength = preferences.getInt("computer_strength", 5) * 100;

        mBoardView.setOnTouchListener((v, event) -> {
            if (mChessController.game == null
                    || mChessController.game.getGameState() != Game.GameState.ALIVE) {
                return false;
            }

            Move move = mBoardView.move(event);
            if (move != null) {
                mChessController.humanMove(move);
            }
            return true;
        });
        restartButton.setOnClickListener(v -> {
            if (mChessController.game != null && mChessController.game.getLastMove() != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Вы уверены что хотите начать заново?").setPositiveButton("Да",
                        (dialog, which) -> new Thread(() -> {
                            mChessController.newGame(true, 16, false, mStrength);
                            mChessController.startGame();
                        }).start()).setNegativeButton("Нет", null);
                showDialog(builder.create());
            }
        });
        undoButton.setOnClickListener(v -> {
            if (mChessController.game != null) {
                mChessController.takeBackMove();
            }
        });
        redoButton.setOnClickListener(v -> {
            if (mChessController.game != null) {
                mChessController.redoMove();
            }
        });

        mChessController = new ChessController(this);
        new Thread(() -> {
            mChessController.newGame(true, 16, false, mStrength);

            List<String> posHistory = new ArrayList<>();
            posHistory.add(preferences.getString("computer_startFEN", ""));
            posHistory.add(preferences.getString("computer_moves", ""));
            posHistory.add(preferences.getString("computer_num_undo", ""));

            mChessController.setPosHistory(posHistory);
            mChessController.startGame();
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mChessController.stopComputerThinking();

        List<String> posHistory = mChessController.getPosHistory();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("computer_startFEN", posHistory.get(0));
        editor.putString("computer_moves", posHistory.get(1));
        editor.putString("computer_num_undo", posHistory.get(2));
        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mChessController.game != null && mChessController.game.getGameState() == Game.GameState.ALIVE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Вы уверены что хотите выйти?")
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
        runOnUIThread(() -> {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(str);
            }
        });
    }

    @Override
    public int timeLimit() {
        return mTimeLimit;
    }

    @Override
    public void requestPromotePiece() {
        runOnUIThread(() -> PromotePieceListDialogFragment.newInstance(mChessController.humanIsWhite).showNow(getSupportFragmentManager(), null));
    }

    @Override
    public void runOnUIThread(Runnable runnable) {
        runOnUiThread(runnable);
    }

    @Override
    public void setLastMove(Move move) {
        mBoardView.setLastMove(move);
    }

    private AlertDialog mGameStateDialog;

    @Override
    public void setGameStateString(String gameStateString) {
        runOnUIThread(() -> {
            if (mGameStateDialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(gameStateString).setPositiveButton("ОК", null);
                mGameStateDialog = showDialog(builder.create());
            } else if (!mGameStateDialog.isShowing()) {
                showDialog(mGameStateDialog);
            }
        });
    }
}
