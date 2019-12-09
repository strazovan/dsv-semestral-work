package cz.strazovan.dsv.sharedvariable.messaging.server;

import cz.strazovan.dsv.sharedvariable.Component;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;
import cz.strazovan.dsv.sharedvariable.messaging.ServiceImpl;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server implements Component, Runnable {

    private static Logger logger = LoggerFactory.getLogger(Server.class);
    private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
    private final io.grpc.Server gRPCServer;
    private final ServiceImpl service;

    public Server(int port, MessageQueue messageQueue) {
        this.service = new ServiceImpl(messageQueue);
        this.gRPCServer = ServerBuilder.forPort(port)
                .addService(this.service)
                .build();
    }

    @Override
    public void start() {
        logger.info("Starting the server...");
        serverExecutor.submit(this);
        logger.info("Server has started...");
    }

    @Override
    public void stop() {
        logger.info("Stopping the server...");
        this.gRPCServer.shutdownNow();
        this.serverExecutor.shutdownNow();
        try {
            this.serverExecutor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            logger.warn("Server shutdown was interrupted.", ex);
        }
        logger.info("Server has stopped...");
    }

    @Override
    public void run() {
        try {
            this.gRPCServer.start();
            this.gRPCServer.awaitTermination();
        } catch (IOException e) {
            logger.error("Failed to start gRPC server...");
        } catch (InterruptedException e) {
            logger.warn("gRPC was interrupted...");
        }
        logger.info("gRPC server has stopped...");
    }
}
