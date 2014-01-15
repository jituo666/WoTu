
package com.xjt.wotu.app;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import com.xjt.wotu.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

}
