package cz.strazovan.dsv.sharedvariable.ui;

import cz.strazovan.dsv.sharedvariable.locking.CaRoDistributedLock;
import cz.strazovan.dsv.sharedvariable.messaging.MessageFactory;
import cz.strazovan.dsv.sharedvariable.messaging.client.Client;
import cz.strazovan.dsv.sharedvariable.topology.TopologyChangeListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;
import cz.strazovan.dsv.sharedvariable.ui.topology.TopologyListModel;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

public class ApplicationController implements TopologyChangeListener {

    private TopologyListModel topologyListModel;
    private SharedTextArea sharedTextArea;
    private JButton unlockButton;
    private JButton lockButton;
    private JButton saveButton;
    private JTextField hostnameBox;
    private JTextField portBox;

    private Client client;
    private CaRoDistributedLock lock;
    private String localhostAddress;
    private int port;

    public ApplicationController(String localhostAddress, int port) {
        this.localhostAddress = localhostAddress;
        this.port = port;
    }

    public void lock() {
        this.lockButton.setEnabled(false);
        CompletableFuture.runAsync(lock::lock)
                .thenRun(() -> SwingUtilities.invokeLater(() -> {
                    this.unlockButton.setEnabled(true);
                    this.saveButton.setEnabled(true);
                    this.sharedTextArea.setEditable(true);
                }));
    }

    public void unlock() {
        this.unlockButton.setEnabled(false);
        this.saveButton.setEnabled(false);
        this.sharedTextArea.setEditable(false);
        CompletableFuture.runAsync(lock::unlock)
                .thenRun(() -> SwingUtilities.invokeLater(() -> this.lockButton.setEnabled(true)));
    }

    public void save() {
        CompletableFuture.runAsync(() ->
                client.broadcast(MessageFactory.createDataChangeMessage(this.sharedTextArea.getText())));
    }

    public void connect() {
        final var targetHostname = hostnameBox.getText().isEmpty() ? localhostAddress : hostnameBox.getText();
        final var to = new TopologyEntry(targetHostname, Integer.parseInt(portBox.getText()));
        client.sendMessage(to, MessageFactory.createRegisterNodeMessage(localhostAddress, port));
    }

    public void disconnect() {
        client.broadcast(MessageFactory.createDisconnectMessage(localhostAddress, port));
    }

    @Override
    public void onNewNode(TopologyEntry nodeId) {
        this.topologyListModel.addElement(nodeId);
        client.sendMessage(nodeId, MessageFactory.createDataChangeMessage(this.sharedTextArea.getText()));
    }

    @Override
    public void onNodeRemoved(TopologyEntry nodeId) {
        this.topologyListModel.removeElement(nodeId);
    }

    public void setTopologyListModel(TopologyListModel topologyListModel) {
        this.topologyListModel = topologyListModel;
    }

    public void setSharedTextArea(SharedTextArea sharedTextArea) {
        this.sharedTextArea = sharedTextArea;
    }

    public void setUnlockButton(JButton unlockButton) {
        this.unlockButton = unlockButton;
    }

    public void setLockButton(JButton lockButton) {
        this.lockButton = lockButton;
    }

    public void setSaveButton(JButton saveButton) {
        this.saveButton = saveButton;
    }

    public void setHostnameBox(JTextField hostnameBox) {
        this.hostnameBox = hostnameBox;
    }

    public void setPortBox(JTextField portBox) {
        this.portBox = portBox;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setLock(CaRoDistributedLock lock) {
        this.lock = lock;
    }

}
