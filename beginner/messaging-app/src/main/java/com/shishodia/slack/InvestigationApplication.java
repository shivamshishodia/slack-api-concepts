package com.shishodia.slack;

import com.shishodia.slack.modals.Query;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.methods.response.views.ViewsPublishResponse;
import com.slack.api.methods.response.views.ViewsUpdateResponse;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState.Value;

import static com.slack.api.model.view.Views.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

public class InvestigationApplication {

	Map<String, Query> userData = new HashMap<String, Query>();

	private List<OptionObject> staticChartList() {
		List<OptionObject> options = new ArrayList<>();

		OptionObject objOne = new OptionObject();
		objOne.setText(plainText(pti -> pti.text("Pie")));
		objOne.setValue("pie");
		options.add(objOne);

		OptionObject objTwo = new OptionObject();
		objTwo.setText(plainText(pti -> pti.text("Histogram")));
		objTwo.setValue("histogram");
		options.add(objTwo);

		OptionObject objThree = new OptionObject();
		objThree.setText(plainText(pti -> pti.text("Data")));
		objThree.setValue("data");
		options.add(objThree);

		return options;
	}

	private Query processQueryData(Map<String, Map<String, Value>> formData) {
		Query queryData = new Query();

		for (Map.Entry<String, Map<String, Value>> ele : formData.entrySet()) {
			Map<String, Value> keyValue = ele.getValue();
			if (ele.getKey().equals("query-input")) {
				String queryInput = keyValue.get("query-input-pti").getValue();
				queryData.setQuery(queryInput == null ? StringUtils.EMPTY : queryInput);
			} else if (ele.getKey().equals("chart-input")) {
				String chartInput = keyValue.values().stream().findFirst().get().getSelectedOption().getValue();
				queryData.setChartType(chartInput == null ? StringUtils.EMPTY : chartInput);
			} else if (ele.getKey().equals("start-date")) {
				String startDate = keyValue.get("datepicker-action-start-date").getValue();
				queryData.setStartDate(startDate == null ? StringUtils.EMPTY : startDate);
			} else if (ele.getKey().equals("end-date")) {
				String endDate = keyValue.get("datepicker-action-end-date").getValue();
				queryData.setEndDate(endDate == null ? StringUtils.EMPTY : endDate);
			}
		}

		return queryData;
	}

	private boolean addQueryData(String userId, Query queryData) {
		userData.put(userId, queryData);
		return true;
	}

	private String generateMarkdownForQuery(Query query) {
		return String.format("`%s` chart generated for query `%s` or date range between `%s` and `%s`", 
			query.getChartType(), query.getQuery(), "2022-10-22", "2022-10-22");
	}

