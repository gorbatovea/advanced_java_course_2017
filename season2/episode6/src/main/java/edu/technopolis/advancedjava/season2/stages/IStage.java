package edu.technopolis.advancedjava.season2.stages;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public interface IStage {
    public IStage proceed(Selector selector, Map<SocketChannel, IStage> map);
}
