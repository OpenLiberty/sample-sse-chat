package io.openliberty.sample.sse.chat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEventSource;

public class ChatAgent implements Runnable, ClientRequestFilter, ClientResponseFilter {
    private final String AGENT_NAME = "SseAgent";
    private final ChatResource chat;
    private final String url;
    private volatile boolean keepRunning = true;

    ChatAgent(ChatResource chat, String url) {
        this.chat = chat;
        this.url = url;
    }

    @Override
    public void run() {
        System.out.println("agent run " +  url);
        Client client = ClientBuilder.newClient().register(this);
        WebTarget target = client.target(url);
        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register((inboundSseEvent) -> {
                try {
                ChatMessage msg = inboundSseEvent.readData(ChatMessage.class, MediaType.APPLICATION_JSON_TYPE);
                String msgText = msg.getMessage();
                if (msgText.startsWith("/")) {
                    System.out.println("agent received command: " + msg);
                    String cmd;
                    String param;
                    int idx = msgText.indexOf(" ");
                    if (idx > 0) {
                        cmd = msgText.substring(1, idx);
                        param = msgText.substring(idx).trim();
                    } else {
                        cmd = msgText.substring(1);
                        param = "";
                    }
                    invoke(cmd, param);
                } else {
                    System.out.println("agent received new message " + msgText);
                }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
            source.open();
            while (keepRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void invoke(String methodName, String param) {
        try {
            Method m = ChatAgent.class.getDeclaredMethod(methodName, String.class);
            m.invoke(this, param);
        } catch (Throwable t) {
            say("I don't know that command: " + methodName);
        }
    }

    void say(String data) {
        chat.broadcast(AGENT_NAME, data);
    }

    void reminder(String data) {
        try {
            String[] splitData = data.split(" ", 2);
            int waitTime = Integer.parseInt(splitData[0]);
            Executors.newSingleThreadScheduledExecutor()
                     .schedule((Runnable) () -> { say(splitData[1]); }, waitTime, TimeUnit.SECONDS);
        } catch (Throwable t) {
            say("I didn't understand that reminder - reminders should be in the format, \"/reminder 30 some text\"" +
                " - then I'll remind you of \"some text\" in 30 seconds.");
        }
    }

    void shutdown(String data) {
        say("Shutting down now... bye!");
        keepRunning=false;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        // add authorization header
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString("SseAgent:pass".getBytes("UTF-8")));
        System.out.println("using SseAgent:pass header : " + requestContext.getHeaderString(HttpHeaders.AUTHORIZATION));

    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        System.out.println("agent sse register responsecode: " + responseContext.getStatus());
    }
}
