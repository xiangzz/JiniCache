package com.jinicache.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP客户端实现
 * 用于节点间的HTTP通信
 * 实现了连接池机制以避免资源泄漏
 */
public class HttpClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final ConcurrentHashMap<String, Channel> connectionPool;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private static final int MAX_CONTENT_LENGTH = 65536;

    /**
     * 构造函数
     */
    public HttpClient() {
        this.group = new NioEventLoopGroup();
        this.connectionPool = new ConcurrentHashMap<>();
        this.bootstrap = new Bootstrap();
        initBootstrap();
    }

    /**
     * 初始化Bootstrap配置
     */
    private void initBootstrap() {
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
    }

    /**
     * 发送GET请求
     * @param url 目标URL
     * @return 响应内容的CompletableFuture
     */
    public CompletableFuture<byte[]> get(String url) {
        if (shutdown.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("HttpClient is shutdown"));
        }
        
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        try {
            logger.debug("Sending GET request to: {}", url);
            URI uri = new URI(url);
            String hostPort = uri.getHost() + ":" + uri.getPort();
            
            getOrCreateConnection(hostPort).whenComplete((channel, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                
                try {
                    String path = uri.getRawPath();
                    if (uri.getRawQuery() != null) {
                        path += "?" + uri.getRawQuery();
                    }
                    
                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                            HttpVersion.HTTP_1_1,
                            HttpMethod.GET,
                            path
                    );
                    request.headers().set(HttpHeaderNames.HOST, uri.getHost());
                    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    request.headers().set(HttpHeaderNames.USER_AGENT, "JiniCache-HttpClient/1.0");
                    
                    // 设置响应处理器
                    channel.pipeline().addLast("response-handler-" + System.nanoTime(), 
                            new HttpClientHandler(future));
                    
                    channel.writeAndFlush(request).addListener((ChannelFutureListener) writeFuture -> {
                        if (!writeFuture.isSuccess()) {
                            future.completeExceptionally(writeFuture.cause());
                        }
                    });
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            logger.error("Error during GET request to {}: {}", url, e.getMessage());
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 发送PUT请求
     * @param url 目标URL
     * @param content 请求内容
     * @return 响应内容的CompletableFuture
     */
    public CompletableFuture<byte[]> put(String url, byte[] content) {
        if (shutdown.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("HttpClient is shutdown"));
        }
        
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        try {
            logger.debug("Sending PUT request to: {} with {} bytes", url, content.length);
            URI uri = new URI(url);
            String hostPort = uri.getHost() + ":" + uri.getPort();
            
            getOrCreateConnection(hostPort).whenComplete((channel, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                
                try {
                    String path = uri.getRawPath();
                    if (uri.getRawQuery() != null) {
                        path += "?" + uri.getRawQuery();
                    }
                    
                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                            HttpVersion.HTTP_1_1,
                            HttpMethod.PUT,
                            path
                    );
                    request.headers().set(HttpHeaderNames.HOST, uri.getHost());
                    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
                    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
                    request.headers().set(HttpHeaderNames.USER_AGENT, "JiniCache-HttpClient/1.0");
                    request.content().writeBytes(content);
                    
                    // 设置响应处理器
                    channel.pipeline().addLast("response-handler-" + System.nanoTime(), 
                            new HttpClientHandler(future));
                    
                    channel.writeAndFlush(request).addListener((ChannelFutureListener) writeFuture -> {
                        if (!writeFuture.isSuccess()) {
                            future.completeExceptionally(writeFuture.cause());
                        }
                    });
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            logger.error("Error during PUT request to {}: {}", url, e.getMessage());
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 获取或创建连接
     * @param hostPort 主机:端口
     * @return Channel的CompletableFuture
     */
    private CompletableFuture<Channel> getOrCreateConnection(String hostPort) {
        Channel existingChannel = connectionPool.get(hostPort);
        if (existingChannel != null && existingChannel.isActive()) {
            return CompletableFuture.completedFuture(existingChannel);
        }
        
        // 移除无效连接
        if (existingChannel != null) {
            connectionPool.remove(hostPort, existingChannel);
        }
        
        CompletableFuture<Channel> future = new CompletableFuture<>();
        String[] parts = hostPort.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new HttpClientCodec());
                ch.pipeline().addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
                
                // 添加连接关闭监听器
                ch.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    connectionPool.remove(hostPort, ch);
                    logger.debug("Connection to {} closed and removed from pool", hostPort);
                });
            }
        });
        
        bootstrap.connect(host, port).addListener((ChannelFutureListener) connectFuture -> {
            if (connectFuture.isSuccess()) {
                Channel channel = connectFuture.channel();
                connectionPool.put(hostPort, channel);
                logger.debug("New connection established to {}", hostPort);
                future.complete(channel);
            } else {
                logger.error("Failed to connect to {}: {}", hostPort, connectFuture.cause().getMessage());
                future.completeExceptionally(connectFuture.cause());
            }
        });
        
        return future;
    }
    
    /**
     * 清理连接池中的无效连接
     */
    private void cleanupInactiveConnections() {
        connectionPool.entrySet().removeIf(entry -> {
            Channel channel = entry.getValue();
            if (!channel.isActive()) {
                logger.debug("Removing inactive connection to {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 获取连接池状态信息
     * @return 连接池大小
     */
    public int getConnectionPoolSize() {
        cleanupInactiveConnections();
        return connectionPool.size();
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            logger.info("Shutting down HttpClient...");
            
            // 关闭所有连接
            connectionPool.values().forEach(channel -> {
                if (channel.isActive()) {
                    channel.close();
                }
            });
            connectionPool.clear();
            
            // 关闭事件循环组
            group.shutdownGracefully();
            logger.info("HttpClient shutdown completed");
        }
    }
}