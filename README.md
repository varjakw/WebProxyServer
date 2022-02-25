# WebProxyServer
Project for Advanced Computer Networks module.

# Overview
The objective of the exercise is to implement a Web Proxy Server. A Web proxy is a local server, which
fetches items from the Web on behalf of a Web client instead of the client fetching them directly. This
allows for caching of pages and access control.

## Functionality 
The program should be able to:
- Respond to HTTP & HTTPS requests and should display each request on a management
console. It should forward the request to the Web server and relay the response to the browser.
- Handle Websocket connections.
- Dynamically block selected URLs via the management console.
- Efficiently cache HTTP requests locally and thus save bandwidth. You must gather timing and
bandwidth data to prove the efficiency of your proxy.
- Handle multiple requests simultaneously by implementing a threaded server
