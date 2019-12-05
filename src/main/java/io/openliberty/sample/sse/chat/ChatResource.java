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

import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

@Path("chat")
@RolesAllowed("ChatUsers")
@ApplicationScoped
public class ChatResource {

    @Context
    private SecurityContext secContext;

    private Sse sse;
    private SseBroadcaster broadcaster;
    private SseBroadcaster agentBroadcaster;

    private Optional<SseBroadcaster> getBroadcaster() {
        return Optional.ofNullable(broadcaster);
    }

    private Optional<SseBroadcaster> getAgentBroadcaster() {
        return Optional.ofNullable(agentBroadcaster);
    }

    @GET
    @Path("register")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public synchronized void register(@Context SseEventSink sink, @Context Sse sse) {
        this.sse = Optional.ofNullable(this.sse).orElse(sse);
        this.broadcaster = Optional.ofNullable(this.broadcaster).orElse(sse.newBroadcaster());
        this.broadcaster.register(sink);
    }

    @GET
    @Path("registerAgent")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public synchronized void registerAgent(@Context SseEventSink sink, @Context Sse sse) {
        register(sink, sse);
        this.agentBroadcaster = Optional.ofNullable(this.agentBroadcaster).orElse(sse.newBroadcaster());
        agentBroadcaster.register(sink);
    }

    @PUT
    public void sendMessage(@QueryParam("message") String message) {
        String user = secContext.getUserPrincipal() == null ? "unknown user" : secContext.getUserPrincipal().getName();
        sendMessage(user, message);
    }

    private void sendMessage(String user, String message) {
        Optional<SseBroadcaster> b = message.startsWith("/") ? getAgentBroadcaster() : getBroadcaster();
        if (b.isPresent()) {
            b.get().broadcast(newMessage(user, message));
        } else {
            System.out.println("cannot send message - broadcaster not yet initialized");
        }
    }

    private OutboundSseEvent newMessage(String sender, String message) {
        ChatMessage chatMessage = new ChatMessage(sender, message);
        return sse.newEventBuilder()
                  .data(ChatMessage.class, chatMessage)
                  .id(""+chatMessage.getMsgID())
                  .mediaType(MediaType.APPLICATION_JSON_TYPE)
                  .build();
    }
}