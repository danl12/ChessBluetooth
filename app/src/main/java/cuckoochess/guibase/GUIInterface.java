/*
    CuckooChess - A java chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package cuckoochess.guibase;

import cuckoochess.chess.Move;
import cuckoochess.chess.Position;

public interface GUIInterface {

    /** Update the displayed board position. */
    void setPosition(Position pos);

    /** Mark square i as selected. Set to -1 to clear selection. */
    void setSelection(int sq);

    /** Set the status text. */
    void setStatusString(String str);
    
    /** Get the current time limit. */
    int timeLimit();

    /** Ask what to promote a pawn to. Should call reportPromotePiece() when done. */
    void requestPromotePiece();

    /** Run code on the GUI thread. */
    void runOnUIThread(Runnable runnable);

    void setLastMove(Move move);

    void setGameStateString(String gameStateString);
}
