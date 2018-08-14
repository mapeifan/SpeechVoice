package com.aispeech.ddsdemo.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.aispeech.ailog.AILog;
import com.aispeech.ddsdemo.R;
import com.aispeech.ddsdemo.widget.avatar.AvatarView;
import com.aispeech.ddsdemo.widget.avatar.IAvatarEvent;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

@EViewGroup(R.layout.input_filed)
public class InputField extends RelativeLayout implements IAvatarEvent {

    public static final String TAG = "InputField";

    private Listener listener;

    @ViewById(R.id.animation_view)
    AvatarView avatarView;

    public InputField(Context context) {
        super(context);
    }

    public InputField(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InputField(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @AfterViews
    protected void setUp() {
        avatarView.setScale(0.4f);
        //monitor();
    }

    @Background(delay = 2000)
    public void monitor() {
        try {
            avatarView.toListen();
            Thread.sleep(8000);
            avatarView.toRecognize();
            Thread.sleep(8000);
            avatarView.toSpeak();
            Thread.sleep(8000);
            avatarView.toIdle();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Click(R.id.animation_view)
    public void micClicked(View clickedView) {
        AILog.e(TAG, clickedView.getId() + " clicked");
        if (listener != null) {
            listener.onMicClicked();
        }
    }

    public void destroy() {
        avatarView.destroy();
    }


    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public AvatarView getAvatarView() {
        return avatarView;
    }


    public interface Listener {
        /**
         * @return if the click event has been used
         */
        boolean onMicClicked();
    }

    @Override
    public void toListen() {
        avatarView.toListen();
    }

    @Override
    public void toIdle() {
        avatarView.toIdle();
    }

    @Override
    public void toRecognize() {
        avatarView.toRecognize();
    }

    @Override
    public void toSpeak() {
        avatarView.toSpeak();
    }

}
