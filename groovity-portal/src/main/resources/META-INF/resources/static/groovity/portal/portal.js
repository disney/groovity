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
if(typeof socket !== 'undefined'){
	socket.on('inbox-status', function(data){
		setPortalNoticeButton(data.fresh)
	})
	socket.on('new-notice', function(data){
		setPortalNoticeButton(true)
	})
	socket.on('delivery-deleted', function(){
		if($("#portal-notice-trigger").is(':checked')){
			loadInboxContents()
		}
	})
}

function setPortalNoticeButton(active){
	if(active){
		$("#portal-notice-button").addClass('active');
		if($("#portal-notice-trigger").is(':checked')){
			loadInboxContents();
		}
	}
	else{
		$("#portal-notice-button").removeClass('active');
	}
}

function clickLink(el,link){
	el.click(function(){
		window.location.href=link;
	});
}

function closeNotice(el,id){
	el.click(function(){
		socket.emit('delete-delivery',id)
		return false;
	});
}

function renderInbox(data){
	var el = $("#portal-notice-list") 
	el.empty();
	if(data.length==0){
		el.append($("<div>No notices</div>"));
	}
	for(var i=0;i<data.length;i++){
		var notice = data[i];
		var dv = $('<div class="portal-notice">');
		var tmpl = '<a class="notice-close" id="closeNotice'+notice.id+'">X</a>';
		if(notice.priority ==255){
			tmpl += '<div class="notice-priority">!</div>';
		}
		tmpl += '<div class="notice-msg">'+notice.message+'</div>';
		tmpl += '<div class="notice-sent">'+notice.sent+'</div>';
		tmpl += '<div class="notice-sender">'+notice.sender+'</div>';
		dv.html(tmpl);
		el.append(dv);
		if(notice.link!=null && notice.link!=''){
			clickLink(dv,notice.link);
		}
		closeNotice($("#closeNotice"+notice.id),notice.id);
	}
}

function loadInboxContents(){
	socket.emit('load-notices', function(data){
		renderInbox(data);
		window.portalInboxAccessTimer = setTimeout(function(){
			socket.emit('inbox-accessed', function(data){
				$("#portal-notice-button").removeClass('active');
			});
		},2000);
	})
}
var allMenuTriggers = ["#portal-notice-trigger","#portal-menu-trigger","#portal-tools-trigger"]
var menuCleaner = function(){
	if(this.checked){
		for(var i=0;i<allMenuTriggers.length;i++){
			var mt = $(allMenuTriggers[i])[0];
			if(mt.id!=this.id){
				mt.checked=false;
			}
		}
	}
}

$(function(){
	//set up notice loading
	$("#portal-notice-trigger").change(function(){
		if(this.checked){
			//load inbox
			loadInboxContents();
		}
		else{
			//cancel timer
			clearTimeout(window.portalInboxAccessTimer);
		}
	});
	//set up menu closer
	for(var i=0;i<allMenuTriggers.length;i++){
		$(allMenuTriggers[i]).change(menuCleaner);
	}
});