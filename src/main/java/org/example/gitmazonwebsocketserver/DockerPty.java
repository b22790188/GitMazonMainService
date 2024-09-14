package org.example.gitmazonwebsocketserver;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.OutputStream;

public class DockerPty {

    private PtyProcess ptyProcess;
    private WebSocketSession session;

    public DockerPty(WebSocketSession session) {
        this.session = session;
    }

    public void start() throws IOException {

        String [] command = {"docker", "exec", "-it", "gitmazon", "bash"};

        // init process
        ptyProcess = new PtyProcessBuilder(command).start();


    }
}
