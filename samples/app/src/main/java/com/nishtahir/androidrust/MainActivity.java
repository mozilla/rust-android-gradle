package com.nishtahir.androidrust;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Callback;
public class MainActivity extends AppCompatActivity implements JNICallback {

    private static final String TAG = "MainActivity";

    // Used to load the 'rust' library on application startup.
    static {
        System.loadLibrary("rust");
    }

    TextView textView;

    private final Callback viaJNA = new Callback() {
        public void callback(String string) {
            Log.i("rust","From JNA: " + string);
            textView.setText("From JNA: " + string);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.sample_text);

        invokeCallbackViaJNI(this);
        LibService.instance.invokeCallbackViaJNA(viaJNA);
    }

    /**
     * A native method that is implemented by the 'rust' native library,
     * which is packaged with this application.
     */
    public static native void invokeCallbackViaJNI(JNICallback callback);

    @Override
    public void callback(String string) {
        Log.i("rust","From JNI: " + string);
        textView.append("From JNI: " + string + "\n");
    }

}

interface LibService extends Library {
    public static final String JNALib = "rust";
    public static final LibService instance = Native.loadLibrary(JNALib, LibService.class);

    void invokeCallbackViaJNA(Callback callback);
}