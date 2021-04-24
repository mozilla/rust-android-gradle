/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.nishtahir.androidrust;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.junit.Assert;

@RunWith(RobolectricTestRunner.class)
public class ExampleUnitTest implements JNACallback, JNICallback  {
    private String callbackValue;

    @Override
    public void invoke(String value) {
        // Here's the JNA version.
        callbackValue = "From JNA: " + value;
    }

    @Override
    public void callback(String value) {
        // Here's the JNI version.
        callbackValue = "From JNI: " + value;
    }

    @Test
    public void testViaJNI() {
        MainActivity.invokeCallbackViaJNI(this);
        Assert.assertEquals("From JNI: Hello from Rust", callbackValue);
    }

    @Test
    public void testViaJNA() {
        MainActivity.invokeCallbackViaJNA(this);
        Assert.assertEquals("From JNA: Hello from Rust", callbackValue);
    }
}
