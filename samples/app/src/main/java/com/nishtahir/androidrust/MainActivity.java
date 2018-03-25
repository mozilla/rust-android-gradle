package com.nishtahir.androidrust;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Used to load the 'rust' library on application startup.
    static {
        System.loadLibrary("rust");
    }

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.sample_text);

        // Example of a call to a native method
        startRequestFromJni(this);
    }

    /**
     * A native method that is implemented by the 'rust' native library,
     * which is packaged with this application.
     */
    private static native void startRequestFromJni(MainActivity callback);

    public void appendToTextView(String string) {
        textView.append(string + "\n");
    }

    public void hello() {
        Log.d(TAG, "Looks like it works");
    }

}
