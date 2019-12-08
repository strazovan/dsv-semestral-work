package cz.strazovan.dsv.sharedvariable;

import cz.strazovan.dsv.NodeId;
import cz.strazovan.dsv.RegisterNode;
import cz.strazovan.dsv.sharedvariable.locking.CaRoDistributedLock;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;
import cz.strazovan.dsv.sharedvariable.messaging.client.Client;
import cz.strazovan.dsv.sharedvariable.messaging.server.Server;
import cz.strazovan.dsv.sharedvariable.topology.Topology;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        final int port;
        if (args.length == 0) {
            System.out.println("No port parameter provided. Server will be started on port 18080.");
            port = 8080;
        } else {
            port = Integer.parseInt(args[0]);
        }

        final var messageQueue = new MessageQueue();
        messageQueue.start();

        final var topology = new Topology("127.0.0.1", port);
        topology.register(messageQueue);
        final var lock = new CaRoDistributedLock(topology);
        lock.register(messageQueue);

        final var server = new Server(port, messageQueue);
        server.start();

        final var client = new Client();
        client.start();
        topology.registerListener(client);

        final var frame = new JFrame();

        final var lockButton = new JButton("Lock");
        lockButton.addActionListener(e -> {
            lock.lock();
        });

        final var unlockButton = new JButton("Unlock");
        unlockButton.addActionListener(e -> {
            lock.unlock();
        });

        final var hostnameBox = new JTextField("IP");
        hostnameBox.setPreferredSize(new Dimension(50, 20));
        final var portBox = new JTextField("port");
        portBox.setPreferredSize(new Dimension(50, 20));
        final var connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            final var to = new TopologyEntry(hostnameBox.getText(), Integer.parseInt(portBox.getText()));

            client.sendMessage(to, RegisterNode.newBuilder()
                    .setNodeId(NodeId.newBuilder()
                            .setIp(hostnameBox.getText())
                            .setPort(to.getPort())
                            .build())
                    .build());
        });

        final var connectPanel = new JPanel();
        connectPanel.add(hostnameBox);
        connectPanel.add(portBox);
        connectPanel.add(connectButton);

        final var mainPanel = new JPanel();
        mainPanel.add(new JLabel("Own topology entry: " + topology.getOwnTopologyEntry()));
        mainPanel.add(lockButton);
        mainPanel.add(unlockButton);
        mainPanel.add(connectPanel);

        frame.add(mainPanel);

        frame.setName("Distributed todo list");
        frame.setSize(600, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            messageQueue.stop();
            server.stop();
        }));
    }
}
