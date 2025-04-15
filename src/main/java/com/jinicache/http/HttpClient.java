package com.jinicache.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP客户端实现
 * 用于节点间的HTTP通信
 */
public class HttpClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    private final EventLoopGroup group;

    /**
     * 构造函数
     */
    public HttpClient() {
        this.group = new NioEventLoopGroup();
    }

    /**
     * 发送GET请求
     * @param url 目标URL
     * @return 响应内容的CompletableFuture
     */
    public CompletableFuture<byte[]> get(String url) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        try {
            logger.debug("Sending GET request to: {}", url);
            URI uri = new URI(url);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new HttpClientHandler(future));
                        }
                    });

            ChannelFuture channelFuture = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.GET,
                    uri.getRawPath() + "?" + uri.getRawQuery()
            );
            request.headers().set(HttpHeaderNames.HOST, uri.getHost());
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            channelFuture.channel().writeAndFlush(request);
            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) channelFuture1 -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("Connection closed"));
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
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        try {
            logger.debug("Sending PUT request to: {}", url);
            URI uri = new URI(url);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new HttpClientHandler(future));
                        }
                    });

            ChannelFuture channelFuture = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.PUT,
                    uri.getRawPath() + "?" + uri.getRawQuery()
            );
            request.headers().set(HttpHeaderNames.HOST, uri.getHost());
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
            request.content().writeBytes(content);

            channelFuture.channel().writeAndFlush(request);
            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) channelFuture1 -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("Connection closed"));
                }
            });
        } catch (Exception e) {
            logger.error("Error during PUT request to {}: {}", url, e.getMessage());
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        logger.info("Shutting down HttpClient");
        group.shutdownGracefully();
    }
} 