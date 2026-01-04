package kuroyale.infrastructure;

import application.network.ConnectionStatus;
import application.network.NetworkAdapter;
import application.network.NetworkMessage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Infrastructure implementation of {@link NetworkAdapter} using {@link Socket} / {@link ServerSocket}.
 * <p>
 * Keeps a single peer connection (Host ↔ Client) as required by the Phase 2 Host/Client model.
 */
public class SocketNetworkAdapter implements NetworkAdapter {

    private Thread ioThread;
    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Listener listener;

    @Override
    public void host(int port, Listener listener) {
        this.listener = listener;
        close();
        running.set(true);

        ioThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setSoTimeout(1000);
                while (running.get()) {
                    if (this.listener != null) {
                        this.listener.onStatus(ConnectionStatus.CONNECTING, "Hosting on port " + port + "…");
                    }
                    Socket client = null;
                    while (running.get() && client == null) {
                        try {
                            client = serverSocket.accept();
                        } catch (SocketTimeoutException timeout) {
                            // allow loop to observe running flag
                        }
                    }
                    if (client == null) {
                        break;
                    }
                    initializeSocket(client);
                    // blocks until disconnect
                    readLoop();
                    safeClose();
                    if (this.listener != null && running.get()) {
                        this.listener.onStatus(ConnectionStatus.DISCONNECTED, "Opponent disconnected (waiting to reconnect)");
                    }
                }
            } catch (IOException e) {
                if (this.listener != null) {
                    this.listener.onError("Host failed", e);
                }
            } finally {
                running.set(false);
                safeClose();
                if (this.listener != null) {
                    this.listener.onStatus(ConnectionStatus.DISCONNECTED, "Disconnected");
                }
            }
        }, "kuroyale-net-host");
        ioThread.setDaemon(true);
        ioThread.start();
    }

    @Override
    public void connect(String host, int port, Listener listener) {
        this.listener = listener;
        close();
        running.set(true);

        ioThread = new Thread(() -> {
            if (this.listener != null) {
                this.listener.onStatus(ConnectionStatus.CONNECTING, "Connecting to " + host + ":" + port + "…");
            }
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, port), 5000);
                initializeSocket(s);
                readLoop();
            } catch (IOException e) {
                if (this.listener != null) {
                    this.listener.onError("Connect failed", e);
                }
            } finally {
                running.set(false);
                safeClose();
                if (this.listener != null) {
                    this.listener.onStatus(ConnectionStatus.DISCONNECTED, "Disconnected");
                }
            }
        }, "kuroyale-net-client");
        ioThread.setDaemon(true);
        ioThread.start();
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void initializeSocket(Socket s) throws IOException {
        this.socket = s;
        this.writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
        if (this.listener != null) {
            this.listener.onStatus(ConnectionStatus.CONNECTED, "Connected");
        }
    }

    private void readLoop() {
        Socket s = this.socket;
        if (s == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                NetworkMessage msg = NetworkMessage.parse(line);
                if (msg == null) {
                    continue;
                }
                Listener l = this.listener;
                if (l != null) {
                    l.onMessage(msg);
                }
            }
        } catch (IOException e) {
            Listener l = this.listener;
            if (l != null) {
                l.onError("I/O error", e);
            }
        }
    }

    @Override
    public void send(NetworkMessage message) {
        if (message == null) {
            return;
        }
        BufferedWriter w = this.writer;
        if (w == null) {
            return;
        }
        try {
            w.write(message.encode());
            w.newLine();
            w.flush();
        } catch (IOException e) {
            Listener l = this.listener;
            if (l != null) {
                l.onError("Send failed", e);
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        safeClose();
    }

    private void safeClose() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
        writer = null;
        socket = null;
    }
}


