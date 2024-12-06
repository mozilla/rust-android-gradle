package com.nishtahir.androidrust;

import static java.lang.System.loadLibrary;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class MainActivity extends AppCompatActivity implements JNACallback, JNICallback {

    private static final String TAG = "MainActivity";

    public interface RustLibrary extends Library {
        RustLibrary INSTANCE = Native.load("rust", RustLibrary.class);

        int invokeCallbackViaJNA(JNACallback callback);
    }

    static {
        // On Android, this can be just:
        // System.loadLibrary("rust");
        // But when running as a unit test, we need to fish the libraries from
        // Java resources and configure the classpath.  We use JNA for that.
        NativeLibrary LIBRARY = NativeLibrary.getInstance("rust");
        System.load(LIBRARY.getFile().getPath());
    }

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.sample_text);

        invokeCallbackViaJNA(this);
        invokeCallbackViaJNI(this);
    }

    /**
     * A native method that is implemented by the 'rust' native library,
     * which is packaged with this application.
     */
    public static native void invokeCallbackViaJNI(JNICallback callback);

    public static void invokeCallbackViaJNA(JNACallback callback) {
        RustLibrary.INSTANCE.invokeCallbackViaJNA(callback);
    }

    @Override
    public void invoke(String string) {
        textView.append("From JNA: " + string + "\n");
    }

    @Override
    public void callback(String string) {
        textView.append("From JNI: " + string + "\n");
    }
}
