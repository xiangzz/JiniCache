package com.jinicache.http;

import com.jinicache.cache.CacheManager;
import com.jinicache.cache.Group;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * HTTP请求处理器
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);
    private final CacheManager cacheManager;

    /**
     * 构造函数
     * @param cacheManager 缓存管理器
     */
    public HttpServerHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        String uri = request.uri();
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        String path = decoder.path();
        Map<String, List<String>> params = decoder.parameters();

        if (path.equals("/api/cache")) {
            handleCacheRequest(ctx, request, params);
        } else {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
        }
    }

    /**
     * 处理缓存请求
     */
    private void handleCacheRequest(ChannelHandlerContext ctx, FullHttpRequest request, Map<String, List<String>> params) {
        String groupName = getParam(params, "group");
        String key = getParam(params, "key");

        if (groupName == null || key == null) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        Group group = cacheManager.getGroup(groupName);
        if (group == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        if (request.method() == HttpMethod.GET) {
            byte[] value = group.get(key);
            if (value == null) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
            sendResponse(ctx, value);
        } else if (request.method() == HttpMethod.PUT) {
            byte[] content = new byte[request.content().readableBytes()];
            request.content().readBytes(content);
            group.getCache().put(key, content);
            sendResponse(ctx, "OK".getBytes(CharsetUtil.UTF_8));
        } else {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
        }
    }

    /**
     * 获取参数值
     */
    private String getParam(Map<String, List<String>> params, String name) {
        List<String> values = params.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    /**
     * 发送响应
     */
    private void sendResponse(ChannelHandlerContext ctx, byte[] content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 发送错误响应
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer("Error: " + status.toString(), CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Channel exception caught", cause);
        ctx.close();
    }
} 