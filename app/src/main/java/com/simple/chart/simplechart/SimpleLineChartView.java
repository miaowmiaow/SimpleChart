package com.simple.chart.simplechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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

public class SimpleLineChartView extends View {

    private int defaultPadding = 50;
    private int width;
    private int height;
    private float abscissaSpacing;
    private float ordinateSpacing;
    private float ordinateValue;
    private float aveOrdinateValue;
    private float maxOrdinateValue;

    private int row = 7;
    private int column = 7;
    private int currentColumn;
    private int minColumn;

    private List<ColumnChartData> columnChartDataList = new ArrayList<>();

    private Paint axisPaint;
    private Paint coordinateValuePaint;
    private Paint rectPaint;
    private Paint linePaint;
    private Paint pointPaint1;
    private Paint pointPaint2;

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

    private ScaleGestureDetector scaleGestureDetector;

    private OnItemClickListener onItemClickListener;
    private int clickNum = -1;
    private RectF clickRectF;

    private List<ColumnItem> columnItems = new ArrayList<>();

    public SimpleLineChartView(Context context) {
        this(context, null);
    }

    public SimpleLineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleLineChartView(final Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.WHITE);
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
        rectPaint.setColor(0xffffffff);
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setStrokeWidth(5);

        linePaint = new Paint();
        linePaint.setColor(0xff2196f3);
        linePaint.setStyle(Paint.Style.STROKE);    // 填充模式 - 描边
        linePaint.setStrokeWidth(5);

        pointPaint1 = new Paint();
        pointPaint1.setColor(0xff2196f3);
        pointPaint1.setStyle(Paint.Style.FILL_AND_STROKE);    // 填充模式 - 描边
        pointPaint1.setStrokeWidth(5);

        pointPaint2 = new Paint();
        pointPaint2.setColor(0xffffffff);
        pointPaint2.setStyle(Paint.Style.FILL);    // 填充模式 - 描边
        pointPaint2.setStrokeWidth(5);

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
        this.column = column + 1;
        this.minColumn = column + 1;
        this.currentColumn = column + 1;
    }

    public void addColumnChartData(ColumnChartData columnChartData) {
        this.columnChartDataList.add(columnChartData);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (columnChartDataList.size() < column) {
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
        maxMoveX = abscissaSpacing * (columnChartDataList.size() - column + 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawVerticalAxis(canvas);
        drawHorizontalAxis(canvas);
        drawLine(canvas);
        drawRecF(canvas);
        drawPoint(canvas);
        drawCoordinateValue(canvas);
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
            if (i + offsetNum < columnChartDataList.size()) {
                String abscissaText = String.valueOf(columnChartDataList.get(i + offsetNum).getAbscissaValue());
                float textHeight = coordinateValuePaint.descent() - coordinateValuePaint.ascent();
                float horizontalTextOffset = coordinateValuePaint.measureText(String.valueOf(abscissaText)) / 2;
                float textX = defaultPadding + abscissaSpacing * (i + 1) - horizontalTextOffset + offsetX;
                float textY = height - defaultPadding + textHeight + 10;
                if (textX > defaultPadding) {
                    canvas.drawText(abscissaText, textX, textY, coordinateValuePaint);
                }
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
        canvas.drawRect(0, 0, defaultPadding, height, rectPaint);
        canvas.drawRect(width - defaultPadding, 0, width, height, rectPaint);
    }

    private void drawLine(Canvas canvas) {
        for (int i = 0; i < column; i++) {
            if (i + 1 + offsetNum < columnChartDataList.size()) {
                calculateOrdinate(i + offsetNum);
                float startX = defaultPadding + abscissaSpacing * (i + 1) + offsetX;
                if (startX <= defaultPadding + abscissaSpacing) {
                    if (i - 1 + offsetNum >= 0) {
                        float startX2 = defaultPadding + abscissaSpacing * (i) + offsetX;
                        float startY2 = height - defaultPadding - ordinateValue * columnChartDataList.get(i - 1 + offsetNum).getOrdinateValue();
                        float stopX2 = defaultPadding + abscissaSpacing * (i + 1) + offsetX;
                        float stopY2 = height - defaultPadding - ordinateValue * columnChartDataList.get(i + offsetNum).getOrdinateValue();
                        canvas.drawLine(startX2, startY2, stopX2, stopY2, linePaint);
                    }
                }
                float startY = height - defaultPadding - ordinateValue * columnChartDataList.get(i + offsetNum).getOrdinateValue();
                float stopX = defaultPadding + abscissaSpacing * (i + 2) + offsetX;
                float stopY = height - defaultPadding - ordinateValue * columnChartDataList.get(i + 1 + offsetNum).getOrdinateValue();
                canvas.drawLine(startX, startY, stopX, stopY, linePaint);
            }
        }
    }

    private void drawPoint(Canvas canvas) {
        columnItems.clear();
        for (int i = 0; i < column; i++) {
            if (i + offsetNum < columnChartDataList.size()) {
                float t = 0f;
                if (clickNum == i + offsetNum) {
                    t = 5f;
                }
                calculateOrdinate(i + offsetNum);
                float startX = defaultPadding + abscissaSpacing * (i + 1) + offsetX;
                if (startX <= defaultPadding + abscissaSpacing) {
                    if (i - 1 + offsetNum >= 0) {
                        float startX2 = defaultPadding + abscissaSpacing * (i) + offsetX;
                        float startY2 = height - defaultPadding - ordinateValue * columnChartDataList.get(i - 1 + offsetNum).getOrdinateValue();
                        columnItems.add(new ColumnItem(i - 1 + offsetNum, new RectF(startX2 - 10, startY2 - 10, startX2 + 10, startY2 + 10)));
                        canvas.drawCircle(startX2, startY2, 10 + t, pointPaint1);
                        canvas.drawCircle(startX2, startY2, 10 + t, pointPaint2);
                    }
                }
                float startY = height - defaultPadding - ordinateValue * columnChartDataList.get(i + offsetNum).getOrdinateValue();
                columnItems.add(new ColumnItem(i + offsetNum, new RectF(startX - 10, startY - 10, startX + 10, startY + 10)));
                canvas.drawCircle(startX, startY, 10 + t, pointPaint1);
                canvas.drawCircle(startX, startY, 10 + t, pointPaint2);
            }
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
                for (SimpleLineChartView.ColumnItem columnItem : columnItems) {
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
                ViewCompat.postOnAnimation(SimpleLineChartView.this, this);
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
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void OnItemClick(int index, ColumnChartData data);
    }

}
