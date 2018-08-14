package com.aispeech.ddsdemo.widget.avatar;

import android.animation.Animator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.OnCompositionLoadedListener;
import com.aispeech.ailog.AILog;
import com.aispeech.ddsdemo.util.AssetUtils;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EView;
import org.androidannotations.annotations.SupposeUiThread;
import org.androidannotations.annotations.UiThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;


@EView
public class AvatarView extends LottieAnimationView implements IAvatarEvent {

    public static final String TAG = "AvatarView";

    /**
     * Because of onCompositionLoaded can not track which json file be loaded, so we use semaphore to ensure json
     * file be
     * loaded one by one.
     */
    private Semaphore loadingSemaphore = new Semaphore(1);

    /**
     * The list of all animation resource in memory.
     */
    private ArrayList<LottieComposition> lottieCompositions = new ArrayList<>();

    /**
     * Avatar current state, see {@link #IDLE } {@link #LISTENING} {@link #RECOGNIZING} {@link #SPEAKING}
     */
    private AbstractAvatarState state;

    /**
     * A flag that indicate that whether a click event has occurred under current state.
     */
    private AtomicBoolean isClickedInCurrentState = new AtomicBoolean(false);

    public AvatarView(Context context) {
        super(context);
        init();
    }

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @Override
    public void setOnClickListener(View.OnClickListener l) {
        super.setOnClickListener(new OneStateOneTimeOnClickListenerImpl(l));
    }

    class OneStateOneTimeOnClickListenerImpl implements View.OnClickListener {

        private View.OnClickListener listener;

        public OneStateOneTimeOnClickListenerImpl(View.OnClickListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            AILog.d(TAG, "onClick ");
            if (state != null && !isClickedInCurrentState.getAndSet(true)) {
                AILog.d(TAG, "onClick valid " + state.getName());
                listener.onClick(view);
            }
        }
    }

    @Background(serial = "worker")
    public void init() {
        addAnimatorListener(listener);

        try {
            ArrayList<String> list = (ArrayList<String>) AssetUtils.getJsonAssets(getContext(), "");

            for (String jsonName : list) {
                loadingSemaphore.acquire();
                AILog.d(TAG, "Get Lottie Animation from : " + jsonName);
                LottieComposition.Factory.fromAssetFileName(getContext(), jsonName,
                        new OnCompositionLoadedListener() {
                            @Override
                            public void onCompositionLoaded(@Nullable LottieComposition composition) {
                                AILog.d(TAG, "Get finished");
                                lottieCompositions.add(composition);
                                loadingSemaphore.release();
                            }
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * When you want to let the AvatarView to work, call this method.
     */
    public void go() {
        if (state == null) {
            state = IDLE;
        }
        AILog.d(TAG, "go " + state.getName());
    }


    /**
     * Cleanup the resource that used
     */
    public void destroy() {
        AILog.d(TAG, "destroy");
        if (isAnimating()) {
            cancelAnimation();
        }
        clearAnimation();
    }


    /**
     * Cancel current animation and play the request one.
     */
    @SupposeUiThread
    public void playAnimation(int compositionIndex, boolean isLoop) {
        cancelAnimation();
        if (compositionIndex + 1 <= lottieCompositions.size()) {
            AILog.d(TAG, "animation[ " + compositionIndex + " ] is playing");
            setComposition(lottieCompositions.get(compositionIndex));
            loop(isLoop);
            playAnimation();
        } else {
            AILog.w(TAG, "animation[ " + compositionIndex + " ] is not playing due to loading not finish.");
        }

    }


    /**
     * callback on UIThread
     */
    private Animator.AnimatorListener listener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            AILog.d(TAG, "onAnimationStart " + AvatarView.this.hashCode());
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            AILog.d(TAG, "onAnimationEnd " + AvatarView.this.hashCode());
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            AILog.d(TAG, "onAnimationCancel " + AvatarView.this.hashCode());
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            AILog.d(TAG, "onAnimationRepeat " + AvatarView.this.hashCode());
        }
    };

    private void transferState(AbstractAvatarState newState) {
        isClickedInCurrentState.set(false);
        this.state.exit();
        AILog.d(TAG, "bug : object" + this.hashCode());
        AILog.d(TAG, "Transfer State:[ " + state.getName() + " ] ==>> [ " + newState.getName() + " ]");
        this.state = newState;
        this.state.entry();
    }

    @Override
    @UiThread
    public void toListen() {
        AILog.d(TAG, "toListen");
        if (state != null) {
            state.toListen();
        }
    }

    @Override
    @UiThread
    public void toIdle() {
        AILog.d(TAG, "toIdle");
        if (state != null) {
            state.toIdle();
        }
    }

    @Override
    @UiThread
    public void toRecognize() {
        AILog.d(TAG, "toRecognize");
        if (state != null) {
            state.toRecognize();
        }
    }

    @Override
    @UiThread
    public void toSpeak() {
        AILog.d(TAG, "toSpeak");
        if (state != null) {
            state.toSpeak();
        }
    }

    private final AbstractAvatarState IDLE = new AbstractAvatarState() {

        @Override
        public String getName() {
            return "IDLE_STATE";
        }

        @Override
        public void entry() {
            super.entry();
            playAnimation(1, false);
        }

        @Override
        public void toListen() {
            transferState(LISTENING);
        }

        @Override
        public void toSpeak() {
            transferState(SPEAKING);
        }
    };

    private final AbstractAvatarState LISTENING = new AbstractAvatarState() {
        @Override
        public String getName() {
            return "LISTENING_STATE";
        }

        @Override
        public void entry() {
            super.entry();
            playAnimation(2, true);
        }

        @Override
        public void toRecognize() {
            transferState(RECOGNIZING);
        }

        @Override
        public void toSpeak() {
            transferState(SPEAKING);
        }

        @Override
        public void toIdle() {
            transferState(IDLE);
        }
    };

    private final AbstractAvatarState RECOGNIZING = new AbstractAvatarState() {
        @Override
        public String getName() {
            return "RECOGNIZING_STATE";
        }

        @Override
        public void entry() {
            super.entry();
            playAnimation(3, true);
        }

        @Override
        public void toIdle() {
            transferState(IDLE);
        }

        @Override
        public void toSpeak() {
            transferState(SPEAKING);
        }
    };

    private final AbstractAvatarState SPEAKING = new AbstractAvatarState() {
        @Override
        public String getName() {
            return "SPEAKING_STATE";
        }

        @Override
        public void entry() {
            super.entry();
            playAnimation(4, true);
        }

        @Override
        public void toIdle() {
            transferState(IDLE);
        }

        @Override
        public void toListen() {
            transferState(LISTENING);
        }
    };


}
