## Implementation
- I have made 2 folders, each for Server and Client. 
- Server directory has ServerFinal.java and Client directory has ClientFinal.java.
- The code is multithreaded. Thus, multiple users can connect to the single server.
- The code is tested over LAN, by running server on one machine and clients on multiple machines.
- I used threads for executing the processes. Client has a thread running in background to listen for any messages from server. 
- Other thread in Client sends the message based upon the client input through command line.
- A folder is created for the client on server when he/she uploads any file either through TCP or UDP.
- When a user downloads a file from any user using get_file function, the code creates a directory for client if it does't exist.
- Server and Client communicates by connecting through a socket.
- UDP and TCP are implemented for file transfer.


## Instructions to run the code

### For Server
- Go to Server directory and write ```javac ServerFinal.java```
- Then run the compiled file as ```java ServerFinal```

### For Client
- Go to Client directory and write ```javac ClientFinal.java```
- Then run the compiled file as ```java ClientFinal.java```
