package org.example.gitmazonwebsocketserver;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TerminalWebSocketHandler extends TextWebSocketHandler {
    private PtyProcess process;
    private OutputStream outputStream;
    private ExecutorService outputExecutor;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String[] command = {
            "ssh", "-t", "-o", "LogLevel=QUIET", "-i", "~/.ssh/webserver.pem", "ubuntu@18.182.42.57",
            "sudo docker exec -it pusheventtest bash",
        };
        process = new PtyProcessBuilder(command).start();
        outputStream = process.getOutputStream();
        InputStream inputStream = process.getInputStream();


        // using single thread to send data back to frontend
        outputExecutor = Executors.newSingleThreadExecutor();
        outputExecutor.submit(() -> {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    session.sendMessage(new TextMessage(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String input = message.getPayload();
        outputStream.write(input.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        process.destroy();
        outputExecutor.shutdown();
    }
}
