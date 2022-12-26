package com.shishodia.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;

import static com.slack.api.model.view.Views.*;
import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

public class InvestigationApplication {

    public static void main(String[] args) throws Exception {

        String botToken = System.getenv("SLACK_BOT_TOKEN");
        String appToken = System.getenv("SLACK_APP_TOKEN");

        App app = new App(AppConfig.builder().singleTeamBotToken(botToken).build());

        // App Home Page.
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            View appHomeView = view(view -> view
                .type("home")
                .blocks(asBlocks(
                    section(section -> section
                        .text(markdownText(mt -> mt.text("*Hi Shivam, Welcome to your personal space*")))),
                    divider(),
                    section(section -> section.text(markdownText(mt -> mt.text(
                        "You can create your own investigations and execute queries from this home page. For more details please refer <https://confluence.oci.oraclecorp.com/display/LOGAN/Investigations+-+proposed+design+in+OCI>.")))),
                    divider(),
                    input(input -> input
                        .blockId("input-block")
                        .element(plainTextInput(pti -> pti.actionId("input-block-field").maxLength(255)))
                        .label(plainText(pt -> pt.text("Input Block"))))
                    )
                )
            );
            ctx.client().viewsPublish(r -> r.userId(payload.getEvent().getUser()).view(appHomeView));
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        socketModeApp.start();

    }

}
