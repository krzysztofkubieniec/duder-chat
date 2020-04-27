package org.duder.integration;


import org.duder.dto.chat.ChatMessage;
import org.duder.dto.chat.MessageType;
import org.duder.chat.dao.Message;
import org.duder.chat.repository.MessageRepository;
import org.duder.user.dao.User;
import org.duder.user.repository.UserRepository;
import org.duder.utils.DataSQLValues;
import org.duder.utils.MySQLContainerProvider;
import org.duder.utils.MyWebSocketClient;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class WebsocketIT {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;

    @LocalServerPort
    private int port;
    private String url;

    private static final String SEND_MESSAGE_ENDPOINT = "/app/message";
    private static final String SEND_MESSAGE_TO_CHANNEL_ENDPOINT = "/app/message/channel";
    private static final String SEND_MESSAGE_TO_USER_ENDPOINT = "/app/message/user";
    private static final String SUBSCRIBE_CHAT_ENDPOINT = "/topic/public";

    private final String CONTENT = "Content";
    private final MessageType MESSAGE_TYPE = MessageType.CHAT;

    @ClassRule
    public static GenericContainer mysqlContainer = MySQLContainerProvider.getInstance();

    @Before
    public void setup() {
        url = "ws://localhost:" + port + "/ws";
    }

    @Test
    @Rollback(false)
    public void sendMessage_sendsMessagesToSubscribersAndPersistsMessageToDb_always() throws InterruptedException, ExecutionException, TimeoutException {
        //given
        Optional<User> user = userRepository.findById(DataSQLValues.getUser().getId());
        assertTrue(user.isPresent());

        final MyWebSocketClient client = new MyWebSocketClient(url, SUBSCRIBE_CHAT_ENDPOINT, user.get(), SEND_MESSAGE_ENDPOINT);
        final CompletableFuture<ChatMessage> completableFuture = client.subscribeForOneMessage();
        ChatMessage chatMessage = ChatMessage.builder()
                .sender(DataSQLValues.getUser().getLogin())
                .content(CONTENT)
                .type(MESSAGE_TYPE)
                .build();

        //when
        client.sendMessage(chatMessage);
        ChatMessage message = completableFuture.get(10, TimeUnit.SECONDS);
        //wait for scheduler (saving to db)
        Thread.sleep(2000);
        List<Message> messagesFromDb = messageRepository.findByAuthorIdOrderByTimestampDesc(DataSQLValues.getUser().getId());


        //then
        assertNotNull(message);
        assertTrue(messagesFromDb.size() > 1);
        //first message is persisted by data.sql
        Message messageEntity = messagesFromDb.get(0);
        assertNotNull(messageEntity);
        assertEquals(DataSQLValues.getUser().getLogin(), messageEntity.getAuthor().getLogin());
        assertEquals(CONTENT, messageEntity.getContent());
        assertEquals(MESSAGE_TYPE, messageEntity.getMessageType());
    }

    @Test
    public void sendMessage_channel() throws InterruptedException, ExecutionException, TimeoutException {
        //given
        int channelId = 1;
        int dummyChannelId = 2;

        Optional<User> user = userRepository.findByLoginIgnoreCase("login");
        assertTrue(user.isPresent());

        Optional<User> user2 = userRepository.findByLoginIgnoreCase("login2");
        assertTrue(user2.isPresent());

        Optional<User> user3 = userRepository.findByLoginIgnoreCase("login3");
        assertTrue(user3.isPresent());

        Optional<User> user4 = userRepository.findByLoginIgnoreCase("login3");
        assertTrue(user4.isPresent());

        MyWebSocketClient messageProducer = new MyWebSocketClient(url, "/topic/" + channelId, user.get(), SEND_MESSAGE_TO_CHANNEL_ENDPOINT);
        MyWebSocketClient messageReceiver = new MyWebSocketClient(url, "/topic/" + channelId, user2.get());
        MyWebSocketClient messageReceiver2 = new MyWebSocketClient(url, "/topic/" + channelId, user3.get());
        MyWebSocketClient dummyClient = new MyWebSocketClient(url, "/topic/" + dummyChannelId, user4.get());

        final CompletableFuture<ChatMessage> completableFuture = messageReceiver.subscribeForOneMessage();
        final CompletableFuture<ChatMessage> completableFuture2 = messageReceiver2.subscribeForOneMessage();
        final CompletableFuture<ChatMessage> dummyCompletableFuture = dummyClient.subscribeForOneMessage();

        ChatMessage chatMessage = ChatMessage.builder()
                .sender(DataSQLValues.getUser().getLogin())
                .content(CONTENT)
                .type(MESSAGE_TYPE)
                .to(String.valueOf(channelId))
                .build();

        //when
        messageProducer.sendMessage(chatMessage);

        //then
        ChatMessage response = completableFuture.get(10, TimeUnit.SECONDS);
        ChatMessage response2 = completableFuture2.get(10, TimeUnit.SECONDS);
        ChatMessage dummyResponse = dummyCompletableFuture.getNow(null);

        assertNotNull(response);
        assertNotNull(response2);
        assertNull(dummyResponse);
    }

    @Test
    @Rollback(false)
    public void sendMessage_toUser() throws InterruptedException, ExecutionException, TimeoutException {
        //given
        Optional<User> producerUser = userRepository.findByLoginIgnoreCase("login2");
        assertTrue(producerUser.isPresent());
        Optional<User> receiverUser = userRepository.findByLoginIgnoreCase("login");
        assertTrue(receiverUser.isPresent());

        MyWebSocketClient messageProducer = new MyWebSocketClient(url, "/user/queue/reply", producerUser.get(), SEND_MESSAGE_TO_USER_ENDPOINT);
        MyWebSocketClient messageReceiver = new MyWebSocketClient(url, "/user/queue/reply", receiverUser.get());

        final CompletableFuture<ChatMessage> completableFuture = messageReceiver.subscribeForOneMessage();

        ChatMessage chatMessageDto = ChatMessage.builder()
                .sender(producerUser.get().getLogin())
                .content(CONTENT)
                .type(MESSAGE_TYPE)
                .to(receiverUser.get().getLogin())
                .build();

        //when
        messageProducer.sendMessage(chatMessageDto);

        //then
        ChatMessage response = completableFuture.get(10, TimeUnit.SECONDS);

        assertNotNull(response);
    }
}