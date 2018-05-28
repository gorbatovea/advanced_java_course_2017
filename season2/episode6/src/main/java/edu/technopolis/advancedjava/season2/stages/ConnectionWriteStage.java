package edu.technopolis.advancedjava.season2.stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class ConnectionWriteStage implements IStage {

    private SocketChannel client, server;
    private ByteBuffer clientBuffer, serverBuffer;
    private boolean accepted;

    public ConnectionWriteStage(SocketChannel clientChannel, SocketChannel serverChannel, ByteBuffer clientBuffer, ByteBuffer serverBuffer, boolean flag) {
        this.client = clientChannel;
        this.server = serverChannel;
        this.clientBuffer = clientBuffer;
        this.serverBuffer = serverBuffer;
        this.accepted = flag;
    }

    @Override
    public void proceed(int ignore, Selector selector, Map<SocketChannel, IStage> stages) {
        try {
            if (!client.isOpen()) {
                if (server.isOpen()) selector.close();
                return;
            }
            while(clientBuffer.hasRemaining()) {
                client.write(clientBuffer);
            }
            if (accepted) {
                serverBuffer.flip();
                stages.put(client, new CommunicationStage(client, server, clientBuffer, serverBuffer));
                stages.put(server, new CommunicationStage(server, client, serverBuffer, clientBuffer));
                client.register(selector, SelectionKey.OP_READ);
                server.register(selector, SelectionKey.OP_READ);
                System.out.println("Connected " + server.getRemoteAddress());
            } else {
                System.out.println("Connection rejected " + server.getRemoteAddress());
            }
        }catch (IOException iOE) {
            iOE.printStackTrace();
        }
    }
}
