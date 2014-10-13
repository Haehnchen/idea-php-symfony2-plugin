package fr.adrienbrault.idea.symfony2plugin.remote.util;

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public class HttpExchangeUtil {

    public static void sendResponse(HttpExchange xchg, String content) throws IOException {
        xchg.sendResponseHeaders(200, content.length());
        OutputStream os = xchg.getResponseBody();
        os.write(content.getBytes());
        os.close();
    }

    public static void sendResponse(HttpExchange xchg, Collection<String> contents) throws IOException {
        sendResponse(xchg, StringUtils.join(contents, "\n"));
    }

    public static void sendResponse(HttpExchange xchg, StringBuilder response) throws IOException {
        xchg.sendResponseHeaders(200, response.length());
        OutputStream os = xchg.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }

}
