package edu.technopolis.advancedjava.season2.stages.stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class CommunicationStage implements IStage{

    private SocketChannel client, server;
    private ByteBuffer clientBuffer, serverBuffer;

    public CommunicationStage(SocketChannel clientChannel, SocketChannel serverChannel, ByteBuffer clientBuffer, ByteBuffer serverBuffer) throws IOException {
        this.client = clientChannel;
        this.server = serverChannel;
        this.clientBuffer = clientBuffer;
        this.serverBuffer = serverBuffer;
    }

    @Override
    public void proceed(int operation, Selector selector, Map<SocketChannel, IStage> map) {
        try {
            if (!client.isOpen()) {
                if (server.isOpen()) server.close();
                return;
            }
            if (!server.isOpen()) {
                if (client.isOpen()) client.close();
                return;
            }
            if (operation == SelectionKey.OP_READ) {
                if (clientBuffer.hasRemaining()) return;
                clientBuffer.clear();
                int bytes = client.read(clientBuffer);
                clientBuffer.flip();
                if ( bytes > 0) {
                    server.register(selector, SelectionKey.OP_WRITE);
                } else {
                    if (client.isOpen()) client.close();
                    if (selector.isOpen()) server.close();
                    return;
                }
            } else if(operation == SelectionKey.OP_WRITE) {
                while(serverBuffer.hasRemaining()) {
                    client.write(serverBuffer);
                }
                client.register(selector, SelectionKey.OP_READ);
            }

        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
    }
}
