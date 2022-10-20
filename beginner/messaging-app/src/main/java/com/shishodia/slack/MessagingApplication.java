package com.shishodia.slack;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.slack.api.Slack;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.event.MessageDeletedEvent;
import com.slack.api.model.event.ReactionAddedEvent;
import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.view.Views.*;
import static com.slack.api.model.block.element.BlockElements.*;

public class MessagingApplication {

    static void publishMessage(String id, String text) {
        // you can get this instance via ctx.client() in a Bolt app
        var client = Slack.getInstance().methods();
        var logger = LoggerFactory.getLogger("beginner-messages");
        try {
            // Call the chat.postMessage method using the built-in WebClient
            var result = client.chatPostMessage(r -> r
                // The token you used to initialize your app
                .token(System.getenv("SLACK_BOT_TOKEN"))
                .channel(id)
                .text(text)
            // You could also use a blocks[] array to send richer content
            );
            // Print result, which includes information about the message (like TS)
            logger.info("result {}", result);
        } catch (IOException | SlackApiException e) {
            logger.error("error: {}", e.getMessage(), e);
        }
    }

	public static void main(String[] args) throws Exception {

        var config = new AppConfig();
        config.setSingleTeamBotToken(System.getenv("SLACK_BOT_TOKEN"));
        config.setSigningSecret(System.getenv("SLACK_SIGNING_SECRET"));
        var app = new App(config);

        // Publish messages, two ways.
        // publishMessage("C046MP6T9N3", "Hello world :tada:");
        // app.client().chatPostMessage(r -> r
        //     .token(System.getenv("SLACK_BOT_TOKEN"))
        //     .channel("C046MP6T9N3")
        //     .text("Hello world :tada:")
        // );

        // App Home Page.
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            var appHomeView = view(view -> view
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
            var logger = ctx.logger;
            try {
                var event = req.getEvent();
                // Call the chat.postMessage method using the built-in WebClient
                var result = ctx.client().chatPostMessage(r -> r
                    // The token you used to initialize your app is stored in the `context` object
                    // .token(ctx.getBotToken())
                    // Payload message should be posted in the channel where original message was heard
                    .channel(event.getChannel())
                    .text("Pattern detected!")
                );
                logger.info("result: {}", result);
            } catch (IOException | SlackApiException e) {
                logger.error("error: {}", e.getMessage(), e);
            }
            return ctx.ack();
        });

        // Command Enablement.
		app.command("/hello", (req, ctx) -> {
			return ctx.ack(":wave: Hello, " + req.getPayload().getUserName() + "!");
		});

        // Schedule messages.
        app.command("/schedule", (req, ctx) -> {
            var logger = ctx.logger;
            var futureTimestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(10);
            try {
                var payload = req.getPayload();
                // Call the chat.scheduleMessage method using the built-in WebClient
                var result = ctx.client().chatScheduleMessage(r -> r
                    // The token you used to initialize your app
                    // .token(ctx.getBotToken())
                    .channel(payload.getChannelId())
                    .text(payload.getText())
                    // Time to post message, in Unix Epoch timestamp format
                    .postAt((int) futureTimestamp.toInstant().getEpochSecond())
                );
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

        var server = new SlackAppServer(app);
        server.start();
	}

}
