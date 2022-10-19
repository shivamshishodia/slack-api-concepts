package com.shishodia.slack;

import java.io.IOException;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.SlackApiException;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.view.Views.*;
import com.slack.api.model.event.AppHomeOpenedEvent;
import static com.slack.api.model.block.element.BlockElements.*;

public class Main {

	public static void main(String[] args) throws Exception {

        var config = new AppConfig();
        config.setSingleTeamBotToken(System.getenv("SLACK_BOT_TOKEN"));
        config.setSigningSecret(System.getenv("SLACK_SIGNING_SECRET"));
        var app = new App(config);

        // App Home Page.
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            var appHomeView = view(view -> view
                .type("home")
                .blocks(asBlocks(
                    section(section -> section
                        .text(markdownText(mt -> mt.text("*Welcome to your _App's Home_* :tada:")))),
                    divider(),
                    section(section -> section.text(markdownText(mt -> mt.text(
                            "This button won't do much for now but you can set up a listener for it using the `actions()` method and passing its unique `action_id`. See an example on <https://slack.dev/java-slack-sdk/guides/interactive-components|slack.dev/java-slack-sdk>.")))),
                    actions(actions -> actions
                        .elements(asElements(
                            button(b -> b.text(plainText(pt -> pt.text("Click me!"))).value("button1")
                                .actionId("button_1"))))))));

            var res = ctx.client().viewsPublish(r -> r
                .userId(payload.getEvent().getUser())
                .view(appHomeView));

            return ctx.ack();
        });

        // Respond to patterns.
        app.message("hello", (req, ctx) -> {
            var logger = ctx.logger;
            try {
                var event = req.getEvent();
                // Call the chat.postMessage method using the built-in WebClient
                var result = ctx.client().chatPostMessage(r -> r
                    // The token you used to initialize your app is stored in the `context` object
                    .token(ctx.getBotToken())
                    // Payload message should be posted in the channel where original message was heard
                    .channel(event.getChannel())
                    .text("world")
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

        var server = new SlackAppServer(app);
        server.start();
	}

}
