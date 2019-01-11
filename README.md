![](https://github.com/OpenLiberty/open-liberty/blob/master/logos/logo_horizontal_light_navy.png)

# SseChat Sample
## A simple chat room using JAX-RS 2.1 Server Sent Events (SSE)

This sample is intended to help understand how to write SSE applications that broadcast events to multiple clients.
In this sample, a remote client would register (GET) with the `ChatResource` - this establishes an SSE connection - and then clients could send (PUT) a message.
The ChatResource would then broadcast that message to all registered clients.

To run this sample, first [download](https://github.com/OpenLiberty/sample-sse-chat/archive/master.zip) or clone this repo - to clone:
```
git clone git@github.com:OpenLiberty/sample-sse-chat.git
```

From inside the sample-sse-chat directory, build and start the application in Open Liberty with the following command:
```
mvn clean package liberty:run-server
```

The server will listen on port 9080 by default.  You can change the port (for example, to port 9081) by adding `mvn clean package liberty:run-server -DtestServerHttpPort=9081` to the end of the Maven command.

After the server has started, you should see some output like this:
![](https://github.com/OpenLiberty/sample-sse-chat/raw/master/img/CmdServerStarted.png)

At this point, you can enter the following URL into your browser:
```
http://localhost:9080/SseChatSample/index.html
```

I recommend connecting with multiple browsers, so that you can have a conversation like this one:
![](https://github.com/OpenLiberty/sample-sse-chat/raw/master/img/SseChatBrowser.png)

Note that SSEs do not work in all browsers.  Consult [Wikipedia](https://en.wikipedia.org/wiki/Server-sent_events#Web_browsers) for more information on browsers that support SSEs.

Press `Ctrl-C` from the command line to stop the server.

Please take a look at the source code that makes this possible.  If you run into any problems with the sample, please let us know by opening a new issue.

Thanks!

