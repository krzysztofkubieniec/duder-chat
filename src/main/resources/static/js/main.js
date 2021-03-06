'use strict';
var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];
var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');

var messageToUserForm = document.querySelector('#messageToUserForm');
var messageToUserInput = document.querySelector('#messageToUser');
var userInput = document.querySelector('#user');

var stompClient = null;
var username = null;
var password = null;
var sessionToken = null;
var HttpClient = function() {
    this.get = function(aUrl, aCallback, sessionToken) {
        var anHttpRequest = new XMLHttpRequest();
        anHttpRequest.onreadystatechange = function() {
            if (anHttpRequest.readyState == 4 && anHttpRequest.status == 200)
                aCallback(anHttpRequest.responseText);
        }
        anHttpRequest.open( "GET", aUrl, true);
        if (sessionToken) {
            anHttpRequest.setRequestHeader('Authorization', sessionToken);
        }
        anHttpRequest.send( null );
    }
}
var client = new HttpClient();

function connect(event) {
    username = document.querySelector('#name').value.trim();
    password = document.querySelector('#password').value.trim();

    if(username) {

        client.get('/user/login?login=' + username +'&password=' + password, function(tokenResponse) {
                sessionToken = tokenResponse;
                client.get('/api/getChatState', function(response) {
                        var messages = JSON.parse(response)
                        messages.forEach(function(message) {
                            onMessageReceivedJson(message)
                        }, sessionToken);
                    }, sessionToken);
        }, null);
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        var headers = {
            login: username,
            password: password
        };

        stompClient.connect(headers, onConnected, onError);
    }

    event.preventDefault();
}


function onConnected() {
    // Subscribe to the Public Topic
    stompClient.subscribe('/topic/public', onMessageReceived);
    stompClient.subscribe('/user/queue/reply', onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/message",
        {},
        JSON.stringify({sender: username, type: 'JOIN'})
    )

    connectingElement.classList.add('hidden');
}


function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}


function sendMessage(event) {
    var messageContent = messageInput.value.trim();

    if(messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageInput.value,
            type: 'CHAT'
        };

        stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

function sendMessageToUser(event) {
    var messageContent = messageToUserInput.value.trim();
    var user = document.querySelector('#user').value;

    if(messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageContent,
            to: user,
            type: 'CHAT'
        };

        stompClient.send("/app/message/user", {}, JSON.stringify(chatMessage));
        messageContent = '';
    }
    event.preventDefault();
}

function onMessageReceivedJson(message) {
    var messageElement = document.createElement('li');

    if(message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' joined!';
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left!';
    } else {
        messageElement.classList.add('chat-message');

        var avatarElement = document.createElement('i');
        var avatarText = document.createTextNode(message.sender[0]);
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(message.sender);

        messageElement.appendChild(avatarElement);

        var usernameElement = document.createElement('span');
        var usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

    var textElement = document.createElement('p');
    var messageText = document.createTextNode(message.content);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);

    onMessageReceivedJson(message)
}


function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    var index = Math.abs(hash % colors.length);
    return colors[index];
}

usernameForm.addEventListener('submit', connect, true)
messageForm.addEventListener('submit', sendMessage, true)
messageToUserForm.addEventListener('submit', sendMessageToUser, true)
