package com.ooftf.barrage.holder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ooftf.barrage.BarrageView;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    BarrageView barrageView;
    Random random = new Random();
    Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        barrageView = findViewById(R.id.barrage);
        barrageView.setViewCreator(new BarrageView.ViewOperator() {
            @Override
            public View createView(BarrageView parent, Object object) {
                TextView textView = new TextView(MainActivity.this);
                textView.setText((String) object);
                textView.setPadding(10, 10, 10, 10);
                textView.setTextSize(20);
                return textView;
            }

            @Override
            public void destroyView(BarrageView parent, View view) {
                parent.removeView(view);
            }
        });
        disposable = Flowable.interval(100, 200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                barrageView.addItem(String.valueOf(random.nextInt()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (disposable != null) {
            disposable.dispose();
        }
        super.onDestroy();
    }
}
