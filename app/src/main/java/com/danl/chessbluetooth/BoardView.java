package com.danl.chessbluetooth;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import cuckoochess.chess.Move;
import cuckoochess.chess.MoveGen;
import cuckoochess.chess.Piece;
import cuckoochess.chess.Position;

public class BoardView extends View {

    private int mSquareSize;
    private boolean mDragging = false;
    private float mTouchX;
    private float mTouchY;
    private boolean mFlipped = false;

    private int mSelectedSquare = -1;

    private Position mPosition = null;
    private MoveGen mMoveGen = new MoveGen();
    private Move mLastMove = null;

    private SparseArray<Bitmap> mBitmaps;
    private Paint mPaint = new Paint();

    public BoardView(Context context) {
        super(context);
    }

    public BoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initBitmaps() {
        mBitmaps = new SparseArray<>(12);

        mBitmaps.put(Piece.WPAWN, getScaledBitmap(R.drawable.light_pawn));
        mBitmaps.put(Piece.WROOK, getScaledBitmap(R.drawable.light_rook));
        mBitmaps.put(Piece.WKNIGHT, getScaledBitmap(R.drawable.light_knight));
        mBitmaps.put(Piece.WBISHOP, getScaledBitmap(R.drawable.light_bishop));
        mBitmaps.put(Piece.WQUEEN, getScaledBitmap(R.drawable.light_queen));
        mBitmaps.put(Piece.WKING, getScaledBitmap(R.drawable.light_king));

        mBitmaps.put(Piece.BPAWN, getScaledBitmap(R.drawable.dark_pawn));
        mBitmaps.put(Piece.BROOK, getScaledBitmap(R.drawable.dark_rook));
        mBitmaps.put(Piece.BKNIGHT, getScaledBitmap(R.drawable.dark_knight));
        mBitmaps.put(Piece.BBISHOP, getScaledBitmap(R.drawable.dark_bishop));
        mBitmaps.put(Piece.BQUEEN, getScaledBitmap(R.drawable.dark_queen));
        mBitmaps.put(Piece.BKING, getScaledBitmap(R.drawable.dark_king));
    }

    public void setPosition(Position position) {
        mPosition = position;
        postInvalidate();
    }

    public void setSelectedSquare(int selectedSquare) {
        mSelectedSquare = selectedSquare;
        postInvalidate();
    }

    public void setLastMove(Move move) {
        mLastMove = move;
        postInvalidate();
    }

    public void setFlipped(boolean flipped) {
        mFlipped = flipped;
        postInvalidate();
    }

    public Move move(MotionEvent event) {
        mTouchX = event.getX();
        mTouchY = event.getY();

        int x = (int) (mTouchX / mSquareSize);
        int y = 7 - (int) (mTouchY / mSquareSize);
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            return null;
        }
        if (mFlipped) {
            x = 7 - x;
            y = 7 - y;
        }

        int square = Position.getSquare(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int piece = mPosition.getPiece(square);
                if (piece != Piece.EMPTY && Piece.isWhite(piece) == mPosition.whiteMove) {
                    setSelectedSquare(square);
                } else if (mSelectedSquare != -1) {
                    return new Move(mSelectedSquare, square, Piece.EMPTY);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mSelectedSquare != -1) {
                    mDragging = true;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDragging) {
                    mDragging = false;
                    if (square != mSelectedSquare) {
                        return new Move(mSelectedSquare, square, Piece.EMPTY);
                    } else {
                        invalidate();
                    }
                }
        }
        return null;
    }

    private Bitmap getScaledBitmap(int res) {
        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), res), mSquareSize, mSquareSize, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        mSquareSize = size / 8;
        initBitmaps();
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int lightSquareColor = ContextCompat.getColor(getContext(), R.color.lightSquareColor);
        int darkSquareColor = ContextCompat.getColor(getContext(), R.color.darkSquareColor);
        int squareColor = lightSquareColor;

        MoveGen.MoveList moves = null;
        if (mPosition != null) {
            moves = mMoveGen.pseudoLegalMoves(mPosition);
            MoveGen.removeIllegal(mPosition, moves);
        }

        int x, y = 0;
        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                x = j * mSquareSize;

                mPaint.setColor(squareColor);
                mPaint.setAlpha(255);
                canvas.drawRect(x, y, x + mSquareSize, y + mSquareSize, mPaint);
                squareColor = squareColor == lightSquareColor ? darkSquareColor : lightSquareColor;

                int square;
                if (!mFlipped) {
                    square = Position.getSquare(j, i);
                } else {
                    square = Position.getSquare(7 - j, 7 - i);
                }

                if (mLastMove != null && (mLastMove.from == square || mLastMove.to == square)) {
                    mPaint.setColor(Color.GRAY);
                    mPaint.setAlpha(127);
                    canvas.drawRect(x, y, x + mSquareSize, y + mSquareSize, mPaint);
                }

                if (mPosition != null) {
                    int piece = mPosition.getPiece(square);

                    if (square == mSelectedSquare) {
                        mPaint.setColor(Color.TRANSPARENT);
                        mPaint.setAlpha(127);
                        canvas.drawBitmap(mBitmaps.get(piece), x, y, mPaint);
                    } else if (piece != Piece.EMPTY) {
                        canvas.drawBitmap(mBitmaps.get(piece), x, y, null);
                    }
                }

                if (mSelectedSquare != -1 && moves != null) {
                    for (int mi = 0; mi < moves.size; mi++) {
                        Move move = moves.m[mi];

                        if (move.from == mSelectedSquare && move.to == square) {
                            mPaint.setColor(Color.YELLOW);
                            mPaint.setAlpha(127);
                            canvas.drawRect(x, y, x + mSquareSize, y + mSquareSize, mPaint);
                            break;
                        }
                    }
                }
            }

            squareColor = squareColor == lightSquareColor ? darkSquareColor : lightSquareColor;
            y += mSquareSize;
        }

        if (mDragging && mPosition != null) {
            canvas.drawBitmap(mBitmaps.get(mPosition.getPiece(mSelectedSquare)), mTouchX - mSquareSize / 2.0f, mTouchY - mSquareSize / 2.0f, null);
        }
    }
}
