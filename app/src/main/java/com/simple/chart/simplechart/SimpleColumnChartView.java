package com.simple.chart.simplechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mr.Zhao on 2018/6/25.
 */

public class SimpleColumnChartView extends View {

    private int defaultPadding = 50;
    private int width;
    private int height;
    private float abscissaSpacing;
    private float ordinateSpacing;
    private float ordinateValue;
    private float aveOrdinateValue;
    private float maxOrdinateValue;

    private int row;
    private int column;
    private int currentColumn;
    private int minColumn;

    private List<ColumnChartData> columnChartDataList = new ArrayList<>();

    private Paint axisPaint;
    private Paint coordinateValuePaint;
    private Paint rectPaint;

    private float downX;
    private float moveX;
    private float maxMoveX;
    private float offsetX;
    private int offsetNum;

    private VelocityTracker velocityTracker;
    private int minFlingVelocity;
    private int maxFlingVelocity;

    private final ViewFlinger viewFlinger = new ViewFlinger();

    //f(x) = (x-1)^5 + 1
    private static final Interpolator sQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    private List<ColumnItem> columnItems = new ArrayList<>();
    private int clickNum = -1;
    private RectF clickRectF;

    private ScaleGestureDetector scaleGestureDetector;

    private OnItemClickListener onItemClickListener;

    public SimpleColumnChartView(Context context) {
        this(context, null);
    }

