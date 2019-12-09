package com.danl.chessbluetooth.bluetooth;

import cuckoochess.chess.Move;
import cuckoochess.chess.Position;

public interface GUIInterface {

    /** Update the displayed board position. */
    void setPosition(Position pos);

    /** Mark square i as selected. Set to -1 to clear selection. */
    void setSelection(int sq);

    /** Set the status text. */
    void setStatusString(String str);

    /** Ask what to promote a pawn to. Should call reportPromotePiece() when done. */
    void requestPromotePiece();

    void setLastMove(Move move);

    void setGameStateString(String gameStateString);
}
