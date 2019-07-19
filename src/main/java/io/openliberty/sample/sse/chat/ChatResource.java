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

import javax.annotation.security.RolesAllowed;
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
public class ChatResource {

    @Context
    private SecurityContext secContext;

    @Context
    private Sse sse;

    private SseBroadcaster broadcaster;

    private SseBroadcaster agentBroadcaster;

    private synchronized SseBroadcaster getOrCreateBroadcaster() {
        if (broadcaster == null) {
            broadcaster = this.sse.newBroadcaster();
        }
        return broadcaster;
    }

    private synchronized SseBroadcaster getOrCreateAgentBroadcaster() {
        if (agentBroadcaster == null) {
            agentBroadcaster = this.sse.newBroadcaster();
        }
        return agentBroadcaster;
    }

    @GET
    @Path("register")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(@Context SseEventSink sink, @Context Sse sse) {
        SseBroadcaster b = getOrCreateBroadcaster();
        b.register(sink);
    }

    @GET
    @Path("registerAgent")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void registerAgent(@Context SseEventSink sink, @Context Sse sse) {
        register(sink, sse); // register for normal messages and commands
        SseBroadcaster b = getOrCreateAgentBroadcaster();
        b.register(sink);
    }

    @PUT
    public void sendMessage(@QueryParam("message") String message) {
        SseBroadcaster b = message.startsWith("/") ? getOrCreateAgentBroadcaster() : getOrCreateBroadcaster();
        b.broadcast(newMessage(secContext.getUserPrincipal().getName(), message));
    }

    OutboundSseEvent newMessage(String sender, String message) {
        ChatMessage chatMessage = new ChatMessage(sender, message);
        return sse.newEventBuilder()
                  .data(ChatMessage.class, chatMessage)
                  .id(""+chatMessage.getMsgID())
                  .mediaType(MediaType.APPLICATION_JSON_TYPE)
                  .build();
    }
}