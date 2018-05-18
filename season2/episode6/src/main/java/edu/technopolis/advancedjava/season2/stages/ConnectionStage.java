package edu.technopolis.advancedjava.season2.stages;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;

public class ConnectionStage implements IStage {
    public static final int CONST_BUFFER_SIZE = 4;
    public static final int PORT_BUFFER_SIZE = 2;
    public static final int ADDRESS_BUFFER_SIZE = 4;

    public static final int CMD_NUMBER_BYTE_POSITION = 1;
    public static final int ADDRESS_TYPE_BYTE_POSITION = 3;

    public static final byte CMD_NUMBER = 0x01;
    public static final byte ADDRESS_TYPE = 0x01;

    public static final byte[] RESPONDING_TEMPLATE = new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,};

    //Succeed
    public static final byte CONNECTION_PROVIDED = 0x00;
    //Error numbers
    public static final byte ERROR_SOCKS_SERVER = 0x01;
    public static final byte ERROR_HOST_UNREACHABLE = 0x04;
    public static final byte ERROR_UNSUPPORTED_CMD = 0x07;
    public static final byte ERROR_UNSUPPORTED_ADDRESS_TYPE= 0x08;

    public static final String ADDRESS_SEPARATOR = ".";


    private SocketChannel sourceChannel;
    private SocketChannel distChannel;
    private ByteBuffer constBuffer;
    private ByteBuffer addressBuffer;
    private ByteBuffer portBuffer;

    public ConnectionStage(SocketChannel sourceChannel) {
        this.sourceChannel = sourceChannel;
        this.constBuffer = ByteBuffer.allocate(CONST_BUFFER_SIZE);
        this.addressBuffer = ByteBuffer.allocate(ADDRESS_BUFFER_SIZE);
        this.portBuffer = ByteBuffer.allocate(PORT_BUFFER_SIZE);
    }

    @Override
    public IStage proceed(Selector selector, Map<SocketChannel, IStage> map) {
        try {
            sourceChannel.read(constBuffer);
            if (constBuffer.position() != constBuffer.limit())
                return this;
            if (constBuffer.get(AuthMethodStage.SOCKS_VERSION_BYTE_POSITION) != AuthMethodStage.SOCKS_VERSION) {
                sendReject(ERROR_UNSUPPORTED_CMD);
                return null;
            }
            if (constBuffer.get(CMD_NUMBER_BYTE_POSITION) != CMD_NUMBER) {
                sendReject(ERROR_UNSUPPORTED_CMD);
                return null;
            }
            if(constBuffer.get(ADDRESS_TYPE_BYTE_POSITION) != ADDRESS_TYPE) {
                sendReject(ERROR_UNSUPPORTED_ADDRESS_TYPE);
                return null;
            }
            sourceChannel.read(addressBuffer);
            if (addressBuffer.position() != addressBuffer.limit())
                return this;
            sourceChannel.read(portBuffer);
            if (portBuffer.position() != portBuffer.limit())
                return this;
            if (distChannel == null) {
                distChannel = SocketChannel.open();
                distChannel.configureBlocking(false);
                distChannel.connect(new InetSocketAddress(getAddress(addressBuffer.array()), getPort(portBuffer.array())));
            }
            distChannel.finishConnect();
            if (!distChannel.isConnected()){
                distChannel.register(selector, SelectionKey.OP_CONNECT);
                map.put(distChannel, this);
                return this;
            }
            sourceChannel.register(selector, SelectionKey.OP_READ);
            map.put(sourceChannel, new CommunicationStage(sourceChannel, distChannel));
            distChannel.register(selector, SelectionKey.OP_READ);
            map.put(distChannel, new CommunicationStage(sourceChannel, distChannel));
            System.out.println("Connected " + distChannel.getRemoteAddress());

            sendAccept();
            return new CommunicationStage(sourceChannel, distChannel);
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
        return null;
    }

    private String getAddress(byte[] addr) {
        ByteBuffer b1 = ByteBuffer.allocate(Integer.BYTES).putInt(addr[0]),
                b2 = ByteBuffer.allocate(Integer.BYTES).putInt(addr[1]),
                b3 = ByteBuffer.allocate(Integer.BYTES).putInt(addr[2]),
                b4 = ByteBuffer.allocate(Integer.BYTES).putInt(addr[3]);
        b1.flip(); b2.flip(); b3.flip(); b4.flip();
        return Integer.toString(b1.getInt() & 0xFF) + ADDRESS_SEPARATOR
                + Integer.toString(b2.getInt() & 0xFF) + ADDRESS_SEPARATOR
                + Integer.toString(b3.getInt() & 0xFF) + ADDRESS_SEPARATOR
                +Integer.toString(b4.getInt() & 0xFF);
    }

    private int getPort(byte[] port) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        byteBuffer.put((byte)0); byteBuffer.put((byte)0);
        byteBuffer.put(port);
        byteBuffer.flip();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getInt();
    }

    private void sendAccept() throws IOException {
        byte[] response = Arrays.copyOf(RESPONDING_TEMPLATE, RESPONDING_TEMPLATE.length);
        response[4] = addressBuffer.get(0);
        response[5] = addressBuffer.get(1);
        response[6] = addressBuffer.get(2);
        response[7] = addressBuffer.get(3);
        response[8] = portBuffer.get(0);
        response[9] = portBuffer.get(1);

        sourceChannel.write(ByteBuffer.wrap(response));
    }

    private void sendReject(byte errorType) throws IOException {
        byte[] response = Arrays.copyOf(RESPONDING_TEMPLATE, RESPONDING_TEMPLATE.length);
        response[1] = errorType;
        response[4] = addressBuffer.get(0);
        response[5] = addressBuffer.get(1);
        response[6] = addressBuffer.get(2);
        response[7] = addressBuffer.get(3);
        response[8] = portBuffer.get(0);
        response[9] = portBuffer.get(1);

        sourceChannel.write(ByteBuffer.wrap(response));
    }
}
