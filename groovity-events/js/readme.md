## Groovity-Events.js

This package contains a minified, ES5 compatible copy of the javascript client library for the groovity-events websocket multiplexing library.  While the groovity-events jar distribution contains this javascript client library as well, this node module is provided for convenience when bundling a front-end application together.

This library supports a similar syntax to Socket.io for interacting with a Groovity server with the groovity-events module installed; the event model is application specific except for the pre-definited `open` and `close` events.


Sample usage:

```
 <script type="text/javascript" src = "/static/groovity/events/groovity-events.js"> </script>
 <script type="text/javascript">
	window.socket = new EventSocket('wss://my.groovity.host/ws/events')
    //emit with no callback is fire and forget from client to server
    socket.emit('page-load', window.location.href)

    //registering an 'open' handler will fire on first connect AND after a reconnect
    socket.on('open', function(){
      setOnline(true)
      //emit with a callback mimics a classic request/response
      socket.emit('fetch-initial-data', window.location.href, function(data){
    		renderInitial(data);
    	})
    })

    //registering a 'close' handler will fire every time the websocket is closed
    socket.on('close', function(){
        setOnline(false)
    })

    //here, on is used to subscribe to server-initiated events
    socket.on('data-update', function(data){
        update(data)
    })

    socket.on('private-message', function(data){
        showPM(data)
    })
  </script>
 ```


For information on the groovity-events module, please visit [the Groovity wiki](https://github.com/disney/groovity/wiki/Events)