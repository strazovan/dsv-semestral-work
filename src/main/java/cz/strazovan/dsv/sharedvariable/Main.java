package cz.strazovan.dsv.sharedvariable;

import cz.strazovan.dsv.DataChange;
import cz.strazovan.dsv.NodeId;
import cz.strazovan.dsv.RegisterNode;
import cz.strazovan.dsv.sharedvariable.clock.Clock;
import cz.strazovan.dsv.sharedvariable.locking.CaRoDistributedLock;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;
import cz.strazovan.dsv.sharedvariable.messaging.client.Client;
import cz.strazovan.dsv.sharedvariable.messaging.server.Server;
import cz.strazovan.dsv.sharedvariable.topology.Topology;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;
import cz.strazovan.dsv.sharedvariable.ui.SharedTextArea;
import cz.strazovan.dsv.sharedvariable.ui.StatusBar;
import cz.strazovan.dsv.sharedvariable.ui.topology.TopologyList;
import cz.strazovan.dsv.sharedvariable.ui.topology.TopologyListModel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

public class Main {

    public static void main(String[] args) {
        final int port;
        if (args.length == 0) {
            System.out.println("No port parameter provided. Server will be started on port 18080.");
            port = 8080;
        } else {
            port = Integer.parseInt(args[0]);
        }

        final String localhostAddress;
        try {
            localhostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            throw new RuntimeException("Failed to start.", ex);
        }


        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            throw new RuntimeException("Failed to set system look and feel.", e);
        }

        final var messageQueue = new MessageQueue();
        messageQueue.start();

        final var topology = new Topology(localhostAddress, port);
        topology.register(messageQueue);

        final var server = new Server(port, messageQueue);
        server.start();

        final var client = new Client(topology::reportDeadNode);
        client.start();
        topology.registerListener(client);
        topology.setClient(client);


        final var lock = new CaRoDistributedLock(topology, client);
        lock.register(messageQueue);

        // TODO refactor UI definition, this is just for getting things done and make something i can test
        final var frame = new JFrame();

        final var textArea = new SharedTextArea();
        textArea.setFont(textArea.getFont().deriveFont(16f));
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.register(messageQueue);

        final var lockButton = new JButton("Lock");
        final var unlockButton = new JButton("Unlock");
        final var saveButton = new JButton("Save");
        saveButton.setEnabled(false);

        unlockButton.addActionListener(e -> {
            unlockButton.setEnabled(false);
            saveButton.setEnabled(false);
            textArea.setEditable(false);
            CompletableFuture.runAsync(lock::unlock)
                    .thenRun(() -> SwingUtilities.invokeLater(() -> lockButton.setEnabled(true)));
        });
        unlockButton.setEnabled(false);

        lockButton.addActionListener(e -> {
            lockButton.setEnabled(false);
            CompletableFuture.runAsync(lock::lock)
                    .thenRun(() -> SwingUtilities.invokeLater(() -> {
                        unlockButton.setEnabled(true);
                        saveButton.setEnabled(true);
                        textArea.setEditable(true);
                    }));

        });

        saveButton.addActionListener(e -> {
            CompletableFuture.runAsync(() -> {
                client.broadcast(DataChange.newBuilder()
                        .setTime(Clock.INSTANCE.tick())
                        .setData(textArea.getText())
                        .build());
            });
        });

        final var hostnameBox = new JTextField("");
        hostnameBox.setPreferredSize(new Dimension(50, 20));
        final var portBox = new JTextField("port");
        portBox.setPreferredSize(new Dimension(50, 20));
        final var connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            final var targetHostname = hostnameBox.getText().isEmpty() ? localhostAddress : hostnameBox.getText();
            final var to = new TopologyEntry(targetHostname, Integer.parseInt(portBox.getText()));
            client.sendMessage(to, RegisterNode.newBuilder()
                    .setNodeId(NodeId.newBuilder()
                            .setIp(localhostAddress)
                            .setPort(port)
                            .build())
                    .setTime(Clock.INSTANCE.tick())
                    .build());

        });

        final var connectPanel = new JPanel();
        connectPanel.add(hostnameBox);
        connectPanel.add(portBox);
        connectPanel.add(connectButton);

        final var topologyListModel = new TopologyListModel();
        topology.registerListener(topologyListModel);
        final var topologyList = new TopologyList(topologyListModel);

        final var mainPanel = new JPanel();
        final var mainLayout = new BorderLayout();
        mainPanel.setLayout(mainLayout);
        final var topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.add(lockButton);
        topPanel.add(unlockButton);
        topPanel.add(saveButton);
        topPanel.add(connectPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        final var topologyPanel = new JPanel();
        topologyPanel.setLayout(new BoxLayout(topologyPanel, BoxLayout.Y_AXIS));
        topologyPanel.add(new JLabel("Topology"));
        final JLabel ownTopologyEntry = new JLabel("Own entry: " + topology.getOwnTopologyEntry());
        final JLabel currentTime = new JLabel("Current time is: " + Clock.INSTANCE.getCurrentTime());
        Clock.INSTANCE.registerClockListener(tick -> currentTime.setText("Current time is: " + tick));

        ownTopologyEntry.setFont(ownTopologyEntry.getFont().deriveFont(Font.BOLD));
        topologyPanel.add(ownTopologyEntry);
        topologyPanel.add(currentTime);
        topologyPanel.add(new JScrollPane(topologyList));
        topologyList.setBorder(LineBorder.createBlackLineBorder());
        topologyPanel.setPreferredSize(new Dimension(300, 400));
        mainPanel.add(topologyPanel, BorderLayout.EAST);


        mainPanel.add(textArea, BorderLayout.CENTER);
        final var statusBar = new StatusBar();
        statusBar.setPreferredSize(new Dimension(600, 25));
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        frame.add(mainPanel);

        frame.setName("Distributed todo list");
        frame.setSize(700, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            messageQueue.stop();
            server.stop();
        }));
    }
}
