package kuroyale.emote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.image.Image;

public final class EmoteAssets {
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private EmoteAssets() {
    }

    public static Image load(EmoteType type) {
        if (type == null || type.getAssetPath() == null) {
            return null;
        }
        return CACHE.computeIfAbsent(type.getAssetPath(), EmoteAssets::loadImage);
    }

    private static Image loadImage(String path) {
        try {
            var url = EmoteAssets.class.getResource(path);
            if (url == null) {
                return null;
            }
            return new Image(url.toExternalForm());
        } catch (Exception e) {
            return null;
        }
    }
}