    public SimpleColumnChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleColumnChartView(final Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        axisPaint = new Paint();
        axisPaint.setAntiAlias(true);
        axisPaint.setColor(0xff999999);
        axisPaint.setStrokeWidth(2);
        axisPaint.setStyle(Paint.Style.FILL);

        coordinateValuePaint = new Paint();
        coordinateValuePaint.setAntiAlias(true);
        coordinateValuePaint.setColor(0xff999999);
        coordinateValuePaint.setTextSize(25);
        coordinateValuePaint.setStyle(Paint.Style.FILL);

        rectPaint = new Paint();
        rectPaint.setAntiAlias(true);
        rectPaint.setColor(0xffff7800);
        rectPaint.setStyle(Paint.Style.FILL);

        ViewConfiguration vc = ViewConfiguration.get(context);
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                column = (int) (currentColumn / detector.getScaleFactor());
                if (column > columnChartDataList.size()) {
                    column = columnChartDataList.size();
                } else if (column < minColumn) {
                    column = minColumn;
                }
                calculateSpacing();
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                currentColumn = column;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//禁止双击缩放
            scaleGestureDetector.setQuickScaleEnabled(false);
        }
    }

    public void setRowAndColumn(int row, int column) {
        this.row = row;
        this.column = column;
        this.minColumn = column;
        this.currentColumn = column;
    }

    public void addColumnChartData(ColumnChartData columnChartData) {
        this.columnChartDataList.add(columnChartData);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(columnChartDataList.size() < column){
            setRowAndColumn(row, columnChartDataList.size());
        }
        width = w;
        height = h;
        calculateSpacing();
    }

    private void calculateSpacing() {
        int chartWidth = width - defaultPadding * 2;
        int chartHeight = height - defaultPadding * 2;
        abscissaSpacing = (float) chartWidth / column;
        ordinateSpacing = (float) chartHeight / row;
        maxMoveX = abscissaSpacing * (columnChartDataList.size() - column);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawVerticalAxis(canvas);
        drawHorizontalAxis(canvas);
        drawCoordinateValue(canvas);
        drawRecF(canvas);
    }

    private void drawHorizontalAxis(Canvas canvas) {
        int size = row + 1;
        for (int i = 0; i < size; i++) {
            canvas.drawLine(defaultPadding, height - defaultPadding - (ordinateSpacing * i), width - defaultPadding, height - defaultPadding - (ordinateSpacing * i), axisPaint);
        }
    }

    private void drawVerticalAxis(Canvas canvas) {
        canvas.drawLine(defaultPadding, defaultPadding, defaultPadding, height - defaultPadding, axisPaint);
        canvas.drawLine(width - defaultPadding, defaultPadding, width - defaultPadding, height - defaultPadding, axisPaint);
    }

    private void drawCoordinateValue(Canvas canvas) {
        for (int i = 0; i < column; i++) {
            calculateOrdinate(i + offsetNum);
            String abscissaText = String.valueOf(columnChartDataList.get(i + offsetNum).getAbscissaValue());
            float textHeight = coordinateValuePaint.descent() - coordinateValuePaint.ascent();
            float horizontalTextOffset = coordinateValuePaint.measureText(String.valueOf(abscissaText)) / 2;
            float textX = defaultPadding + abscissaSpacing * (i + 0.5f) - horizontalTextOffset + offsetX;
            float textY = height - defaultPadding + textHeight + 10;
            if (textX > defaultPadding) {
                canvas.drawText(abscissaText, textX, textY, coordinateValuePaint);
            }
        }
        int rowSize = row + 1;
        for (int i = 1; i < rowSize; i++) {
            String ordinateText = String.valueOf(Math.round(aveOrdinateValue * i * 100) / 100);
            float textHeight = coordinateValuePaint.descent() - coordinateValuePaint.ascent();
            float verticalTextOffset = (textHeight / 2) - coordinateValuePaint.descent();
            float horizontalTextOffset = coordinateValuePaint.measureText(String.valueOf(ordinateText)) + 20;
            canvas.drawText(ordinateText, defaultPadding - horizontalTextOffset, height - defaultPadding - (ordinateSpacing * i) + verticalTextOffset, coordinateValuePaint);
        }
    }

    private void drawRecF(Canvas canvas) {
        columnItems.clear();
        for (int i = 0; i < column; i++) {
            float t = 0f;
            if (clickNum == i + offsetNum) {
                t = 5f;
            }
            float left = defaultPadding + abscissaSpacing * (i + 0.25f) + offsetX - t;
            if (left < defaultPadding) {
                left = defaultPadding;
                if (column + offsetNum < columnChartDataList.size()) {
                    calculateOrdinate(column + offsetNum);
                    String abscissaText = String.valueOf(columnChartDataList.get(column + offsetNum).getAbscissaValue());
                    float textHeight = coordinateValuePaint.descent() - coordinateValuePaint.ascent();
                    float horizontalTextOffset = coordinateValuePaint.measureText(String.valueOf(abscissaText)) / 2;
                    float textX = defaultPadding + abscissaSpacing * (column + 0.5f) - horizontalTextOffset + offsetX;
                    float textY = height - defaultPadding + textHeight + 10;
                    if (textX <= width - defaultPadding - horizontalTextOffset) {
                        canvas.drawText(abscissaText, textX, textY, coordinateValuePaint);
                    }
                    float t2 = 0f;
                    if (clickNum == column + offsetNum) {
                        t2 = 5f;
                    }
                    float left2 = defaultPadding + abscissaSpacing * (column + 0.25f) + offsetX - t2;
                    float top2 = height - defaultPadding - ordinateValue * columnChartDataList.get(column + offsetNum).getOrdinateValue();
                    float right2 = defaultPadding + abscissaSpacing * (column + 0.75f) + offsetX + t2;
                    if (right2 > width - defaultPadding) {
                        right2 = width - defaultPadding;
                    }
                    float bottom2 = height - defaultPadding;
                    RectF rectF = new RectF(left2, top2, right2, bottom2);
                    columnItems.add(new ColumnItem(column + offsetNum, rectF));
                    if (columnChartDataList.get(column + offsetNum).getColor() != -1) {
                        rectPaint.setColor(columnChartDataList.get(column + offsetNum).getColor());
                    }
                    canvas.drawRect(rectF, rectPaint);
                }
            }
            float top = height - defaultPadding - ordinateValue * columnChartDataList.get(i + offsetNum).getOrdinateValue();
            float right = defaultPadding + abscissaSpacing * (i + 0.75f) + offsetX + t;
            if (right < defaultPadding) {
                right = defaultPadding;
            }
            float bottom = height - defaultPadding;
            RectF rectF = new RectF(left, top, right, bottom);
            columnItems.add(new ColumnItem(i + offsetNum, rectF));
            if (columnChartDataList.get(i + offsetNum).getColor() != -1) {
                rectPaint.setColor(columnChartDataList.get(i + offsetNum).getColor());
            }
            canvas.drawRect(rectF, rectPaint);
        }
    }

    private void calculateOrdinate(int index) {
        if (maxOrdinateValue < columnChartDataList.get(index).getMaxOrdinateValue()) {
            maxOrdinateValue = columnChartDataList.get(index).getMaxOrdinateValue();
            ordinateValue = (height - defaultPadding * 2) / maxOrdinateValue;
            aveOrdinateValue = maxOrdinateValue / row;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        scaleGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (ColumnItem columnItem : columnItems) {
                    if (columnItem.getRectF().contains(event.getX(), event.getY())) {
                        clickNum = columnItem.getLocation();
                        clickRectF = columnItem.getRectF();
                        break;
                    }
                }
                downX = event.getX() - moveX;
                break;
            case MotionEvent.ACTION_MOVE:
                if (clickRectF == null || !clickRectF.contains(event.getX(), event.getY())) {
                    clickNum = -1;
                }
                moveX = event.getX() - downX;
                scrollX();
                break;
            case MotionEvent.ACTION_UP:
                if (onItemClickListener != null && clickNum != -1) {
                    onItemClickListener.OnItemClick(clickNum, columnChartDataList.get(clickNum));
                }
                clickNum = -1;
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                float xVelocity = velocityTracker.getXVelocity();
                if (Math.abs(xVelocity) < minFlingVelocity) {
                    viewFlinger.stop();
                } else {
                    viewFlinger.fling((int) Math.max(-maxFlingVelocity, Math.min(xVelocity, maxFlingVelocity)));
                }
                resetTouch();
                break;
            case MotionEvent.ACTION_CANCEL:
                clickNum = -1;
                resetTouch();
                break;
        }
        invalidate();
        return true;
    }

    private void scrollX() {
        if (moveX > 0) {
            moveX = 0;
        }
        if (moveX < -maxMoveX) {
            moveX = -maxMoveX;
            viewFlinger.stop();
        }
        offsetX = moveX % abscissaSpacing;
        offsetNum = (int) (-moveX / abscissaSpacing);
    }

    private void resetTouch() {
        if (velocityTracker != null) {
            velocityTracker.clear();
        }
    }

    private class ViewFlinger implements Runnable {

        private OverScroller mScroller;
        private boolean mEatRunOnAnimationRequest = false;
        private boolean mReSchedulePostAnimationCallback = false;

        public ViewFlinger() {
            mScroller = new OverScroller(getContext(), sQuinticInterpolator);
        }

        @Override
        public void run() {
            disableRunOnAnimationRequests();
            if (mScroller.computeScrollOffset()) {
                moveX = mScroller.getCurrX();
                scrollX();
                invalidate();
                postOnAnimation();
            }
            enableRunOnAnimationRequests();
        }

        public void fling(int velocityX) {
            mScroller.fling((int) moveX, 0, velocityX, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            postOnAnimation();
        }

        public void stop() {
            removeCallbacks(this);
            mScroller.abortAnimation();
        }

        private void disableRunOnAnimationRequests() {
            mReSchedulePostAnimationCallback = false;
            mEatRunOnAnimationRequest = true;
        }

        private void enableRunOnAnimationRequests() {
            mEatRunOnAnimationRequest = false;
            if (mReSchedulePostAnimationCallback) {
                postOnAnimation();
            }
        }

        void postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true;
            } else {
                removeCallbacks(this);
                ViewCompat.postOnAnimation(SimpleColumnChartView.this, this);
            }
        }
    }

    private class ColumnItem {
        private int location;
        private RectF rectF;

        public ColumnItem(int location, RectF rectF) {
            this.location = location;
            this.rectF = rectF;
        }

        public RectF getRectF() {
            return rectF;
        }

        public int getLocation() {
            return location;
        }

    }

    public static class ColumnChartData {

        private float maxOrdinateValue;
        private float ordinateValue;
        private String abscissaValue;
        private int color = -1;

        public float getMaxOrdinateValue() {
            return maxOrdinateValue;
        }

        public void setMaxOrdinateValue(float maxOrdinateValue) {
            this.maxOrdinateValue = maxOrdinateValue;
        }

        public float getOrdinateValue() {
            return ordinateValue;
        }

        public void setOrdinateValue(float ordinateValue) {
            this.ordinateValue = ordinateValue;
            if (this.maxOrdinateValue < ordinateValue) {
                this.maxOrdinateValue = ordinateValue;
            }
        }

        public String getAbscissaValue() {
            return abscissaValue;
        }

        public void setAbscissaValue(String abscissaValue) {
            this.abscissaValue = abscissaValue;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }

    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void OnItemClick(int index, ColumnChartData data);
    }

}
