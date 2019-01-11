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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class ChatMessage {

	private static final AtomicLong idGenerator = new AtomicLong();
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	
	private final long msgID;
	private final String timestamp;
	private final String user;
	private final String message;
	
	ChatMessage(String user, String message) {
		this.user = user;
		this.message = message;
		this.msgID = idGenerator.incrementAndGet();
		this.timestamp = dateFormat.format(new Date());
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

