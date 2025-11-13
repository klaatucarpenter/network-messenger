# Network Messenger – Server and Multiple Clients

## Project Assumptions

The goal of this project is to:

- Create a server application capable of handling multiple clients simultaneously
- Implement communication based on TCP sockets
- Develop a simple message exchange protocol
- Design a graphical user interface (GUI) for the client application

## Protocol
| Client Command         | Meaning                       | Server Response                                |
| ---------------------- | ----------------------------- | ---------------------------------------------- |
| `HELLO <nick>`         | connection, user registration | `WELCOME` / `ERROR ...`                        |
| `MSG <text>`           | public message                | server sends `FROM <nick> <text>` to all users |
| `PRIV <toNick> <text>` | private message               | server sends `PRIVFROM <fromNick> <text>`      |
| `USERS`                | list of online users          | `USERS <nick1,nick2,...>`                      |
| `QUIT`                 | end of session                | server closes the connection                   |

## How to run

Prerequisites:
- Java 21 (project uses Gradle toolchains to compile with Java 21)
- Use the included Gradle wrapper (`./gradlew`)

### Start the server (default entry point):
```bash
./gradlew run --args="5000"
```
Or use the convenience task:
```bash
./gradlew runServer
```

Launch the GUI client (Swing desktop app):
```bash
./gradlew runClient
```

Alternatively, from your IDE:
- Run `chat.server.ChatServer.main` to start the server (optionally pass a port argument, default 5000)
- Run `chat.app.ChatApp.main` to start the GUI client

Quick sanity checks
- Confirm it’s listening: `nc -zv localhost 5000`
- If you ever need to free the port manually: `lsof -nP -iTCP:5000 | grep LISTEN` then `kill -TERM <PID>`