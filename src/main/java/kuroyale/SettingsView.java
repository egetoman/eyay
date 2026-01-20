package kuroyale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

        Button back = new Button("Back");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> navigator.showWelcomeScreen());

        Button reset = new Button("Reset to defaults");
        reset.getStyleClass().add("secondary-button");

        Slider musicSlider = buildSlider(70);
        Slider sfxSlider = buildSlider(60);

        CheckBox fullscreen = new CheckBox("Fullscreen mode");
        fullscreen.setSelected(navigator.isFullScreen());
        fullscreen.getStyleClass().add("settings-toggle");
        fullscreen.setOnAction(e -> navigator.setFullScreen(fullscreen.isSelected()));

        CheckBox animations = new CheckBox("Enable UI animations");
        animations.setSelected(true);
        animations.getStyleClass().add("settings-toggle");

        ChoiceBox<String> scale = new ChoiceBox<>();
        scale.getItems().addAll("80%", "90%", "100%", "110%", "120%");
        scale.setValue("100%");

        VBox audioSection = createSection("Audio", createAudioSection(musicSlider, sfxSlider));
        VBox displaySection = createSection("Display", createDisplaySection(fullscreen, animations, scale));
        VBox socialSection = createSection("Social", createSocialSection());

        reset.setOnAction(e -> {
            musicSlider.setValue(70);
            sfxSlider.setValue(60);
            fullscreen.setSelected(false);
            navigator.setFullScreen(false);
            animations.setSelected(true);
            scale.setValue("100%");
        });

        VBox settingsStack = new VBox(20, audioSection, displaySection, socialSection);
        settingsStack.getStyleClass().add("settings-stack");
        settingsStack.setPadding(new Insets(10, 10, 20, 10));

        ScrollPane scrollPane = new ScrollPane(settingsStack);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("settings-scroll");

        HBox actions = new HBox(12, reset, back);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("settings-actions");

        VBox center = new VBox(20, scrollPane, actions);
        center.setAlignment(Pos.CENTER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.setCenter(center);
    }

    private VBox createSection(String title, Node content) {
        Label sectionTitle = new Label(title);
        sectionTitle.getStyleClass().add("section-title");

        VBox section = new VBox(12, sectionTitle, content);
        section.getStyleClass().addAll("sub-panel", "settings-section");
        return section;
    }

    private Node createAudioSection(Slider musicSlider, Slider sfxSlider) {
        Label musicValue = createValueLabel(musicSlider);
        HBox musicRow = createSliderRow("Music volume", musicSlider, musicValue);

        Label sfxValue = createValueLabel(sfxSlider);
        HBox sfxRow = createSliderRow("SFX volume", sfxSlider, sfxValue);

        VBox content = new VBox(12, musicRow, sfxRow);
        content.getStyleClass().add("settings-content");
        return content;
    }

    private Node createDisplaySection(CheckBox fullscreen, CheckBox animations, ChoiceBox<String> scale) {
        HBox scaleRow = createChoiceRow("UI scale", scale);

        VBox content = new VBox(12, fullscreen, animations, scaleRow);
        content.getStyleClass().add("settings-content");
        return content;
    }

    private Node createSocialSection() {
        CheckBox muteOpponent = new CheckBox("Mute opponent emotes");
        muteOpponent.setSelected(EmoteSettings.isMuteOpponentEmotes());
        muteOpponent.setOnAction(e -> EmoteSettings.setMuteOpponentEmotes(muteOpponent.isSelected()));
        muteOpponent.getStyleClass().add("settings-toggle");

        CheckBox emoteSound = new CheckBox("Emote sounds");
        emoteSound.setSelected(EmoteSettings.isEmoteSoundsEnabled());
        emoteSound.setOnAction(e -> EmoteSettings.setEmoteSoundsEnabled(emoteSound.isSelected()));
        emoteSound.getStyleClass().add("settings-toggle");

        VBox content = new VBox(12, muteOpponent, emoteSound);
        content.getStyleClass().add("settings-content");
        return content;
    }

    private Slider buildSlider(double initial) {
        Slider slider = new Slider(0, 100, initial);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(4);
        slider.setBlockIncrement(1);
        slider.getStyleClass().add("settings-slider");
        return slider;
    }

    private Label createValueLabel(Slider slider) {
        Label value = new Label();
        value.getStyleClass().add("settings-value");
        updateSliderLabel(slider, value);
        slider.valueProperty().addListener((obs, oldVal, newVal) -> updateSliderLabel(slider, value));
        return value;
    }

    private void updateSliderLabel(Slider slider, Label value) {
        value.setText(Math.round(slider.getValue()) + "%");
    }

    private HBox createSliderRow(String labelText, Slider slider, Label valueLabel) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, label, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("settings-row");

        VBox wrapper = new VBox(8, row, slider);
        wrapper.getStyleClass().add("settings-row-group");
        return new HBox(wrapper);
    }

    private HBox createChoiceRow(String labelText, ChoiceBox<String> choiceBox) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, label, spacer, choiceBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("settings-row");
        return row;
    }

    public Parent getRoot() {
        return root;
    }
}
