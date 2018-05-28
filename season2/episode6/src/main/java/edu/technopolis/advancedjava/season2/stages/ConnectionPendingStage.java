package edu.technopolis.advancedjava.season2.stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

import static edu.technopolis.advancedjava.season2.stages.Utils.*;

public class ConnectionPendingStage implements IStage {

    private SocketChannel client, server;
    private ByteBuffer clientBuffer;

    public ConnectionPendingStage(SocketChannel clientChannel, SocketChannel serverChannel, ByteBuffer clientBuffer) {
        this.client = clientChannel;
        this.server = serverChannel;
        this.clientBuffer = clientBuffer;
    }

    @Override
    public void proceed(int operation, Selector selector, Map<SocketChannel, IStage> stages) {
        try {
            if (!client.isOpen()) {
                if (server.isOpen()) server.close();
                return;
            }
            if (server.finishConnect()) {
                IStage stage = new ConnectionWriteStage(client, server, clientBuffer, ByteBuffer.allocate(BUFFER_SIZE), true);
                stages.put(client, stage);
                stages.put(server, stage);
                server.register(selector, SelectionKey.OP_READ);
                client.register(selector, SelectionKey.OP_WRITE);
            } else {
                clientBuffer.clear();
                reject(ERROR_SOCKS_SERVER);
                client.register(selector, SelectionKey.OP_WRITE);
                stages.put(client, new ConnectionWriteStage(client, server, clientBuffer, ByteBuffer.allocate(BUFFER_SIZE), false));
                if(server.isOpen()) server.close();
            }
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
    }

    private void reject(byte error) {
        clientBuffer.put(SOCKS_VERSION)
                .put(ERROR_SOCKS_SERVER)
                .put(RESERVED_BYTE)
                .put(ADDRESS_TYPE)
                .put(new byte[4])
                .putShort((short) 0);
        clientBuffer.flip();
    }
}
