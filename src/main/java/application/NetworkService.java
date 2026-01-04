package application;

import application.network.NetworkConfig;
import application.network.NetworkConfigService;
import application.network.NetworkLobbyController;
import kuroyale.domain.Deck;
import kuroyale.infrastructure.SocketNetworkAdapter;

/**
 * Application-level facade for Phase 2 Network Multiplayer.
 * <p>
 * Pattern note: this acts like a small "Service Facade" that creates controllers and hides
 * infrastructure dependencies (Socket adapter, config loader) from UI code.
 */
public class NetworkService {
    private final NetworkConfigService configService;

    public NetworkService(NetworkConfigService configService) {
        this.configService = configService;
    }

    public NetworkConfig loadConfig() {
        return configService != null ? configService.load() : NetworkConfig.defaults();
    }

    public NetworkLobbyController createHostLobby(String name, Deck deck) {
        NetworkConfig config = loadConfig();
        return new NetworkLobbyController(new SocketNetworkAdapter(), config, true, name, deck);
    }

    public NetworkLobbyController createClientLobby(String name, Deck deck) {
        NetworkConfig config = loadConfig();
        return new NetworkLobbyController(new SocketNetworkAdapter(), config, false, name, deck);
    }
}


