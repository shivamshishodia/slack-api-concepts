# Socket Mode Application

## References
- [Responding to app mentions](https://api.slack.com/tutorials/tracks/responding-to-app-mentions).
- [Getting Started with Bolt (Socket Mode)](https://slack.dev/java-slack-sdk/guides/getting-started-with-bolt-socket-mode).
- [App Hosting](https://api.slack.com/docs/hosting)
- [Event Types](https://api.slack.com/events)
- [Events API](https://api.slack.com/apis/connections/events-api)

## Setup
- pom.xml (check project object model for required dependencies)
- Set scope: Go to OAuth & Permissions > Scopes > `app_mentions:read` and `commands`. Reinstall your application.
- Set event: Go to Event Subscriptions > Subscribe to bot events > `app_mentions`. Reinstall your application.
- Generate an app-level token and set it inside environment variables.
```
Go to Basic Information > App-Level Tokens >Generate an app-level token (button)
Token Name > socket-name
Add Scope > connections:write (Route your app’s interactions and event payloads over WebSockets)
Set ENV VAR `SLACK_APP_TOKEN` as `xapp-xxxxxxxxxxxxxxxxxxxxx`
```

## Details
- App Name: messaging-app
- Workspace: sandbox-training

## Command Enablement
To enable `/socket` command given in main class. You need to follow the below steps.

- Go to Settings > Socket Mode on the left pane > Turn on Enable Socket Mode.
- Choose your app, go to Features > Slash Commands on the left pane.
- Click Create New Command button.
```
Command: /socket
Short Description: Best suited description
```
- Click Save Button.
- Go to Settings > Install App and click Reinstall App button.

## Execution
mvn exec:java
