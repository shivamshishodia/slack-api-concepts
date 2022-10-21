# Slack API Concepts
Beginner and intermediate concepts of Slack API.

## Beginner
- messaging-app: [Post messages on a schedule](https://api.slack.com/tutorials/tracks/scheduling-messages)
- socket-mode-app: [Responding to app mentions](https://api.slack.com/tutorials/tracks/responding-to-app-mentions)

## Local Environment Setup
Create `.vscode > launch.json` file and place the following tokens under `env` attribute of your project.
```
{
    ...
    "configurations": [
        {
            "type": "java",
            "name": "Launch SocketModeApplication",
            "request": "launch",
            "mainClass": "com.shishodia.slack.SocketModeApplication",
            "projectName": "messaging-app",
            "env": {
                "SLACK_BOT_TOKEN": "xoxb-xxxxx",
                "SLACK_APP_TOKEN": "xapp-xxxxx"
                "SLACK_SIGNING_SECRET": "f1274xxxx"
            }
        }
        ...
    ]
}
```
