package com.zyhang.arouter.autowiredtransform.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.android.arouter.facade.annotation.Autowired;

/**
 * Created by zyhang on 2020/6/2.10:55
 */
public class JavaActivity extends AppCompatActivity {

    @Autowired
    int i;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView view = new TextView(this);
        setContentView(view);

        view.setText(String.valueOf(i));
    }
}
