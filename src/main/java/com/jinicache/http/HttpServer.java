package com.jinicache.http;

import com.jinicache.cache.CacheManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HTTP服务器实现
 * 用于处理节点间的HTTP请求
 */
public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private final int port;
    private final CacheManager cacheManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 构造函数
     * @param port 服务器端口
     * @param cacheManager 缓存管理器
     */
    public HttpServer(int port, CacheManager cacheManager) {
        this.port = port;
        this.cacheManager = cacheManager;
    }

    /**
     * 启动服务器（异步）
     * @return 启动结果的CompletableFuture
     */
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> startFuture = new CompletableFuture<>();
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            logger.info("Starting HTTP server on port {}", port);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpServerHandler(cacheManager));
                        }
                    });

            bootstrap.bind(port).addListener((ChannelFutureListener) bindFuture -> {
                if (bindFuture.isSuccess()) {
                    serverChannel = bindFuture.channel();
                    logger.info("HTTP server started successfully on port {}", port);
                    startFuture.complete(null);
                    
                    // 监听服务器关闭
                    bindFuture.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                        logger.info("HTTP server on port {} has been closed", port);
                        shutdown();
                    });
                } else {
                    logger.error("Failed to bind server to port {}", port, bindFuture.cause());
                    shutdown();
                    startFuture.completeExceptionally(bindFuture.cause());
                }
            });
        } catch (Exception e) {
            logger.error("Failed to start server on port {}", port, e);
            shutdown();
            startFuture.completeExceptionally(e);
        }
        
        return startFuture;
    }

    /**
     * 启动服务器（同步）
     * @throws Exception 启动失败时抛出异常
     */
    public void startSync() throws Exception {
        start().get(30, TimeUnit.SECONDS);
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        logger.info("HTTP server stopped");
    }
}