package kuroyale.emote;

import java.net.URL;
import javafx.scene.media.AudioClip;

public final class EmoteSound {
    private static AudioClip cached;

    private EmoteSound() {
    }

    public static void play() {
        if (!EmoteSettings.isEmoteSoundsEnabled()) {
            return;
        }
        AudioClip clip = load();
        if (clip != null) {
            clip.play();
        }
    }

    private static AudioClip load() {
        if (cached != null) {
            return cached;
        }
        try {
            URL url = EmoteSound.class.getResource("/assets/sounds/emote_pop.wav");
            if (url == null) {
                return null;
            }
            cached = new AudioClip(url.toExternalForm());
            return cached;
        } catch (Exception e) {
            return null;
        }
    }
}
