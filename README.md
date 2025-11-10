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