	public static void main(String[] args) throws Exception {

		InvestigationApplication obj = new InvestigationApplication();

		String botToken = System.getenv("SLACK_BOT_TOKEN");
		String appToken = System.getenv("SLACK_APP_TOKEN");

		App app = new App(AppConfig.builder().singleTeamBotToken(botToken).build());

		// Global Shortcut Flow 
		// (Global Shortcut > Modal Launch Query > Modal Show Graph). Refer https://api.slack.com/surfaces/modals/using.

		// Global shortcuts.
		app.globalShortcut("launch_query", (req, ctx) -> {
			View appHomeView = view(view -> view
					.type("modal")
					.callbackId("global_shortcut_launch_query")
					.title(viewTitle(title -> title.type("plain_text").text("Launch Query").emoji(true)))
					.submit(viewSubmit(submit -> submit.type("plain_text").text("Save Query").emoji(true)))
					.close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
					.blocks(
						asBlocks(
							section(section -> section
									.text(markdownText(mt -> mt.text("*Welcome to your personal investigation space!*")))),
							divider(),
							section(section -> section.text(markdownText(mt -> mt.text(
									"You can now create your own investigations and execute queries using this interface. For more details please refer the documentation given on <https://confluence.oci.oraclecorp.com/display/LOGAN/Investigations+-+proposed+design+in+OCI|LOGAN Investigations Documentation>.")))),
							divider(),
							input(input -> input
									.blockId("query-input")
									.element(plainTextInput(pti -> pti.actionId("query-input-pti").maxLength(255)))
									.label(plainText(pt -> pt.text("Query").emoji(true)))),
							input(input -> input
									.blockId("chart-input")
									.label(plainText(pt -> pt.text("Chart Type").emoji(true)))
									.element(staticSelect(
									ss -> ss.placeholder(plainText(pt -> pt.text("Select an item").emoji(true)))
									.options(obj.staticChartList())))),
							section(section -> section
									.blockId("start-date")
									.text(markdownText(mt -> mt.text("Pick a start date")))
									.accessory(datePicker(dp -> dp.actionId("datepicker-action-start-date")
											.initialDate("2022-12-25")
											.placeholder(plainText(pt -> pt.text("Select a date").emoji(true)))))),
							section(section -> section
									.blockId("end-date")
									.text(markdownText(mt -> mt.text("Pick an end date")))
									.accessory(datePicker(dp -> dp.actionId("datepicker-action-end-date")
											.initialDate("2022-12-25")
											.placeholder(plainText(pt -> pt.text("Select a date").emoji(true)))))),
							section(section -> section
									.blockId("global-shortcut-launch-query-btn")
									.text(markdownText(mt -> mt.text("You can initiate an investigation after launching the queries.")))
									.accessory(button(btn -> btn
											.text(plainText(pt -> pt.text("Launch Query").emoji(true)))
											.value("global-shortcut-launch-query-btn").actionId("global-shortcut-launch-query-btn"))))
					))
			);
			ViewsOpenResponse vw = ctx.client().viewsOpen(r -> r
					.triggerId(ctx.getTriggerId())
					.view(appHomeView));
			return ctx.ack();
		});

		// Modal Show Graph.
		app.blockAction("global-shortcut-launch-query-btn", (req, ctx) -> {
			Query inputData = obj.processQueryData(req.getPayload().getView().getState().getValues());
			boolean addStatus = obj.addQueryData(req.getPayload().getUser().getId(), inputData);

			View appHomeView = view(view -> view
					.type("modal")
					.callbackId("global_shortcut_launch_query")
					.title(viewTitle(title -> title.type("plain_text").text("Query Results").emoji(true)))
					.submit(viewSubmit(submit -> submit.type("plain_text").text("Save Query").emoji(true)))
					.close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
					.blocks(
						asBlocks(
							section(section -> section
									.text(markdownText(mt -> mt.text(obj.generateMarkdownForQuery(inputData))))),
							com.slack.api.model.block.Blocks.image(im -> im
									.imageUrl("https://images.edrawsoft.com/articles/create-pie-chart/blank-pie-chart.png")
									.altText("appToken"))
					))
			);
			ViewsUpdateResponse vw = ctx.client().viewsUpdate(r -> r
					.viewId(req.getPayload().getView().getId())
					.view(appHomeView));
			return ctx.ack();
		});

		app.viewSubmission("global_shortcut_launch_query", (req, ctx) -> {
			// Sent inputs: req.getPayload().getView().getState().getValues()
			return ctx.ack();
		});

		app.blockAction("datepicker-action-start-date", (req, ctx) -> {
			// Do something where
			return ctx.ack();
		});

		app.blockAction("datepicker-action-end-date", (req, ctx) -> {
			// Do something where
			return ctx.ack();
		});

		// App Message Tab Flow
		// (App Message > Slash Command > Show Graph > Message Shortcut)
		app.command("/query", (req, ctx) -> {
			Logger logger = ctx.logger;
			try {
				ChatPostMessageResponse result = ctx.client().chatPostMessage(r -> r
						.channel(req.getContext().getChannelId())
						.blocks(asBlocks(
							section(section -> section
									.text(markdownText(mt -> mt.text("*Query and Investigation Interface!*")))),
							divider(),
							section(section -> section.text(markdownText(mt -> mt.text(
									"You can execute your own queries and create own investigations using this interface. For more details please refer the documentation given on <https://confluence.oci.oraclecorp.com/display/LOGAN/Investigations+-+proposed+design+in+OCI|LOGAN Investigations Documentation>.")))),
							divider(),
							input(input -> input
									.blockId("query-input")
									.element(plainTextInput(pti -> pti.actionId("query-input-pti").maxLength(255)))
									.label(plainText(pt -> pt.text("Query").emoji(true)))),
							input(input -> input
									.blockId("chart-input")
									.label(plainText(pt -> pt.text("Chart Type").emoji(true)))
									.element(staticSelect(
									ss -> ss.placeholder(plainText(pt -> pt.text("Select an item").emoji(true)))
									.options(obj.staticChartList())))),
							section(section -> section
									.blockId("start-date")
									.text(markdownText(mt -> mt.text("Pick a start date")))
									.accessory(datePicker(dp -> dp.actionId("datepicker-action-start-date")
											.initialDate("2022-12-25")
											.placeholder(plainText(pt -> pt.text("Select a date").emoji(true)))))),
							section(section -> section
									.blockId("end-date")
									.text(markdownText(mt -> mt.text("Pick an end date")))
									.accessory(datePicker(dp -> dp.actionId("datepicker-action-end-date")
											.initialDate("2022-12-25")
											.placeholder(plainText(pt -> pt.text("Select a date").emoji(true)))))),
							section(section -> section
									.blockId("slash-command-launch-query-btn")
									.text(markdownText(mt -> mt.text("You can initiate an investigation after launching the queries.")))
									.accessory(button(btn -> btn
											.text(plainText(pt -> pt.text("Launch Query").emoji(true)))
											.value("slash-command-launch-query-btn").actionId("slash-command-launch-query-btn"))))
					)));
				logger.info("result: {}", result);
			} catch (IOException | SlackApiException e) {
				logger.error("error: {}", e.getMessage(), e);
			}
			return ctx.ack();
		});

		// Modal Show Graph.
		app.blockAction("slash-command-launch-query-btn", (req, ctx) -> {
			Query inputData = obj.processQueryData(req.getPayload().getState().getValues());
			boolean addStatus = obj.addQueryData(req.getPayload().getUser().getId(), inputData);

            var logger = ctx.logger;
            try {
                var payload = req.getPayload();
				String chartType;
				switch (inputData.getChartType()) {
					case "data":
					chartType = "chart-" + inputData.getChartType() + ".png";
					break;
					case "histogram":
					chartType = "chart-" + inputData.getChartType() + ".jpeg";
					break;
					case "pie":
					chartType = "chart-" + inputData.getChartType() + ".png";
					break;
					default:
					chartType = "chart-data.png";
					break;
				}
                // The name of the file you're going to upload
				String userDirectory = Paths.get("").toAbsolutePath().toString();
                var filepath = userDirectory + "/beginner/messaging-app/src/main/java/com/shishodia/slack/resources/" + chartType;
                // Call the files.upload method using the built-in WebClient
                var result = ctx.client().filesUpload(r -> r
                    // The token you used to initialize your app is stored in the `context` object
                    // .token(ctx.getBotToken())
                    .channels(Arrays.asList(payload.getChannel().getId()))
                    .initialComment(obj.generateMarkdownForQuery(inputData))
                    // Include your filename in a ReadStream here
					.filename(UUID.randomUUID().toString())
                    .file(new File(filepath))
                );
                // Print result
                logger.info("result: {}", result);
            } catch (IOException | SlackApiException e) {
                logger.error("error: {}", e.getMessage(), e);
            }
            // Acknowledge incoming command event
            return ctx.ack();
		});

		// Message shortcuts.
		app.messageShortcut("open_investigation", (req, ctx) -> {
			String queryContext = req.getPayload().getMessage().getText();
			View appHomeView = view(view -> view
					.type("modal")
					.callbackId("open_investigation_launch_query")
					.title(viewTitle(title -> title.type("plain_text").text("Create").emoji(true)))
					.submit(viewSubmit(submit -> submit.type("plain_text").text("Create Investigation").emoji(true)))
					.close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
					.blocks(
						asBlocks(
							section(section -> section
									.text(markdownText(mt -> mt.text(queryContext))))
					))
			);
			ViewsOpenResponse vw = ctx.client().viewsOpen(r -> r
				.triggerId(ctx.getTriggerId())
				.view(appHomeView));
			return ctx.ack();
		});

		app.viewSubmission("open_investigation_launch_query", (req, ctx) -> {
			// Sent inputs: req.getPayload().getView().getState().getValues()
			return ctx.ack();
		});

		// App Home Page Flow.
		// (App Home > Show Graph Modal > Create Investigation)
		app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
			View appHomeView = view(view -> view
					.type("home")
					.blocks(asBlocks(
							section(section -> section
									.text(markdownText(mt -> mt.text("*Hi Shivam, Welcome to your personal investigation space!*")))),
							divider(),
							section(section -> section.text(markdownText(mt -> mt.text(
									"You can now create your own investigations and execute queries from this home page. For more details please refer the documentation given on <https://confluence.oci.oraclecorp.com/display/LOGAN/Investigations+-+proposed+design+in+OCI|LOGAN Investigations Documentation>.")))),
							divider(),
							input(input -> input
									.blockId("query-input")
									.element(plainTextInput(pti -> pti.actionId("query-input-pti").maxLength(255)))
									.label(plainText(pt -> pt.text("Query").emoji(true)))),
							input(input -> input
									.blockId("chart-input")
									.label(plainText(pt -> pt.text("Chart Type").emoji(true)))
									.element(staticSelect(
									ss -> ss.placeholder(plainText(pt -> pt.text("Select an item").emoji(true)))
									.options(obj.staticChartList())))),
							section(section -> section
									.blockId("start-date")
									.text(markdownText(mt -> mt.text("Pick a start date")))
									.accessory(datePicker(dp -> dp.actionId("datepicker-action-start-date")
											.initialDate("2022-12-25")
											.placeholder(plainText(pt -> pt.text("Select a date").emoji(true)))))),
							section(section -> section
									.blockId("end-date")
									.text(markdownText(mt -> mt.text("Pick an end date")))
									.accessory(datePicker(dp -> dp.actionId("datepicker-action-end-date")
											.initialDate("2022-12-25")
											.placeholder(plainText(pt -> pt.text("Select a date").emoji(true)))))),
							section(section -> section
									.blockId("app-home-launch-query-btn")
									.text(markdownText(mt -> mt.text("You can initiate an investigation after launching the queries.")))
									.accessory(button(btn -> btn
											.text(plainText(pt -> pt.text("Launch Query").emoji(true)))
											.value("app-home-launch-query-btn").actionId("app-home-launch-query-btn")))),
							divider(),
							section(section -> section
							.text(markdownText(mt -> mt.text("*Latest Queries*")))),
							divider(),
							section(section -> section.text(markdownText(mt -> mt.text(
								"`pie` chart generated for query `select * from logs` or date range between `2022-10-22` and `2022-10-22`")))),
							section(section -> section.text(markdownText(mt -> mt.text(
								"`histogram` chart generated for query `select * from error_logs` or date range between `2022-09-22` and `2022-09-30`")))),
							section(section -> section.text(markdownText(mt -> mt.text(
								"`data` chart generated for query `select * from logs | asc` or date range between `2022-09-22` and `2022-10-22`"))))
			)));
			ViewsPublishResponse vw = ctx.client().viewsPublish(r -> r.userId(payload.getEvent().getUser()).view(appHomeView));
			return ctx.ack();
		});
		
		app.blockAction("app-home-launch-query-btn", (req, ctx) -> {
			Query inputData = obj.processQueryData(req.getPayload().getView().getState().getValues());
			boolean addStatus = obj.addQueryData(req.getPayload().getUser().getId(), inputData);

			View appHomeView = view(view -> view
					.type("modal")
					.callbackId("app_home_launch_query")
					.title(viewTitle(title -> title.type("plain_text").text("Query Results").emoji(true)))
					.submit(viewSubmit(submit -> submit.type("plain_text").text("Save Query").emoji(true)))
					.close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
					.blocks(
						asBlocks(
							section(section -> section
									.text(markdownText(mt -> mt.text(obj.generateMarkdownForQuery(inputData))))),
							com.slack.api.model.block.Blocks.image(im -> im
									.imageUrl("https://images.edrawsoft.com/articles/create-pie-chart/blank-pie-chart.png")
									.altText("appToken")),
							section(section -> section
									.blockId("app-home-investigate-btn")
									.text(markdownText(mt -> mt.text("You can now initiate an investigation.")))
									.accessory(button(btn -> btn
											.text(plainText(pt -> pt.text("Initiate Investigation").emoji(true)))
											.value("app-home-investigate-btn").actionId("app-home-investigate-btn"))))
					))
			);
			ViewsOpenResponse vw = ctx.client().viewsOpen(r -> r
				.triggerId(ctx.getTriggerId())
				.view(appHomeView));
			return ctx.ack();
		});

		app.viewSubmission("app_home_launch_query", (req, ctx) -> {
			// Sent inputs: req.getPayload().getView().getState().getValues()
			return ctx.ack();
		});

		// Command Enablement.
		app.command("/chart", (req, ctx) -> {
            var logger = ctx.logger;
            try {
                var payload = req.getPayload();
				String chartType;
				switch (payload.getText()) {
					case "data":
					chartType = "chart-" + payload.getText() + ".png";
					break;
					case "histogram":
					chartType = "chart-" + payload.getText() + ".jpeg";
					break;
					case "pie":
					chartType = "chart-" + payload.getText() + ".png";
					break;
					default:
					chartType = "chart-data.png";
					break;
				}
                // The name of the file you're going to upload
				String userDirectory = Paths.get("").toAbsolutePath().toString();
                var filepath = userDirectory + "/beginner/messaging-app/src/main/java/com/shishodia/slack/resources/" + chartType;
                // Call the files.upload method using the built-in WebClient
                var result = ctx.client().filesUpload(r -> r
                    // The token you used to initialize your app is stored in the `context` object
                    // .token(ctx.getBotToken())
                    .channels(Arrays.asList(payload.getChannelId()))
                    .initialComment("Here's my file :smile:")
                    // Include your filename in a ReadStream here
					.filename(UUID.randomUUID().toString())
                    .file(new File(filepath))
                );
                // Print result
                logger.info("result: {}", result);
            } catch (IOException | SlackApiException e) {
                logger.error("error: {}", e.getMessage(), e);
            }
            // Acknowledge incoming command event
            return ctx.ack();
        });

		SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
		socketModeApp.start();

	}

}
