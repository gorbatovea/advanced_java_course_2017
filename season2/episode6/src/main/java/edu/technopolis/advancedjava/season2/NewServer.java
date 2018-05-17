package edu.technopolis.advancedjava.season2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.istack.internal.NotNull;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import edu.technopolis.advancedjava.season2.stages.AuthMethodStage;
import edu.technopolis.advancedjava.season2.stages.CommunicationStage;
import edu.technopolis.advancedjava.season2.stages.ConnectionStage;
import edu.technopolis.advancedjava.season2.stages.IStage;

/**
 * Сервер, построенный на API java.nio.* . Работает единственный поток,
 * обрабатывающий события, полученные из селектора.
 * Нельзя блокировать или нагружать долгоиграющими действиями данный поток, потому что это
 * замедлит обработку соединений.
 */
public class NewServer {
    private static final int PORT = 1080;

    public static void main(String[] args) {
        Map<SocketChannel, IStage> map = new HashMap<>();
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()){
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select(); //блокирующий вызов
                @NotNull
                Set<SelectionKey> keys = selector.selectedKeys();
                if (keys.isEmpty()) {
                    continue;
                }
                //при обработке ключей из множества selected, необходимо обязательно очищать множество.
                //иначе те же ключи могут быть обработаны снова
                keys.removeIf(key -> {
                    if (!key.isValid()) {
                        return true;
                    }
                    if (key.isAcceptable()) {
                        return accept(map, key);
                    }
                    if (key.isWritable()) {
                        //Внимание!!!
                        //Важно, чтобы при обработке не было долгоиграющих (например, блокирующих операций),
                        //поскольку текущий поток занимается диспетчеризацией каналов и должен быть всегда доступен
                        IStage stage = map.get(key.channel()).proceed();
                        if (stage == null) {
                            closeChannel((SocketChannel) key.channel());
                            return true;
                        }
                        map.put((SocketChannel) key.channel(), stage);
                        key.interestOps(SelectionKey.OP_WRITE);
                        return true;
                    }
                    return true;
                });
                //удаление закрытых каналов из списка обрабатываемых
                map.keySet().removeIf(channel -> !channel.isOpen());
            }

        } catch (IOException e) {
            LogUtils.logException("Unexpected error on processing incoming connection", e);
        }
    }

    private static boolean accept(Map<SocketChannel, IStage> map, SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = null;
        try {
            System.out.print("Accepting ");
            channel = serverChannel.accept(); //non-blocking call
            System.out.print(channel.getRemoteAddress());
            channel.configureBlocking(false);
            channel.register(key.selector(), SelectionKey.OP_WRITE);
            map.put(channel, new AuthMethodStage(channel));
            System.out.println(" is accepted");
        } catch (IOException e) {
            System.out.println(" is not accepted");
            LogUtils.logException("Failed to process channel " + channel, e);
            if (channel != null) {
                closeChannel(channel);
            }
        }
        return true;
    }

    private static void closeChannel(SocketChannel channel) {
        try {
            System.out.println("Closing channel " + channel.getRemoteAddress());
            channel.close();
        } catch (IOException e) {
            System.err.println("Failed to close channel " + channel);
        }
    }
}
