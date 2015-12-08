package ru.zipta.bobr;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;


public class MyNumberPicker extends LinearLayout implements View.OnTouchListener {

    public static final String TAG = MyNumberPicker.class.getSimpleName();
    public static final int DELAY_MILLIS = 250;
    private EditText value_et;
    private int min, max;
    private Handler handler;

    public MyNumberPicker(Context context) {
        super(context);
        init(null, 0);
    }

    public MyNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public MyNumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.my_number_picker, this);
        value_et = (EditText) findViewById(R.id.val_et);
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MyNumberPicker,
                0, 0);
        try {
            min = a.getInt(R.styleable.MyNumberPicker_min, 0);
            max = a.getInt(R.styleable.MyNumberPicker_max, 59);
            value_et.setFilters(new InputFilter[]{new InputFilterMinMax(min, max)});
            setValue(a.getInt(R.styleable.MyNumberPicker_value, 0));
        } finally {
            a.recycle();
        }
        Button dec_btn = (Button) findViewById(R.id.dec_btn);
        dec_btn.setOnTouchListener(this);
        Button inc_btn = (Button) findViewById(R.id.inc_btn);
        inc_btn.setOnTouchListener(this);
    }

    public int getValue(){
        return Integer.parseInt(value_et.getText().toString());
    }

    public void setValue(int v){
        if(v >= min && v <= max){
            if(!isInEditMode()){
                value_et.setText(String.valueOf(v));
            }
        }
    }

    private Runnable decRunnable = new Runnable() {
        @Override
        public void run() {
            setValue(getValue()-1);
            //Log.d(TAG, "pressed");
            handler.postDelayed(decRunnable, DELAY_MILLIS);
        }
    };

    private Runnable incRunnable = new Runnable() {
        @Override
        public void run() {
            setValue(getValue()+1);
            //Log.d(TAG, "pressed");
            handler.postDelayed(incRunnable, DELAY_MILLIS);
        }
    };

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Runnable a;
        if(v.getId()==R.id.dec_btn){
            a = decRunnable;
        } else if(v.getId()==R.id.inc_btn){
            a = incRunnable;
        } else {
            return false;
        }
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (handler != null) return true;
                handler = new Handler();
                handler.postDelayed(a, 1);
                break;
            case MotionEvent.ACTION_UP:
                if (handler == null) return true;
                handler.removeCallbacks(a);
                handler = null;
                break;
        }
        return false;
    }

    private class InputFilterMinMax implements InputFilter{
        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                //Log.d(TAG, "dest " + dstart + ":" + dest.toString() + " source " +start + ":" + source.toString());
                String egg;
                if(dstart<=start){
                    egg = source.toString() + dest.toString();
                } else {
                    egg = dest.toString() + source.toString();
                }
                int input = Integer.parseInt(egg);
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) {/*nop*/}
            return "";
        }

        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }
}
