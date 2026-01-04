package application.network;

/**
 * Application-layer abstraction for the network transport.
 * <p>
 * This enables unit testing and keeps UI decoupled from socket implementation details.
 */
public interface NetworkAdapter {

    interface Listener {
        void onStatus(ConnectionStatus status, String detail);
        void onMessage(NetworkMessage message);
        void onError(String message, Exception error);
    }

    void host(int port, Listener listener);

    void connect(String host, int port, Listener listener);

    /**
     * Allows re-wiring listeners during session handoff (Lobby -> Match).
     */
    void setListener(Listener listener);

    void send(NetworkMessage message);

    void close();
}


