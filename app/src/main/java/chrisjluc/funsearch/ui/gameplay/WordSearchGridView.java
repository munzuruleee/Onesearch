package chrisjluc.funsearch.ui.gameplay;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

import chrisjluc.funsearch.R;
import chrisjluc.funsearch.WordSearchManager;
import chrisjluc.funsearch.adapters.WordSearchGridAdapter;
import chrisjluc.funsearch.wordSearchGenerator.generators.WordSearchGenerator;
import chrisjluc.funsearch.wordSearchGenerator.models.Node;
import chrisjluc.funsearch.wordSearchGenerator.models.Point;

public class WordSearchGridView extends GridView {

    public boolean mIsWordFound = false;
    private int mXLength, mYLength;
    private int mColumnWidth;
    private int mHorizontalMargin, mVerticalMargin;
    private Point mStartDrag, mEndDrag;
    private Node[] mWordSearchNodes;
    private List<Node> mWordSearchHighlightedNodes;
    private String mWord;
    private Point mWordStart, mWordEnd;
    private WordFoundListener mWordFoundListener;
    private WordSearchGridAdapter mAdapter;

    public WordSearchGridView(Context context, AttributeSet attrs) {
        super(context, attrs);

        WordSearchManager manager = WordSearchManager.getInstance();
        WordSearchGenerator wordSearch = manager.getWordSearch(WordSearchActivity.currentItem++);
        mXLength = wordSearch.getnCol();
        mYLength = wordSearch.getnRow();
        mWord = wordSearch.getWord();
        List<Point> points = wordSearch.getStartAndEndPointOfWord();

        // Convert cartesian from matrix coordinates
        mWordStart = new Point(points.get(0).y, points.get(0).x);
        mWordEnd = new Point(points.get(1).y, points.get(1).x);
        mWordSearchNodes = wordSearch.getWordSearchNodes();
        // Init to max size of wordsearch
        mWordSearchHighlightedNodes = new ArrayList<Node>(mXLength);
        mHorizontalMargin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
        mVerticalMargin = (int) getResources().getDimension(R.dimen.activity_vertical_margin);

        // Calculate column dimensions
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        mColumnWidth = (int) (width - 2 * mHorizontalMargin / displayMetrics.density) / (mXLength);
        setColumnWidth(mColumnWidth);
        mAdapter = new WordSearchGridAdapter(context, mWordSearchNodes, mColumnWidth, mXLength);
        setAdapter(mAdapter);
    }

    public void setWordFoundListener(WordFoundListener listener) {
        this.mWordFoundListener = listener;
    }

    private void isWordFound() {
        if ((mWordStart.equals(mStartDrag) && mWordEnd.equals(mEndDrag))
                || (mWordStart.equals(mEndDrag) && mWordEnd.equals(mStartDrag))) {
            mIsWordFound = true;
            mWordFoundListener.notifyWordFound();
        }
    }

    public void highlightWord() {
        mStartDrag = mWordStart;
        updateCurrentHighlightedNodes(mWordEnd);
    }

    private void updateCurrentHighlightedNodes(Point p) {
        if (p.y < 0 || p.y >= mYLength || p.x < 0 || p.x >= mXLength) return;

        if (p.equals(mEndDrag)) return;

        mEndDrag = p;

        boolean isValid = false;
        // diagonal
        if (Math.abs(mEndDrag.x - mStartDrag.x) == Math.abs(mEndDrag.y - mStartDrag.y)) {
            isValid = true;
            // horizontal
        } else if (mEndDrag.x - mStartDrag.x == 0) {
            isValid = true;
            // vertical
        } else if (mEndDrag.y - mStartDrag.y == 0) {
            isValid = true;
        }

        if (!isValid) return;

        clearHighlightedNodes();
        int dX = mEndDrag.x - mStartDrag.x;
        int dY = mEndDrag.y - mStartDrag.y;

        int length = 0;

        if (dX != 0)
            length = Math.abs(dX);

        if (dY != 0)
            length = Math.abs(dY);

        for (int i = 0; i < length + 1; i++) {
            Point point = new Point(mStartDrag.x, mStartDrag.y);
            if (dX != 0)
                point.x += dX > 0 ? i : -i;
            if (dY != 0)
                point.y += dY > 0 ? i : -i;

            int index = point.y * mXLength + point.x;
            if (index < 0 || index >= mWordSearchNodes.length)
                continue;
            Node n = mWordSearchNodes[index];
            mWordSearchHighlightedNodes.add(n);
            n.setHighlighted(true);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void clearHighlightedNodes() {
        for (Node n : mWordSearchHighlightedNodes)
            n.setHighlighted(false);
        mWordSearchHighlightedNodes.clear();
    }

    private void clearHighlighedNodesAndNotifyAdapter() {
        for (Node n : mWordSearchHighlightedNodes)
            n.setHighlighted(false);
        mWordSearchHighlightedNodes.clear();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x1, x2, y1, y2;
        boolean result = false;
        if (mIsWordFound) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = (int) event.getX();
                y1 = (int) event.getY();
                mStartDrag = new Point(calcRelativeX(x1), calcRelativeY(y1));
                result = true;
                break;
            case MotionEvent.ACTION_MOVE:
                x2 = (int) event.getX();
                y2 = (int) event.getY();
                result = true;
                updateCurrentHighlightedNodes(new Point(calcRelativeX(x2), calcRelativeY(y2)));
                break;
            case MotionEvent.ACTION_UP:
                isWordFound();
                if (!mIsWordFound)
                    clearHighlighedNodesAndNotifyAdapter();
                result = true;
                break;
        }

        if (result) invalidate();
        return result;
    }

    private int calcRelativeX(int d) {
        return (d - mHorizontalMargin) / mColumnWidth;
    }

    private int calcRelativeY(int d) {
        return (d - mVerticalMargin) / mColumnWidth;
    }

    public String getWord() {
        return mWord;
    }

    public interface WordFoundListener {
        public void notifyWordFound();
    }
}
