package kuroyale;

import application.DeckController;
import application.NetworkService;
import application.network.NetworkConfig;
import application.network.NetworkLobbyController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Deck;

/**
 * Phase 2 Feature 2: Network Multiplayer - Host/Join entry screen.
 */
public class NetworkMenuView {
    private final VBox root;

    public NetworkMenuView(ScreenNavigator navigator, DeckController deckController, NetworkService networkService, ArenaLayout activeLayout) {
        root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Network Multiplayer (Phase 2)");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        NetworkConfig config = networkService != null ? networkService.loadConfig() : application.network.NetworkConfig.defaults();
        Gson gson = new GsonBuilder().create();

        Label subtitle = new Label("Host a match or join by IP. Lobby shows names, deck preview, and ready status.");

        TextField nameField = new TextField("Player");
        nameField.setPrefWidth(260);

        TextField ipField = new TextField("127.0.0.1");
        ipField.setPrefWidth(260);

        TextField portField = new TextField(String.valueOf(config.getDefaultPort()));
        portField.setPrefWidth(120);

        ComboBox<String> deckChoice = new ComboBox<>();
        deckChoice.getItems().addAll("Saved Deck", "Default Deck");
        deckChoice.setValue("Saved Deck");

        Label status = new Label();
        status.setWrapText(true);

        Button hostButton = new Button("Host");
        hostButton.setOnAction(e -> {
            Deck deck = resolveDeck(deckController, deckChoice.getValue());
            NetworkLobbyController lobby = networkService.createHostLobby(nameField.getText(), deck);
            int port = parsePort(portField.getText(), config.getDefaultPort());
            lobby.setStartPayload(activeLayout != null ? gson.toJson(activeLayout) : "");
            navigator.showNetworkLobbyScreen(lobby);
            lobby.startHost(port);
        });

        Button joinButton = new Button("Join");
        joinButton.setOnAction(e -> {
            Deck deck = resolveDeck(deckController, deckChoice.getValue());
            NetworkLobbyController lobby = networkService.createClientLobby(nameField.getText(), deck);
            int port = parsePort(portField.getText(), config.getDefaultPort());
            String ip = ipField.getText() != null ? ipField.getText().trim() : "127.0.0.1";
            if (ip.isBlank()) {
                ip = "127.0.0.1";
            }
            navigator.showNetworkLobbyScreen(lobby);
            lobby.startClient(ip, port);
        });

        Button back = new Button("Back");
        back.setOnAction(e -> navigator.showWelcomeScreen());

        VBox nameBox = new VBox(6, new Label("Your Name"), nameField);
        VBox deckBox = new VBox(6, new Label("Your Deck"), deckChoice);
        HBox row1 = new HBox(20, nameBox, deckBox);
        row1.setAlignment(Pos.CENTER);

        VBox ipBox = new VBox(6, new Label("Host IP"), ipField);
        VBox portBox = new VBox(6, new Label("Port"), portField);
        HBox row2 = new HBox(20, ipBox, portBox);
        row2.setAlignment(Pos.CENTER);

        HBox actions = new HBox(12, back, hostButton, joinButton);
        actions.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, subtitle, row1, row2, status, actions);
    }

    private Deck resolveDeck(DeckController deckController, String choice) {
        if (deckController == null) {
            return null;
        }
        if ("Default Deck".equalsIgnoreCase(choice)) {
            return deckController.buildDefaultDeck();
        }
        return deckController.loadDeck();
    }

    private int parsePort(String raw, int fallback) {
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < 1 || v > 65535) {
                return fallback;
            }
            return v;
        } catch (Exception e) {
            return fallback;
        }
    }

    public Parent getRoot() {
        return root;
    }
}


