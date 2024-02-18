package com.nishtahir.androidrust;

import android.content.Context;
import android.widget.TextView;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.nishtahir.androidrust.BuildConfig;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.nishtahir.androidrust", appContext.getPackageName());
    }

    @Test
    public void testJNI() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TextView textView = activity.findViewById(R.id.sample_text);
                String text = textView.getText().toString();
                assertTrue(text.contains("From JNI: Hello from Rust"));
                assertTrue(text.contains(String.format("[feature=%s]", BuildConfig.FEATURES)));
            });
        }
    }
}
