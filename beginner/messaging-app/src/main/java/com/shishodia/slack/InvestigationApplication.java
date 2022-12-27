package com.shishodia.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;

import static com.slack.api.model.view.Views.*;

import java.util.ArrayList;
import java.util.List;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

public class InvestigationApplication {

    public static void main(String[] args) throws Exception {

        String botToken = System.getenv("SLACK_BOT_TOKEN");
        String appToken = System.getenv("SLACK_APP_TOKEN");

        App app = new App(AppConfig.builder().singleTeamBotToken(botToken).build());

        // App Home Page.
        List<OptionObject> options = new ArrayList<>();
        OptionObject objOne = new OptionObject();
        options.add(objOne);

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
                                    .blockId("query-input")
                                    .element(plainTextInput(pti -> pti.actionId("query-input-pti").maxLength(255)))
                                    .label(plainText(pt -> pt.text("Query").emoji(true)))),
                            // input(input -> input
                            //         .blockId("chart-input")
                            //         .label(plainText(pt -> pt.text("Chart Type").emoji(true)))
                            //         .element(staticSelect(
                            //                 ss -> ss.placeholder(plainText(pt -> pt.text("Select an item").emoji(true)))
                            //                         .options(options)))),
                            section(section -> section
                                    .blockId("start-date")
                                    .text(markdownText(mt -> mt.text("Pick a start date")))
                                    .accessory(datePicker(dp -> dp.actionId("datepicker-action")
                                            .initialDate("2022-12-25")
                                            .placeholder(plainText(pt -> pt.text("Select a date").emoji(true)))))),
                            section(section -> section
                                    .blockId("end-date")
                                    .text(markdownText(mt -> mt.text("Pick an end date")))
                                    .accessory(datePicker(dp -> dp.actionId("datepicker-action")
                                            .initialDate("2022-12-25")
                                            .placeholder(plainText(pt -> pt.text("Select a date").emoji(true)))))),
                            section(section -> section
                                    .blockId("click-btn")
                                    .text(markdownText(mt -> mt.text("Initiate an investigation")))
                                    .accessory(button(btn -> btn
                                            .text(plainText(pt -> pt.text("Create Investigation").emoji(true)))
                                            .value("click_123").actionId("click-btn"))))
                )));
            ctx.client().viewsPublish(r -> r.userId(payload.getEvent().getUser()).view(appHomeView));
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        socketModeApp.start();

    }

}
