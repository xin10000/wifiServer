package com.example.administrator.wifiserver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    public void onClick(View view) {
        if (view.getId() == R.id.btn_server) {
            startActivity(new Intent(this,ServerActivity.class));
        } else {
            startActivity(new Intent(this,ClientActivity.class));

        }
    }
}
