package application.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple immutable-ish state container for the network lobby UI.
 */
public class NetworkLobbyState {
    private String localName;
    private String remoteName;
    private List<String> localDeckNames = new ArrayList<>();
    private List<String> remoteDeckNames = new ArrayList<>();
    private List<String> localDeckCardIds = new ArrayList<>();
    private List<String> remoteDeckCardIds = new ArrayList<>();
    private boolean localReady;
    private boolean remoteReady;
    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;
    private String statusDetail = "";
    private int latencyMillis = -1;
    private boolean isHost;

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public List<String> getLocalDeckNames() {
        return new ArrayList<>(localDeckNames);
    }

    public void setLocalDeckNames(List<String> localDeckNames) {
        this.localDeckNames = localDeckNames != null ? new ArrayList<>(localDeckNames) : new ArrayList<>();
    }

    public List<String> getRemoteDeckNames() {
        return new ArrayList<>(remoteDeckNames);
    }

    public void setRemoteDeckNames(List<String> remoteDeckNames) {
        this.remoteDeckNames = remoteDeckNames != null ? new ArrayList<>(remoteDeckNames) : new ArrayList<>();
    }

    public List<String> getLocalDeckCardIds() {
        return new ArrayList<>(localDeckCardIds);
    }

    public void setLocalDeckCardIds(List<String> localDeckCardIds) {
        this.localDeckCardIds = localDeckCardIds != null ? new ArrayList<>(localDeckCardIds) : new ArrayList<>();
    }

    public List<String> getRemoteDeckCardIds() {
        return new ArrayList<>(remoteDeckCardIds);
    }

    public void setRemoteDeckCardIds(List<String> remoteDeckCardIds) {
        this.remoteDeckCardIds = remoteDeckCardIds != null ? new ArrayList<>(remoteDeckCardIds) : new ArrayList<>();
    }

    public boolean isLocalReady() {
        return localReady;
    }

    public void setLocalReady(boolean localReady) {
        this.localReady = localReady;
    }

    public boolean isRemoteReady() {
        return remoteReady;
    }

    public void setRemoteReady(boolean remoteReady) {
        this.remoteReady = remoteReady;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(String statusDetail) {
        this.statusDetail = statusDetail != null ? statusDetail : "";
    }

    public int getLatencyMillis() {
        return latencyMillis;
    }

    public void setLatencyMillis(int latencyMillis) {
        this.latencyMillis = latencyMillis;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }
}


