package edu.technopolis.advancedjava.season2.stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class CommunicationStage implements IStage{

    private SocketChannel sourceChannel, distChannel;
    private ByteBuffer sourceBuffer = ByteBuffer.allocate(500), distBuffer = ByteBuffer.allocate(500);

    public CommunicationStage(SocketChannel source, SocketChannel dist) throws IOException {
        this.sourceChannel = source;
        this.distChannel = dist;
    }

    @Override
    public IStage proceed(Selector selector, Map<SocketChannel, IStage> map) {
        try {
            int sourceBytes = 0, distBytes = 0;
            sourceBytes = sourceChannel.read(sourceBuffer);
            distBytes = distChannel.read(distBuffer);
            if (sourceBytes == -1) {
                if (distBytes != -1) distChannel.close();
                return null;
            }
            if (distBytes == -1) {
                if (sourceBytes != -1) sourceChannel.close();
                return null;
            }
            write(sourceBytes, sourceBuffer, distChannel);
            write(distBytes, distBuffer, sourceChannel);
            return this;
        } catch (IOException iOE) {
            iOE.printStackTrace();
            return null;
        }
    }
    private void write(int bytes, ByteBuffer src, SocketChannel dist) throws IOException {
        if (bytes > 0) {
            src.flip();
            while (src.hasRemaining()) {
                dist.write(src);
            }
            src.compact();
        }
    }
}
