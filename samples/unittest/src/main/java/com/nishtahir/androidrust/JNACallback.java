package com.nishtahir.androidrust;

import com.sun.jna.Callback;

public interface JNACallback extends Callback {
    public void invoke(String string);
}
