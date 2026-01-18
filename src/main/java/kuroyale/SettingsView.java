package kuroyale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import kuroyale.emote.EmoteSettings;

public class SettingsView {
    private final BorderPane root = new BorderPane();

    public SettingsView(ScreenNavigator navigator) {
        root.setPadding(new Insets(20));

        Label title = new Label("Settings");
        title.getStyleClass().add("screen-title");
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);

        CheckBox muteOpponent = new CheckBox("Mute opponent emotes");
        muteOpponent.setSelected(EmoteSettings.isMuteOpponentEmotes());
        muteOpponent.setOnAction(e -> EmoteSettings.setMuteOpponentEmotes(muteOpponent.isSelected()));

        CheckBox emoteSound = new CheckBox("Emote sounds");
        emoteSound.setSelected(EmoteSettings.isEmoteSoundsEnabled());
        emoteSound.setOnAction(e -> EmoteSettings.setEmoteSoundsEnabled(emoteSound.isSelected()));

        VBox content = new VBox(12, muteOpponent, emoteSound);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("sub-panel");

        Button back = new Button("Back");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> navigator.showWelcomeScreen());

        VBox center = new VBox(20, content, back);
        center.setAlignment(Pos.CENTER);
        root.setCenter(center);
    }

    public Parent getRoot() {
        return root;
    }
}
