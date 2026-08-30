package com.studyagent.localserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyagent.model.ActiveTab;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class TabServer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<ActiveTab> latestTab = new AtomicReference<>();

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/tab", this::handleTab);
        server.start();
        System.out.println("TabServer 已启动 端口在: " + port);
    }

    private void handleTab(HttpExchange exchange) throws IOException {
        try {
            String body = new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ActiveTab tab = objectMapper.readValue(body, ActiveTab.class);
            latestTab.set(tab);
            System.out.println("Received tab: " + tab.getUrl());

            byte[] resp = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
        } catch (Exception e) {
            System.out.println("Bad request: " + e.getMessage());
            exchange.sendResponseHeaders(400, -1);
        } finally {
            exchange.close();
        }
    }

    public ActiveTab getLatestTab() {
        return latestTab.get();
    }

}