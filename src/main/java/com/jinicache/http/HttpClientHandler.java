package com.jinicache.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP客户端处理器
 */
public class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientHandler.class);
    private final CompletableFuture<byte[]> future;

    /**
     * 构造函数
     * @param future 用于存储响应结果的CompletableFuture
     */
    public HttpClientHandler(CompletableFuture<byte[]> future) {
        this.future = future;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            byte[] content = new byte[response.content().readableBytes()];
            response.content().readBytes(content);
            future.complete(content);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Channel exception caught", cause);
        future.completeExceptionally(cause);
    }
} 