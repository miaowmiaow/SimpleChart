package com.simple.chart.simplechart;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final int COLOR_BLUE = Color.parseColor("#33B5E5");
    public static final int COLOR_VIOLET = Color.parseColor("#AA66CC");
    public static final int COLOR_GREEN = Color.parseColor("#99CC00");
    public static final int COLOR_ORANGE = Color.parseColor("#FFBB33");
    public static final int COLOR_RED = Color.parseColor("#FF4444");
    public static final int[] COLORS = new int[]{COLOR_BLUE, COLOR_VIOLET, COLOR_GREEN, COLOR_ORANGE, COLOR_RED};
    public static int pickColor() {
        return COLORS[(int) Math.round(Math.random() * (COLORS.length - 1))];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SimpleColumnChartView columnChartView = (SimpleColumnChartView) findViewById(R.id.column);
        columnChartView.setRowAndColumn(7, 7);
        for(int i = 1; i<40; i++){
            SimpleColumnChartView.ColumnChartData data = new SimpleColumnChartView.ColumnChartData();
            data.setMaxOrdinateValue(7);
            data.setOrdinateValue(new Random().nextInt(7)+1);
            data.setAbscissaValue(i+"");
            data.setColor(pickColor());
            columnChartView.addColumnChartData(data);
        }
        columnChartView.setOnItemClickListener(new SimpleColumnChartView.OnItemClickListener() {
            @Override
            public void OnItemClick(int index, SimpleColumnChartView.ColumnChartData data) {
                Toast.makeText(MainActivity.this, "点击："+(index+1), Toast.LENGTH_SHORT).show();
            }
        });

        SimpleLineChartView line = (SimpleLineChartView) findViewById(R.id.line);
        line.setRowAndColumn(7, 7);
        for(int i = 1; i<40; i++){
            SimpleLineChartView.ColumnChartData data = new SimpleLineChartView.ColumnChartData();
            data.setMaxOrdinateValue(7);
            data.setOrdinateValue(new Random().nextInt(7)+1);
            data.setAbscissaValue(i+"");
            line.addColumnChartData(data);
        }
        line.setOnItemClickListener(new SimpleLineChartView.OnItemClickListener() {
            @Override
            public void OnItemClick(int index, SimpleLineChartView.ColumnChartData data) {
                Toast.makeText(MainActivity.this, "点击："+(index+1), Toast.LENGTH_SHORT).show();
            }
        });

    }
}
