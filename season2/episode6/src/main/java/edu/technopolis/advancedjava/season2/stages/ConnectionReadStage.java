package edu.technopolis.advancedjava.season2.stages;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Optional;

import static edu.technopolis.advancedjava.season2.stages.Utils.*;


public class ConnectionReadStage implements IStage {

    private SocketChannel client, server;
    private ByteBuffer buffer;

    public ConnectionReadStage(SocketChannel clientChannel, ByteBuffer clientBuffer) {
        this.client = clientChannel;
        this.buffer = clientBuffer;
    }

    @Override
    public void proceed(int ignore, Selector selector, Map<SocketChannel, IStage> stages) {
        try {
            if (!client.isOpen()) return;
            int bytes = client.read(buffer);
            buffer.flip();
            if (bytes == -1) {
                if (client.isOpen()) client.close();
                System.out.println("Closing " + client.toString());
                return;
            }

            Optional<ByteBuffer> optional = Optional.of(buffer);
            optional.filter(bb -> bb.position() < 10)
                    .filter(bb -> bb.get() == SOCKS_VERSION)
                    .filter(bb -> bb.get() == CMD_NUMBER)
                    .filter(bb -> bb.get() == RESERVED_BYTE)
                    .filter(bb -> bb.get() == ADDRESS_TYPE);
            if (!optional.isPresent()) {
                buffer.clear();
                reject(ERROR_SOCKS_SERVER);
                stages.put(client, new ConnectionWriteStage(client, server, buffer, null, false));
                client.register(selector, SelectionKey.OP_WRITE);
            }

            byte[] ipv4 = new byte[IPV4_BYTES];
            buffer.get(ipv4);
            short port = buffer.getShort();

            server = SocketChannel.open();
            server.configureBlocking(false);
            server.connect(new InetSocketAddress(InetAddress.getByAddress(ipv4), port));
            server.finishConnect();

            buffer.clear();
            accept(ipv4, port);

            IStage stage = new ConnectionPendingStage(client, server, buffer);
            stages.put(client, stage);
            stages.put(server, stage);

            server.register(selector, SelectionKey.OP_CONNECT);
            client.register(selector, SelectionKey.OP_READ);

        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
    }

    private void accept(byte[] ip, short port) {
        buffer.put(SOCKS_VERSION)
                .put(CONNECTION_PROVIDED_CODE)
                .put(RESERVED_BYTE)
                .put(ADDRESS_TYPE)
                .put(ip)
                .putShort(port);
        buffer.flip();
    }

    private void reject(byte error) {
        buffer.put(SOCKS_VERSION)
                .put(ERROR_SOCKS_SERVER)
                .put(RESERVED_BYTE)
                .put(ADDRESS_TYPE)
                .put(new byte[4])
                .putShort((short) 0);
        buffer.flip();
    }
}
