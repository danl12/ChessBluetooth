package com.danl.chessbluetooth.bluetooth;

import java.util.List;

import cuckoochess.chess.ChessParseError;
import cuckoochess.chess.Game;
import cuckoochess.chess.Game.GameState;
import cuckoochess.chess.HumanPlayer;
import cuckoochess.chess.Move;
import cuckoochess.chess.MoveGen;
import cuckoochess.chess.Piece;
import cuckoochess.chess.Position;
import cuckoochess.chess.TextIO;

class ChessController {

    private Game game;
    private GUIInterface gui;
    private boolean server;

    ChessController(GUIInterface gui) {
        this.gui = gui;
    }

    final void newGame(boolean server) {
        this.server = server;
        game = new Game(new HumanPlayer(), new HumanPlayer());
    }

    final void newGame() {
        game = new Game(new HumanPlayer(), new HumanPlayer());
        startGame();
    }

    final void startGame() {
        gui.setSelection(-1);
        updateGUI();
    }

    Game getGame() {
        return game;
    }

    boolean isServer() {
        return server;
    }

    final void setPosHistory(List<String> posHistStr) {
        try {
            String fen = posHistStr.get(0);
            Position pos = TextIO.readFEN(fen);
            game.processString("new");
            game.pos = pos;
            for (String s : posHistStr.get(1).split(" ")) {
                game.processString(s);
            }
            int numUndo = Integer.parseInt(posHistStr.get(2));
            for (int i = 0; i < numUndo; i++) {
                game.processString("undo");
            }
        } catch (ChessParseError e) {
            // Just ignore invalid positions
        }
    }

    final List<String> getPosHistory() {
        return game.getPosHistory();
    }

    final boolean isPlayerTurn() {
        return game.pos.whiteMove == server;
    }

    final void takeBackMove() {
        game.processString("undo");
        updateGUI();
    }

    final void redoMove() {
        game.processString("redo");
        updateGUI();
    }

    final boolean humanMove(Move m) {
        boolean result = doMove(m);
        if (result) {
            updateGUI();
        }
        gui.setSelection(-1);
        return result;
    }

    private Move promoteMove;

    final Move reportPromotePiece(int choice) {
        final boolean white = game.pos.whiteMove;
        int promoteTo;
        switch (choice) {
            case 1:
                promoteTo = white ? Piece.WROOK : Piece.BROOK;
                break;
            case 2:
                promoteTo = white ? Piece.WBISHOP : Piece.BBISHOP;
                break;
            case 3:
                promoteTo = white ? Piece.WKNIGHT : Piece.BKNIGHT;
                break;
            default:
                promoteTo = white ? Piece.WQUEEN : Piece.BQUEEN;
                break;
        }
        promoteMove.promoteTo = promoteTo;
        Move m = promoteMove;
        promoteMove = null;
        humanMove(m);
        return m;
    }

    /**
     * Move a piece from one square to another.
     *
     * @return True if the move was legal, false otherwise.
     */
    private boolean doMove(Move move) {
        Position pos = game.pos;
        MoveGen.MoveList moves = new MoveGen().pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, moves);
        int promoteTo = move.promoteTo;
        for (int mi = 0; mi < moves.size; mi++) {
            Move m = moves.m[mi];
            if ((m.from == move.from) && (m.to == move.to)) {
                if ((m.promoteTo != Piece.EMPTY) && (promoteTo == Piece.EMPTY)) {
                    promoteMove = m;
                    gui.requestPromotePiece();
                    return false;
                }
                if (m.promoteTo == promoteTo) {
                    String strMove = TextIO.moveToString(pos, m, false);
                    game.processString(strMove);
                    return true;
                }
            }
        }
        return false;
    }

    private void updateGUI() {
        setStatusString();
        gui.setPosition(game.pos);
        gui.setLastMove(game.getLastMove());
    }

    private void setStatusString() {
        String str = game.pos.whiteMove ? "Ход белых" : "Ход черных";
        if (game.getGameState() != GameState.ALIVE) {
            str = game.getGameStateString();
            gui.setGameStateString(str);
        }
        gui.setStatusString(str);
    }
}
