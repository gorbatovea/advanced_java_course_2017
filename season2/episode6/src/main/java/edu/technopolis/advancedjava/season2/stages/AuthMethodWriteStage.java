package edu.technopolis.advancedjava.season2.stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class AuthMethodWriteStage implements IStage {

    private boolean accepted;
    private SocketChannel client;
    private ByteBuffer buffer;

    public AuthMethodWriteStage(SocketChannel clientChannel, ByteBuffer clientBuffer, boolean flag) {
        this.client = clientChannel;
        this.buffer = clientBuffer;
        this.accepted = flag;
    }

    @Override
    public void proceed(int ignore, Selector selector, Map<SocketChannel, IStage> stages) {
        try {
            if (!client.isOpen()) return;
            while (buffer.hasRemaining()) {
                client.write(buffer);
            }
            buffer.clear();
            if (accepted) {
                client.register(selector, SelectionKey.OP_READ);
                stages.put(client, new ConnectionReadStage(client, buffer));
                System.out.println("Auth completed for " + client.getRemoteAddress());
            } else {
                if (client.isOpen()) client.close();
                System.out.println("Auth rejected for " + client.getRemoteAddress());
            }
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
    }
}
