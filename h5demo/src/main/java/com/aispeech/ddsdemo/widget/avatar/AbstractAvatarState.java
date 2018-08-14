package com.aispeech.ddsdemo.widget.avatar;

import android.text.TextUtils;

import com.aispeech.ailog.AILog;

public abstract class AbstractAvatarState implements IAvatarEvent {

    public static final String TAG = "AbstractAvatarState";

    public abstract String getName();

    /**
     * Triggered when entry the state.
     */
    public void entry() {
        AILog.d(TAG, "entry state: " + getName());
    }

    /**
     * Triggered when exit the state.
     */
    public void exit() {
        AILog.d(TAG, "exit state: " + getName());
    }

    public boolean equals(AbstractAvatarState state) {
        return TextUtils.equals(this.getName(), state.getName());
    }


    // The events below is alternative for AvatarState to care about. //

    @Override
    public void toListen() {

    }

    @Override
    public void toIdle() {

    }

    @Override
    public void toRecognize() {

    }

    @Override
    public void toSpeak() {

    }
}
