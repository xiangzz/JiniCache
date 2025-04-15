package com.jinicache.http;

import com.jinicache.cache.CacheManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 启动服务器
     */
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpServerHandler(cacheManager));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("HTTP server started on port {}", port);

            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("HTTP server interrupted", e);
        } finally {
            shutdown();
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        logger.info("HTTP server stopped");
    }
} 