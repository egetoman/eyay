package kuroyale;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import kuroyale.domain.Card;

final class CardArt {
    private static final String BASE_PATH = "/assets/cards/";

    private CardArt() {
    }

    static ImageView buildCardPortrait(Card card, double size) {
        Image image = loadCardImage(card);
        if (image == null) {
            return null;
        }
        ImageView view = new ImageView(image);
        view.setViewport(new Rectangle2D(0, 0, image.getWidth(), image.getHeight()));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);

        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        view.setClip(clip);
        return view;
    }

    static Image loadCardImage(Card card) {
        if (card == null || card.getId() == null) {
            return null;
        }
        String id = card.getId();
        String base = id.startsWith("card_") ? id.substring(5) : id;

        List<String> candidates = new ArrayList<>();
        candidates.add(base + ".png");
        candidates.add(base.toLowerCase() + ".png");
        if ("mini_pekka".equals(base)) {
            candidates.add("mini_PEKKA.png");
        }

        for (String filename : candidates) {
            Image image = loadImage(BASE_PATH + filename);
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    private static Image loadImage(String resourcePath) {
        try (InputStream in = CardArt.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            return new Image(in);
        } catch (Exception e) {
            return null;
        }
    }
}
