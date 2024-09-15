package org.example.gitmazonwebsocketserver;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import lombok.extern.log4j.Log4j2;
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

@Log4j2
public class TerminalWebSocketHandler extends TextWebSocketHandler {
//    private PtyProcess process;
//    private OutputStream outputStream;
//    private ExecutorService outputExecutor;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connection established");

        String[] command = {
            "ssh", "-t", "-o", "LogLevel=QUIET", "-i", "~/.ssh/webserver.pem", "ubuntu@18.182.42.57",
            "sudo docker exec -it pusheventtest bash",
        };
        PtyProcess process = new PtyProcessBuilder(command).start();
        InputStream inputStream = process.getInputStream();

        // using single thread to send data back to frontend
        ExecutorService outputExecutor = Executors.newSingleThreadExecutor();
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

        session.getAttributes().put("process", process);
        session.getAttributes().put("outputExecutor", outputExecutor);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        PtyProcess process = (PtyProcess) session.getAttributes().get("process");

        if(process != null) {
            OutputStream outputStream = process.getOutputStream();
            String input = message.getPayload();
            outputStream.write(input.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } else {
            log.error("Pty Process not found for session" + session.getId());
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        PtyProcess process = (PtyProcess) session.getAttributes().get("process");
        ExecutorService outputExecutor = (ExecutorService) session.getAttributes().get("outputExecutor");

        if(process != null) {
            process.destroy();
        }

        if(outputExecutor != null) {
            outputExecutor.shutdown();
        }
    }
}
