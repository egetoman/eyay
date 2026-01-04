package kuroyale;

import application.network.ConnectionStatus;
import application.network.NetworkLobbyController;
import application.network.NetworkLobbyState;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Phase 2 Feature 2: Network Multiplayer - Lobby UI.
 * <p>
 * Shows names, deck preview (names only), ready status, connection status + latency.
 */
public class NetworkLobbyView {
    private final BorderPane root;
    private final Label statusLabel = new Label();
    private final Label latencyLabel = new Label();
    private final Label localNameLabel = new Label();
    private final Label remoteNameLabel = new Label();
    private final Label localDeckLabel = new Label();
    private final Label remoteDeckLabel = new Label();
    private final Label localReadyLabel = new Label();
    private final Label remoteReadyLabel = new Label();
    private final Button readyButton = new Button("Ready");
    private final Button startButton = new Button("Start Match");
    private final Button enterMatchButton = new Button("Enter Match");

    public NetworkLobbyView(ScreenNavigator navigator, NetworkLobbyController lobbyController) {
        root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f1216;");

        Label title = new Label("Network Lobby");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.WHITE);
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);

        VBox center = new VBox(14);
        center.setPadding(new Insets(20));
        center.setAlignment(Pos.TOP_CENTER);

        styleSubtle(statusLabel);
        styleSubtle(latencyLabel);

        VBox localBox = buildPlayerBox("You", localNameLabel, localDeckLabel, localReadyLabel);
        VBox remoteBox = buildPlayerBox("Opponent", remoteNameLabel, remoteDeckLabel, remoteReadyLabel);
        HBox players = new HBox(18, localBox, remoteBox);
        players.setAlignment(Pos.CENTER);

        readyButton.setOnAction(e -> lobbyController.toggleReady());
        startButton.setOnAction(e -> lobbyController.tryStartMatch());
        enterMatchButton.setVisible(false);
        enterMatchButton.setManaged(false);
        enterMatchButton.setOnAction(e -> navigator.showNetworkMatchFromLobby(lobbyController, false, lobbyController.getLastReceivedStartPayload()));

        Button back = new Button("Back");
        back.setOnAction(e -> {
            lobbyController.close();
            navigator.showNetworkMenuScreen();
        });

        HBox actions = new HBox(12, back, readyButton, startButton, enterMatchButton);
        actions.setAlignment(Pos.CENTER);

        center.getChildren().addAll(statusLabel, latencyLabel, players, actions);
        root.setCenter(center);

        // Hook controller → UI
        lobbyController.setOnStateChanged(state -> Platform.runLater(() -> render(state)));
        lobbyController.setOnStartMatch((isHostSide, startPayload) -> Platform.runLater(() -> {
            navigator.showNetworkMatchFromLobby(lobbyController, isHostSide, startPayload);
        }));

        render(lobbyController.getState());
    }

    private VBox buildPlayerBox(String header, Label name, Label deck, Label ready) {
        Label h = new Label(header);
        h.setTextFill(Color.web("#cbd0d6"));
        h.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        name.setTextFill(Color.WHITE);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        deck.setTextFill(Color.web("#8f94a3"));
        deck.setWrapText(true);

        ready.setTextFill(Color.web("#ffd54f"));
        ready.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        VBox box = new VBox(8, h, name, deck, ready);
        box.setPadding(new Insets(14));
        box.setPrefWidth(420);
        box.setStyle("-fx-background-color: #1c1f2a; -fx-background-radius: 10; -fx-border-color: #2d2f36; -fx-border-radius: 10;");
        return box;
    }

    private void render(NetworkLobbyState state) {
        if (state == null) {
            return;
        }
        localNameLabel.setText(nonBlank(state.getLocalName(), "—"));
        remoteNameLabel.setText(nonBlank(state.getRemoteName(), "Waiting…"));
        localDeckLabel.setText("Deck: " + join(state.getLocalDeckNames()));
        remoteDeckLabel.setText("Deck: " + (state.getRemoteDeckNames().isEmpty() ? "—" : join(state.getRemoteDeckNames())));
        localReadyLabel.setText(state.isLocalReady() ? "READY" : "NOT READY");
        remoteReadyLabel.setText(state.isRemoteReady() ? "READY" : "NOT READY");

        ConnectionStatus status = state.getStatus();
        statusLabel.setText("Status: " + (status != null ? status : ConnectionStatus.DISCONNECTED)
            + (state.getStatusDetail() != null && !state.getStatusDetail().isBlank() ? " (" + state.getStatusDetail() + ")" : ""));
        latencyLabel.setText("Latency: " + (state.getLatencyMillis() >= 0 ? state.getLatencyMillis() + "ms" : "—"));

        startButton.setDisable(!state.isHost() || !(state.isLocalReady() && state.isRemoteReady()));

        // Reconnect fallback: if client is connected but stuck waiting, allow manual entry into match.
        boolean showEnter = !state.isHost()
            && status == ConnectionStatus.CONNECTED
            && (state.getRemoteName() == null || state.getRemoteName().isBlank());
        enterMatchButton.setVisible(showEnter);
        enterMatchButton.setManaged(showEnter);
    }

    private String join(java.util.List<String> names) {
        if (names == null || names.isEmpty()) {
            return "—";
        }
        return String.join(", ", names);
    }

    private void styleSubtle(Label l) {
        l.setTextFill(Color.web("#cbd0d6"));
    }

    private String nonBlank(String s, String fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        return s.trim();
    }

    public Parent getRoot() {
        return root;
    }
}


