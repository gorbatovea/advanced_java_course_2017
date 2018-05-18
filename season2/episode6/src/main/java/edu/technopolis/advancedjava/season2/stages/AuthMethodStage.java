package edu.technopolis.advancedjava.season2.stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class AuthMethodStage implements IStage {
    public static int CONST_BUFFER_SIZE = 2;
    public static int SOCKS_VERSION_BYTE_POSITION = 0;
    public static int METHODS_NUMBER_BYTE_POSITION = 1;
    public static final byte SOCKS_VERSION = 0x05;
    public static final byte AUTH_ACCEPTABLE_METHOD = 0x00;


    private static byte[] ACCEPT_AUTH_METHOD = new byte[]{0x05, 0x00};
    private static byte[] REJECT_AUTH_METHOD = new byte[]{0x05, 0xF};

    private ByteBuffer constBuffer, offeredMethods;
    private SocketChannel sourceChannel;

    public AuthMethodStage(SocketChannel sourceChannel) {
        this.constBuffer= ByteBuffer.allocate(CONST_BUFFER_SIZE);
        this.sourceChannel = sourceChannel;
    }

    @Override
    public IStage proceed(Selector selector, Map<SocketChannel, IStage> map) {
        try {
            sourceChannel.read(constBuffer);
            if (constBuffer.position() != constBuffer.limit())
                return this;
            if (constBuffer.get(SOCKS_VERSION_BYTE_POSITION) != SOCKS_VERSION) {
                sendReject();
                return null;
            }
            if (constBuffer.get(METHODS_NUMBER_BYTE_POSITION) < 1) {
                sendReject();
                return null;
            }
            if (offeredMethods == null)
                offeredMethods = ByteBuffer.allocate(constBuffer.get(METHODS_NUMBER_BYTE_POSITION));
            sourceChannel.read(offeredMethods);
            if (offeredMethods.position() != offeredMethods.limit())
                return this;
            for (byte b :
                    offeredMethods.array()) {
                if (b == AUTH_ACCEPTABLE_METHOD) {
                    System.out.println("Auth completed for " + sourceChannel.getRemoteAddress());
                    sendAccept();
                    sourceChannel.register(selector, SelectionKey.OP_READ);
                    map.put(sourceChannel, new ConnectionStage(sourceChannel));
                    return new ConnectionStage(sourceChannel);
                }
            }
            sendReject();
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
        return null;
    }

    private void sendAccept() throws IOException {
        sourceChannel.write(ByteBuffer.wrap(ACCEPT_AUTH_METHOD));
    }

    private void sendReject() throws IOException {
        sourceChannel.write(ByteBuffer.wrap(REJECT_AUTH_METHOD));
    }
}
