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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class ChatMessage {

	private static final AtomicLong idGenerator = new AtomicLong();
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
	
	private final long msgID;
	private final String timestamp;
	private final String user;
	private final String message;
	
	ChatMessage(String user, String message) {
		this.user = user;
		this.message = message;
		this.msgID = idGenerator.incrementAndGet();
		this.timestamp = LocalDateTime.now().format(dtf);
	}

	public long getMsgID() {
		return msgID;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getUser() {
		return user;
	}

	public String getMessage() {
		return message;
	}
}

