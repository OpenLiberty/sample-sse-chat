// ******************************************************************************
//  Copyright (c) 2019 IBM Corporation and others.
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  which accompanies this distribution, and is available at
//  http://www.eclipse.org/legal/epl-v10.html
//
//  Contributors:
//  IBM Corporation - initial API and implementation
// ******************************************************************************
package io.openliberty.sample.sse.chat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import org.eclipse.microprofile.rest.client.RestClientBuilder;

public class ChatAgent implements Runnable, ClientRequestFilter, ClientResponseFilter {
    private final static Logger LOG = Logger.getLogger(ChatAgent.class.getName());
    private final static int PREVIOUS_MESSSAGE_CAPACITY = 1000;
    private final static String AGENT_NAME = "SseAgent";
    private final static String AGENT_PASSWORD = "pass";
    private final String url;
    private final ChatClient client;
    private volatile boolean keepRunning = true;
    private List<ChatMessage> previousMessages = new LinkedList<>();

    ChatAgent(String url) {
        this.url = url;
        client = RestClientBuilder.newBuilder()
                                  .baseUri(URI.create(url))
                                  .register(this)
                                  .build(ChatClient.class);
    }

    @Override
    public void run() {
        LOG.info("agent run " +  url);
        while(keepRunning) {
            Client client = ClientBuilder.newClient().register(this);
            WebTarget target = client.target(url + "/registerAgent");
            AtomicBoolean keepProcessing = new AtomicBoolean(true);
            try (SseEventSource source = SseEventSource.target(target).build()) {
                source.register(
                    inboundSseEvent -> {
                        try {
                            processMessage(inboundSseEvent.readData(ChatMessage.class, MediaType.APPLICATION_JSON_TYPE));
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    },
                    throwable -> {
                        LOG.log(Level.WARNING, "Error during SSE processing - will reconnect", throwable);
                        keepProcessing.set(false);
                    });
                source.open();
                while (keepProcessing.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        LOG.info("agent has stopped");
    }

    private void processMessage(ChatMessage msg) {
        String msgText = msg.getMessage();
        if (msgText.startsWith("/")) {
            LOG.info("agent received command: " + msgText);
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
            LOG.info("agent received new message " + msgText);
            previousMessages.add(0, msg);
            if (previousMessages.size() > PREVIOUS_MESSSAGE_CAPACITY) {
                previousMessages.remove(PREVIOUS_MESSSAGE_CAPACITY - 1);
            }
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
        client.sendMessage(data);
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

    void replay(String data) {
        try {
            int numOfMessages = Math.min(Integer.parseInt(data), previousMessages.size());
            String replay = "Replaying last " + numOfMessages + " messages:";
            List<ChatMessage> list = previousMessages.subList(0, numOfMessages);
            for (int i=numOfMessages-1; i >= 0 ; i--) {
                ChatMessage msg = list.get(i);
                replay += "<br>" + msg.getUser() + " said \"" + msg.getMessage() + "\" at " + msg.getTimestamp(); 
            }
            say(replay);
        } catch (Throwable t) {
            say("I didn't understand that replay command - replay commands should be in the format, \"/replay n\"" +
                " where n is the number of messages to replay");
            t.printStackTrace();
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
                "Basic " + Base64.getEncoder().encodeToString((AGENT_NAME + ":" + AGENT_PASSWORD).getBytes("UTF-8")));
        LOG.info("using SseAgent:pass header : " + requestContext.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        LOG.info("agent sse register responsecode: " + responseContext.getStatus());
    }
}