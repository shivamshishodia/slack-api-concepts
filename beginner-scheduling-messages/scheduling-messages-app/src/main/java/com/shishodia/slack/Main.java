package com.shishodia.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jetty.SlackAppServer;

public class Main {

	public static void main(String[] args) throws Exception {
		System.out.println("Hello world!");

        var config = new AppConfig();
        config.setSingleTeamBotToken(System.getenv("SLACK_BOT_TOKEN"));
        config.setSigningSecret(System.getenv("SLACK_SIGNING_SECRET"));
        var app = new App(config);

        // app.message("hello", (req, ctx) -> {
        //     var logger = ctx.logger;
        //     try {
        //         var event = req.getEvent();
        //         // Call the chat.postMessage method using the built-in WebClient
        //         var result = ctx.client().chatPostMessage(r -> r
        //             // The token you used to initialize your app is stored in the `context` object
        //             .token(ctx.getBotToken())
        //             // Payload message should be posted in the channel where original message was heard
        //             .channel(event.getChannel())
        //             .text("world")
        //         );
        //         logger.info("result: {}", result);
        //     } catch (IOException | SlackApiException e) {
        //         logger.error("error: {}", e.getMessage(), e);
        //     }
        //     return ctx.ack();
        // });

		app.command("/hello", (req, ctx) -> {
			return ctx.ack(":wave: Hello, " + req.getPayload().getUserName() + "!");
		});

        var server = new SlackAppServer(app);
        server.start();
	}

}
