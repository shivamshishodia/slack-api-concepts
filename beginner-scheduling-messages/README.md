# Scheduling Messages

## References
- [Post messages on a schedule](https://api.slack.com/tutorials/tracks/scheduling-messages).
- [Building an app with Bolt for Java](https://api.slack.com/start/building/bolt-java).
- [Getting Started with Bolt](https://slack.dev/java-slack-sdk/guides/getting-started-with-bolt).

## Setup
- [ngrok](https://ngrok.com/download)
- `ngrok http 3000` (after starting the Bolt app.)

## Details
- App Name: beginner-scheduling-messages
- Workspace: sandbox-training

## Scopes
- `channels:read`
- `chat:write`
- `chat:write:public`

## Tokens
- `SLACK_BOT_TOKEN`
- `SLACK_SIGNING_SECRET`

## Command Enablement
To enable `/hello` command given in main class. You need to follow the below steps.

- Choose your app, go to Features > Slash Commands on the left pane.
- Click Create New Command button.
```
Command: /hello
Request URL: https://{random}.ngrok.io/slack/events
Short Description: Best suited description
```
- Click Save Button.
- Go to Settings > Install App and click Reinstall App button.
