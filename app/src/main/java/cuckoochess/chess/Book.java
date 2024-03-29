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

package cuckoochess.chess;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Implements an opening book. */
public class Book {
    public static class BookEntry {
        Move move;
        int count;
        BookEntry(Move move) {
            this.move = move;
            count = 1;
        }
    }
    private static Map<Long, List<BookEntry>> bookMap;
    private static Random rndGen;
    private static int numBookMoves = -1;
    private boolean verbose;

    public Book(boolean verbose) {
        this.verbose = verbose;
    }

    private void initBook() {
        if (numBookMoves >= 0)
            return;
        long t0 = System.currentTimeMillis();
        bookMap = new HashMap<>();
        rndGen = new SecureRandom();
        rndGen.setSeed(System.currentTimeMillis());
        numBookMoves = 0;
        try (InputStream inStream = getClass().getResourceAsStream("/book.bin")) {
            List<Byte> buf = new ArrayList<>(8192);
            byte[] tmpBuf = new byte[1024];
            while (true) {
                int len = inStream.read(tmpBuf);
                if (len <= 0) break;
                for (int i = 0; i < len; i++)
                    buf.add(tmpBuf[i]);
            }
            Position startPos = TextIO.readFEN(TextIO.startPosFEN);
            Position pos = new Position(startPos);
            UndoInfo ui = new UndoInfo();
            int len = buf.size();
            for (int i = 0; i < len; i += 2) {
                int b0 = buf.get(i); if (b0 < 0) b0 += 256;
                int b1 = buf.get(i+1); if (b1 < 0) b1 += 256;
                int move = (b0 << 8) + b1;
                if (move == 0) {
                    pos = new Position(startPos);
                } else {
                    boolean bad = ((move >> 15) & 1) != 0;
                    int prom = (move >> 12) & 7;
                    Move m = new Move(move & 63, (move >> 6) & 63,
                                      promToPiece(prom, pos.whiteMove));
                    if (!bad)
                        addToBook(pos, m);
                    pos.makeMove(m, ui);
                }
            }
        } catch (ChessParseError ex) {
            throw new RuntimeException();
        } catch (IOException ex) {
            System.out.println("Can't read opening book resource");
            throw new RuntimeException();
        }
        if (verbose) {
            long t1 = System.currentTimeMillis();
            System.out.printf("Book moves:%d (parse time:%.3f)%n", numBookMoves,
                    (t1 - t0) / 1000.0);
        }
    }

    /** Add a move to a position in the opening book. */
    private void addToBook(Position pos, Move moveToAdd) {
        List<BookEntry> ent = bookMap.get(pos.zobristHash());
        if (ent == null) {
            ent = new ArrayList<>();
            bookMap.put(pos.zobristHash(), ent);
        }
        for (int i = 0; i < ent.size(); i++) {
            BookEntry be = ent.get(i);
            if (be.move.equals(moveToAdd)) {
                be.count++;
                return;
            }
        }
        BookEntry be = new BookEntry(moveToAdd);
        ent.add(be);
        numBookMoves++;
    }

    /** Return a random book move for a position, or null if out of book. */
    public final Move getBookMove(Position pos) {
        initBook();
        List<BookEntry> bookMoves = bookMap.get(pos.zobristHash());
        if (bookMoves == null) {
            return null;
        }
        
        MoveGen.MoveList legalMoves = new MoveGen().pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, legalMoves);
        int sum = 0;
        for (int i = 0; i < bookMoves.size(); i++) {
            BookEntry be = bookMoves.get(i);
            boolean contains = false;
            for (int mi = 0; mi < legalMoves.size; mi++)
                if (legalMoves.m[mi].equals(be.move)) {
                    contains = true;
                    break;
                }
            if  (!contains) {
                // If an illegal move was found, it means there was a hash collision.
                return null;
            }
            sum += getWeight(bookMoves.get(i).count);
        }
        if (sum <= 0) {
            return null;
        }
        int rnd = rndGen.nextInt(sum);
        sum = 0;
        for (int i = 0; i < bookMoves.size(); i++) {
            sum += getWeight(bookMoves.get(i).count);
            if (rnd < sum) {
                return bookMoves.get(i).move;
            }
        }
        // Should never get here
        throw new RuntimeException();
    }

    private int getWeight(int count) {
        double tmp = Math.sqrt(count);
        return (int)(tmp * Math.sqrt(tmp) * 100 + 1);
    }

    /** Return a string describing all book moves. */
    public final String getAllBookMoves(Position pos) {
        initBook();
        StringBuilder ret = new StringBuilder();
        List<BookEntry> bookMoves = bookMap.get(pos.zobristHash());
        if (bookMoves != null) {
            for (BookEntry be : bookMoves) {
                String moveStr = TextIO.moveToString(pos, be.move, false);
                ret.append(moveStr);
                ret.append("(");
                ret.append(be.count);
                ret.append(") ");
            }
        }
        return ret.toString();
    }

    private static int promToPiece(int prom, boolean whiteMove) {
        switch (prom) {
        case 1: return whiteMove ? Piece.WQUEEN : Piece.BQUEEN;
        case 2: return whiteMove ? Piece.WROOK  : Piece.BROOK;
        case 3: return whiteMove ? Piece.WBISHOP : Piece.BBISHOP;
        case 4: return whiteMove ? Piece.WKNIGHT : Piece.BKNIGHT;
        default: return Piece.EMPTY;
        }
    }
}
