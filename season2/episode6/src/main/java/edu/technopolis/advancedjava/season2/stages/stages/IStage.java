package edu.technopolis.advancedjava.season2.stages.stages;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public interface IStage {
    void proceed(int OP, Selector selector, Map<SocketChannel, IStage> map);
}
