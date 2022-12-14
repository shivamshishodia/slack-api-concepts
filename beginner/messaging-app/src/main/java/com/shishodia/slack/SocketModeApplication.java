package com.shishodia.slack;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.chat.ChatScheduleMessageResponse;
import com.slack.api.methods.response.files.FilesUploadResponse;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageDeletedEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.ReactionAddedEvent;
import com.slack.api.model.view.View;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.view.Views.*;
import static com.slack.api.model.block.element.BlockElements.*;

public class SocketModeApplication {

    /**
     * Socket Mode allows your app to use the Events API and interactive components
     * of the platform—without exposing a public HTTP Request URL.
     */

    public static void main(String[] args) throws Exception {

        String botToken = System.getenv("SLACK_BOT_TOKEN");
        String appToken = System.getenv("SLACK_APP_TOKEN");

        App app = new App(AppConfig.builder().singleTeamBotToken(botToken).build());

        // App mentions.
        app.event(AppMentionEvent.class, (req, ctx) -> {
            ctx.say("Hi there! what can I do for you?");
            return ctx.ack();
        });

        // App Home Page.
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            View appHomeView = view(view -> view
                .type("home")
                .blocks(asBlocks(
                    section(section -> section.text(markdownText(mt -> mt.text("*Welcome to your personal space* :tada:")))),
                    divider(),
                    section(section -> section.text(markdownText(mt -> mt.text("This button won't do much for now but you can set up a listener for it using the `actions()` method and passing its unique `action_id`. See an example on <https://slack.dev/java-slack-sdk/guides/interactive-components|slack.dev/java-slack-sdk>.")))),
                    actions(actions -> actions.elements(asElements(button(b -> b.text(plainText(pt -> pt.text("Click me!"))).value("button1").actionId("button_1")))))
                    )
                )
            );
            ctx.client().viewsPublish(r -> r.userId(payload.getEvent().getUser()).view(appHomeView));
            return ctx.ack();
        });

        // If the reaction added is check mark, then reply on that message to that user.
        app.event(ReactionAddedEvent.class, (payload, ctx) -> {
            ReactionAddedEvent event = payload.getEvent();
            if (event.getReaction().equals("white_check_mark")) {
                ChatPostMessageResponse message = ctx.client().chatPostMessage(r -> r
                    .channel(event.getItem().getChannel())
                    .threadTs(event.getItem().getTs())
                    .text("<@" + event.getUser() + "> Thank you! We greatly appreciate your efforts :two_hearts:"));
                if (!message.isOk()) {
                    ctx.logger.error("chat.postMessage failed: {}", message.getError());
                }
            }
            return ctx.ack();
        });

        // Respond to patterns.
        Pattern sdk = Pattern.compile(".*[(Java SDK)|(Bolt)|(slack\\-java\\-sdk)].*", Pattern.CASE_INSENSITIVE);
        app.message(sdk, (req, ctx) -> {
            Logger logger = ctx.logger;
            try {
                MessageEvent event = req.getEvent();
                // Call the chat.postMessage method using the built-in WebClient
                ChatPostMessageResponse result = ctx.client().chatPostMessage(r -> r
                    // The token you used to initialize your app is stored in the `context` object
                    // .token(ctx.getBotToken())
                    // Payload message should be posted in the channel where original message was heard
                    .channel(event.getChannel())
                    .threadTs(event.getEventTs()) // Remove this if the response is not thread reply.
                    .text("Pattern detected!")
                );
            } catch (IOException | SlackApiException e) {
                logger.error("error: {}", e.getMessage(), e);
            }
            return ctx.ack();
        });

        // Command Enablement.
		app.command("/hello", (req, ctx) -> {
			return ctx.ack(":wave: Hello, " + req.getPayload().getUserName() + "!");
		});

        // Socket command.
        app.command("/socket", (req, ctx) -> {
            return ctx.ack(":wave: Hello! Response via socket.");
        });

        // Schedule messages.
        app.command("/schedule", (req, ctx) -> {
            Logger logger = ctx.logger;
            ZonedDateTime futureTimestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(10);
            try {
                SlashCommandPayload payload = req.getPayload();
                // Call the chat.scheduleMessage method using the built-in WebClient
                ChatScheduleMessageResponse result = ctx.client().chatScheduleMessage(r -> r
                    // The token you used to initialize your app
                    // .token(ctx.getBotToken())
                    .channel(payload.getChannelId())
                    .text(payload.getText() + " [Scheduled]")
                    // Time to post message, in Unix Epoch timestamp format
                    .postAt((int) futureTimestamp.toInstant().getEpochSecond())
                );
                // Print result
                logger.info("response url: {}", req.getResponseUrl());
            } catch (IOException | SlackApiException e) {
                logger.error("error: {}", e.getMessage(), e);
            }
            // Acknowledge incoming command event
            return ctx.ack();
        });

        // Prettify a ugly JSON string.
        String uglyJSONString = "{'totalMatchedCount':211486626,'arePartialResults':false,'columns':[{'type':'COLUMN','displayName':'Log Source','subSystem':'LOG','values':[{'displayValue':'Linux Syslog Logs','internalValue':'LinuxSyslogSource','isDeleted':false}],'isListOfValues':true,'isMultiValued':false,'isCaseSensitive':false,'isGroupable':true,'isEvaluable':true,'valueType':'STRING','internalName':'msrcid'},{'type':'COLUMN','displayName':'Time','subSystem':'LOG','values':[],'isListOfValues':false,'isMultiValued':false,'isCaseSensitive':false,'isGroupable':false,'isEvaluable':true,'valueType':'TIMESTAMP','internalName':'time'}]}";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        app.command("/prettify", (req, ctx) -> {
            Logger logger = ctx.logger;
            try {
                SlashCommandPayload payload = req.getPayload();
                JsonElement je = JsonParser.parseString(uglyJSONString);
                // Call the files.upload method using the built-in WebClient
                FilesUploadResponse result = ctx.client().filesUpload(r -> r
                        // The token you used to initialize your app is stored in the `context` object
                        // .token(ctx.getBotToken())
                        .channels(Arrays.asList(payload.getChannelId()))
                        .initialComment("Here's your pretty-printed file :smile:")
                        // Filetypes: https://api.slack.com/types/file#file_types
                        .filetype("json") 
                        .filename("Prettify File")
                        // Include your filename in a ReadStream here
                        .content(gson.toJson(je)));
                // Print result
                logger.info("result: {}", result);
            } catch (IOException | SlackApiException e) {
                logger.error("error: {}", e.getMessage(), e);
            }
            // Acknowledge incoming command event
            return ctx.ack();
        });

        // Deleted messages.
        app.event(MessageDeletedEvent.class, (payload, ctx) -> {
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        socketModeApp.start();
        
    }

}
