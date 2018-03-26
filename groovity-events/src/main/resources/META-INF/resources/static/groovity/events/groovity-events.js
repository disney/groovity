/*******************************************************************************
 * © 2018 Disney | ABC Television Group
 *
 * Licensed under the Apache License, Version 2.0 (the "Apache License")
 * with the following modification; you may not use this file except in
 * compliance with the Apache License and the following modification to it:
 * Section 6. Trademarks. is deleted and replaced with:
 *
 * 6. Trademarks. This License does not grant permission to use the trade
 *     names, trademarks, service marks, or product names of the Licensor
 *     and its affiliates, except as required to comply with Section 4(c) of
 *     the License and to reproduce the content of the NOTICE file.
 *
 * You may obtain a copy of the Apache License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License with the above modification is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the Apache License for the specific
 * language governing permissions and limitations under the Apache License.
 *******************************************************************************/
class EventSocket{
	constructor(url){
		if(url === null || url === ''){
			url = "ws"
			if(window.location.protocol === 'https:'){
				url += "s"
			}
			url += "://"+window.location.host+"/ws/events"
		}
		this.url=url
		this.subs = new Map()
		this.cbs = new Map()
		this.opened = false;
		this.openers = new Array()
		this.closers = new Array()
		this.buf = []
		this.reconnect = null;
		this.doConnect()
		this.debug=false;
		this.retryTime = 1000
		this.heartbeat = null
	}
	doConnect(){
		if(this.reconnect!=null){
			window.clearTimeout(this.reconnect)
			this.reconnect=null
		}
		this.ws = new WebSocket(this.url)
		this.opened = false;
		var t = this
		this.ws.onopen = function(){
			if(t.debug){
				console.log("websocket open")
			}
			this.retryTime = 1000
			t.opened = true
			while(t.buf.length >0){
				var b = t.buf.shift()
				if(t.debug){
					console.log("clearing buffer",b)
				}
				t.ws.send(b)
			}
			for(var prop of t.subs.keys()){
				t.doSend(JSON.stringify(
					{ "subscribe" : prop }
				))
			}
			for(var i=0;i<t.openers.length;i++){
				t.openers[i]()
			}
			t.heartbeat = setInterval( function(){
				if(t.debug){
					console.log('sending heartbeat')
				}
				t.emit('heartbeat')
			}, 30000)
		}
		this.ws.onmessage = function(message){
			if(t.debug){
				console.log('got message from server', message)
			}
			try{
				var msg = JSON.parse(message.data)	
				if(msg.event != null){
					var sub = t.subs.get(msg.event)
					if(sub){
						// pass event data to subscribers
						for(var i=0;i<sub.length;i++){
							var s = sub[i]
							s(msg.data)
						}
					}
				}
				else if(msg.id != null){
					var cb = t.cbs.get(msg.id)
					if(cb){
						//process and remove callback
						cb(msg.data)
						t.cbs.delete(msg.id)
					}
				}
			}
			catch(e){
				console.log('error',e)
			}
		}
		this.ws.onclose = function(event){
			t.doReconnect()
		}
		this.ws.onerror = function(event){
			t.doReconnect()
		}
	}
	
	doReconnect(){
		if(this.reconnect==null){
			if(this.opened){
				this.opened = false;
				for(var i=0;i<this.closers.length;i++){
					this.closers[i]()
				}
				window.clearInterval(this.heartbeat)
				this.heartbeat = null
				if(this.debug){
					console.log('cancelled heartbeat')
				}
			}
			var t = this
			if(this.debug){
				console.log("Trying reconnect to ",t.url)
			}
			this.reconnect = window.setTimeout(function(){ t.doConnect() }, this.retryTime)
			if(this.retryTime < 32000){
				this.retryTime *=2
			}
		}
	}
	on(event, handler){
		if(event == 'open'){
			this.openers.push(handler)
			if(this.opened){
				handler()
			}
		}
		else if(event == 'close'){
			this.closers.push(handler)
		}
		else{
			var handlers = this.subs.get(event)
			if(handlers == null){
				handlers = []
				this.subs.set(event, handlers)
			}
			handlers.push( handler )
			if(this.opened === true){
				this.doSend(JSON.stringify(
					{ "subscribe" : event }
				))
			}
		}
	}
	
	doSend(msg){
		if(this.opened === true){
			if(this.debug){
				console.log("sending",msg)
			}
			this.ws.send(msg)
		}
		else{
			if(this.debug){
				console.log("buffering",msg)
			}
			this.buf.push(msg)
		}
	}
	
	emit(event){
		if(this.debug){
			console.log("Emitting ",event,arguments.length,arguments)
		}
		var message = { event: event }
		if(arguments.length > 1){
			var lastArg = arguments[arguments.length-1]
			if(typeof lastArg == 'function'){
				//register callback
				var cbid = this.generateCBID()
				while(typeof this.cbs.get(cbid) != 'undefined'){
					cbid = this.generateCBID()
				}
				this.cbs.set(cbid, lastArg)
				if(this.debug){
					console.log('registered callback',cbid,this.cbs.get(cbid))
				}
				message['id'] = cbid
				if(arguments.length > 2){
					message['data'] = arguments[1]
				}
			}
			else{
				message['data'] = lastArg
			}
		}
		this.doSend(JSON.stringify(message))
	}
	
	generateCBID(){
		return Math.floor(Math.random() * 2000000000).toString()
	}
}
