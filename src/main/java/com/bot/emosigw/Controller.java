package com.bot.emosigw;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
public class Controller {

    @Autowired
    @Qualifier("lineMessagingClient")
    private LineMessagingClient lineMessagingClient;

    @Autowired
    @Qualifier("lineSignatureValidator")
    private LineSignatureValidator lineSignatureValidator;

    @RequestMapping(value="/webhook", method= RequestMethod.POST)
    public ResponseEntity<String> callback(
            @RequestHeader("X-Line-Signature") String xLineSignature,
            @RequestBody String eventsPayload)
    {
        try {
//            if (!lineSignatureValidator.validateSignature(eventsPayload.getBytes(), xLineSignature)) {
////                throw new RuntimeException("Invalid Signature Validation");
////            }

            // parsing event
            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            EventsModel eventsModel = objectMapper.readValue(eventsPayload, EventsModel.class);

            eventsModel.getEvents().forEach((event)->{
//                if (event instanceof MessageEvent) {
//                    MessageEvent messageEvent = (MessageEvent) event;
////                    TextMessageContent textMessageContent = (TextMessageContent) messageEvent.getMessage();
////                    replyText(messageEvent.getReplyToken(), textMessageContent.getText());
////                    replySticker(messageEvent.getReplyToken(), "1", "1");
//
////                    List<Message> msgArray = new ArrayList<>();
////                    msgArray.add(new TextMessage(textMessageContent.getText()));
////                    msgArray.add(new StickerMessage("1", "106"));
////                    ReplyMessage replyMessage = new ReplyMessage(messageEvent.getReplyToken(), msgArray);
////                    reply(replyMessage);
//                }
                if (event instanceof MessageEvent) {
                    MessageEvent messageEvent = (MessageEvent) event;
                    if  (messageEvent.getMessage() instanceof AudioMessageContent
                            || messageEvent.getMessage() instanceof ImageMessageContent
                            || messageEvent.getMessage() instanceof VideoMessageContent
                            || messageEvent.getMessage() instanceof FileMessageContent
                    ) {
                        handleContentMessage(messageEvent);
                    } else if(messageEvent.getMessage() instanceof TextMessageContent) {
                        handleTextMessage(messageEvent);
                    } else {
                        replyText(messageEvent.getReplyToken(), "Unknown Message");
                    }
                }
            });

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/content/{id}", method = RequestMethod.GET)
    public ResponseEntity content(
            @PathVariable("id") String messageId
    ){
        MessageContentResponse messageContent = getContent(messageId);

        if(messageContent != null) {
            HttpHeaders headers = new HttpHeaders();
            String[] mimeType = messageContent.getMimeType().split("/");
            headers.setContentType(new MediaType(mimeType[0], mimeType[1]));

            InputStream inputStream = messageContent.getStream();
            InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

            return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private MessageContentResponse getContent(String messageId) {
        try {
            return lineMessagingClient.getMessageContent(messageId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void reply(ReplyMessage replyMessage) {
        try {
            lineMessagingClient.replyMessage(replyMessage).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void replyText(String replyToken, String messageToUser){
        TextMessage textMessage = new TextMessage(messageToUser);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, textMessage);
        reply(replyMessage);
    }

    private void replyFlexMessage(String replyToken) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            String flexTemplate = IOUtils.toString(classLoader.getResourceAsStream("flex_message.json"));

            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            FlexContainer flexContainer = objectMapper.readValue(flexTemplate, FlexContainer.class);

            ReplyMessage replyMessage = new ReplyMessage(replyToken, new FlexMessage("Coffee Shop", flexContainer));
            reply(replyMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleContentMessage(MessageEvent event) {
        String baseURL     = "https://emosigwasli.herokuapp.com/";
        String contentURL  = baseURL+"/content/"+ event.getMessage().getId();
        String contentType = event.getMessage().getClass().getSimpleName();
        String textMsg     = contentType.substring(0, contentType.length() -14)
                + " yang kamu kirim bisa diakses dari link:\n "
                + contentURL;

        replyText(event.getReplyToken(), textMsg);
    }

    private void handleTextMessage(MessageEvent event) {
        TextMessageContent textMessageContent = (TextMessageContent) event.getMessage();

        if (textMessageContent.getText().toLowerCase().contains("flex")) {
            replyFlexMessage(event.getReplyToken());
        } else {
            replyText(event.getReplyToken(), textMessageContent.getText());
        }
    }

//    private void replySticker(String replyToken, String packageId, String stickerId){
//        StickerMessage stickerMessage = new StickerMessage(packageId, stickerId);
//        ReplyMessage replyMessage = new ReplyMessage(replyToken, stickerMessage);
//        reply(replyMessage);
//    }

}
