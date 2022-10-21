package com.shishodia.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;

public class SocketModeApplication {

    /**
     * Socket Mode allows your app to use the Events API and interactive components
     * of the platform—without exposing a public HTTP Request URL.
     */

    public static void main(String[] args) throws Exception {

        String botToken = System.getenv("SLACK_BOT_TOKEN");
        String appToken = System.getenv("SLACK_APP_TOKEN");

        App app = new App(AppConfig.builder().singleTeamBotToken(botToken).build());

        // Socket command.
        app.command("/socket", (req, ctx) -> {
            return ctx.ack(":wave: Hello! Response via socket.");
        });

        // App mentions.
        app.event(AppMentionEvent.class, (req, ctx) -> {
            ctx.say("Hi there! what can I do for you?");
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        socketModeApp.start();
        
    }

}
