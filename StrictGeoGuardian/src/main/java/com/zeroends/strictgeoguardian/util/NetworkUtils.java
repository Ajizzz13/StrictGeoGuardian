package com.zeroends.strictgeoguardian.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class NetworkUtils {

    public static int getSocketTTL(InetSocketAddress address) {
        try (SocketChannel channel = SocketChannel.open()) {
            channel.socket().connect(address, 3000);
            int ttl = channel.socket().getTrafficClass();
            return ttl;
        } catch (IOException e) {
            return -1;
        }
    }

    public static int getTCPMSS(InetSocketAddress address) {
        try (SocketChannel channel = SocketChannel.open()) {
            channel.socket().connect(address, 3000);
            return channel.socket().getSendBufferSize();
        } catch (IOException e) {
            return -1;
        }
    }
}
