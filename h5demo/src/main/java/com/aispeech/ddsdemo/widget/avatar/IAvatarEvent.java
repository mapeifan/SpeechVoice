package com.aispeech.ddsdemo.widget.avatar;


/**
 * List all of the events that Avatar care about.
 */
public interface IAvatarEvent {

    /**
     * Indicate to show listening animation.<br>
     * Triggered when:<ul>
     * <li>dialog.state listen</li>
     * </ul>
     */
    void toListen();


    /**
     * Indicate to show idle animation.
     * Triggered when:<ul>
     * <li>dialog.state idle</li>
     * </ul>
     */
    void toIdle();


    /**
     * Indicate to show recognizing animation.
     * Triggered when:<ul>
     * <li>dialog.state process</li>
     * </ul>
     */
    void toRecognize();


    /**
     * Indicate to show speaking animation.
     * Triggered when:<ul>
     * <li>dialog.state speak</li>
     * </ul>
     */
    void toSpeak();
}
