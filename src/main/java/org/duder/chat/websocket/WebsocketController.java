package org.duder.chat.websocket;

import org.duder.chat.dto.ChatMessageDto;
import org.duder.chat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
public class WebsocketController {
    private final MessageService messageService;


    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    public WebsocketController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/message")
    public ChatMessageDto sendMessage(@Payload ChatMessageDto chatMessageDto) {
        messageService.sendMessage(chatMessageDto);
        return chatMessageDto;
    }

    @MessageMapping("/message/channel")
    public ChatMessageDto sendMessageToChannel(@Payload ChatMessageDto chatMessageDto) {
        messageService.sendChannelMessage(chatMessageDto, Integer.valueOf(chatMessageDto.getTo()));
        return chatMessageDto;
    }

    @MessageMapping("/message/user")
    public void sendMessage(@Payload ChatMessageDto chatMessageDto, Principal user, @Header("simpSessionId") String sessionId) {
        simpMessagingTemplate.convertAndSendToUser(chatMessageDto.getTo(), "/queue/reply", chatMessageDto);
    }
}

