package edu.technopolis.advancedjava.season2.stages.stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Optional;

import static edu.technopolis.advancedjava.season2.stages.stages.Utils.*;

public class AuthMethodReadStage implements IStage {

    private ByteBuffer buffer;
    private SocketChannel client;

    public AuthMethodReadStage(SocketChannel clientChannel, ByteBuffer clientBuffer) {
        this.client = clientChannel;
        this.buffer = clientBuffer;
    }

    @Override
    public void proceed(int ignore, Selector selector, Map<SocketChannel, IStage> stages) {
        try {
            int bytes = client.read(buffer);
            if (bytes < 3) return;
            buffer.flip();
            Optional<ByteBuffer> optional = Optional.of(buffer);
            optional.filter(bb -> bb.get() == SOCKS_VERSION)
                    .filter(bb -> isAcceptableAuthMethod(bb.get(), bb));
            buffer.clear();
            if (optional.isPresent()) {
                accept(stages);
            } else {
                reject(stages);
            }
            client.register(selector, SelectionKey.OP_WRITE);
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
    }

    private void accept(Map<SocketChannel, IStage> map) {
        buffer.put(SOCKS_VERSION)
                .put(AUTH_METHOD);
        buffer.flip();
        map.put(client, new AuthMethodWriteStage(client, buffer, true));
    }

    private void reject(Map<SocketChannel, IStage> map) {
        buffer.put(SOCKS_VERSION)
                .put(AUTH_REJECT);
        buffer.flip();
        map.put(client, new AuthMethodWriteStage(client, buffer, false));
    }

    private boolean isAcceptableAuthMethod(int methodsNumber, ByteBuffer bb) {
        if (methodsNumber < 1) return false;
        for(int i = 0; i < methodsNumber; i++) {
            if (bb.get() == AUTH_METHOD) {
                return true;
            }
        }
        return false;
    }
}
