# Distributed Chat

This project is a prototype implementation of a distributed chat client to demonstrate 
the use of distributed systems algorithms.

The project consists of a server and client component, both implemented in Java 11. 

The project fulfills the following requirements:

 - Dynamic host discovery
 - Leader election based on a ring algorithm
 - Failure detection of the leader and replicas
 - Being tolerant to crash faults
 - Reliable ordered multicast by ordering chat messages based on VectorClocks and NACK based message retransmission
 
 
# Architecture overview

![Alt text](./architectural_overview.png?raw=true "Architectural overview")


| System        | Are           | 
| ------------- |-------------| 
| Client      | <ul><li>Defining a username</li><li>Listening on a multicast group </li><li>Sending and receiving chat messages</li> <li>Ordering chat messages in causal order</li></ul> |
| Server Application - Leader      |  <ul><li>Accepts and delivers chat messages from and to the clients</li><li>Manages the membership and failure detection of the replicas</li><li>Replicates it’s state to replicas, so that on a crash of the leader a replica can take over the leader role </li></ul>      | 
| Server Application - Replica |  <ul><li>Update the internal members view by the received one's from the leader</li><li>Update it’s state received from the leader</li><li>Detection of leader failures</li> <li>Initation and participation in the leader election</li></ul>     | 
 


 # Setup

Checkout the project and build with running the below command in the root directory:

`.\mvnw clean install`

Maven downloads the required dependencies and builds the server and client applications.

You can start the server application with: 

`java -jar -DIP_ADDRESS=<YOUR_IP_ADDRESS> -DTCP_SERVER_PORT=8080 -DMEMBER_ID=10 .\target\chat-server`

And the client application with: 

`java -jar -DIP_ADDRESS=<YOUR_IP_ADDRESS> .\target\chat-client`

## Example cluster setup

You can start multiple server applications. The first started server acts as the 
leader and manages the group membership initially. The servers are identified by the `MEMBER_ID`. 
With the following commands two server applications are started. Bear in mind that you have to replace
the `IP_ADDRESS` with your own ip address.

`java -jar -DIP_ADDRESS=192.168.43.220 -DTCP_SERVER_PORT=8080 -DMEMBER_ID=10 ./target/chat-server.jar`

`java -jar -DIP_ADDRESS=192.168.43.220 -DTCP_SERVER_PORT=8020 -DMEMBER_ID=20 ./target/chat-server.jar`

`java -jar -DIP_ADDRESS=192.168.43.220 -DTCP_SERVER_PORT=8010 -DMEMBER_ID=30 ./target/chat-server.jar`

The network interface of the ip address have to support multicast. 
The client and server applications are using multicast for dynamic host discovery. The server distributes 
chat message over multicast as well. 


You should see in the logs of the leader the  replication of the members view to 
the other servers like:

```
c.d.d.s.server.cluster.MemberService.handleMasterMessages - Members are: [id=20 | host=192.168.43.220:8020, id=10 | host=192.168.43.220:8080, id=30 | host=192.168.43.220:8010]
c.d.d.s.shared.network.TcpClient.send - Going to send message MemberMessage{member=[id=30 | host=192.168.43.220:8010, id=20 | host=192.168.43.220:8020, id=10 | host=192.168.43.220:8080]} to 192
c.d.d.s.shared.network.TcpClient.send - Going to send message HealthMessage{} to 192.168.43.220:8010
c.d.d.s.shared.network.TcpClient.send - Going to send message HealthMessage{} to 192.168.43.220:8020
```

The leader also starts the failure detection using periodic health message send over TCP in scheduled periodically and send 
in back ground threads.

After that you can start the client multiple times: 

`java -jar -DIP_ADDRESS=192.168.43.220 ./target/chat-client.jar`

You should now be able to send and receive chat messages. 
Shutting one server done will result in a leader election. The one with the highest MEMBER_ID will be
elected as the new leader.

# Credits

This project is a result of an exam for the Distributed Systems course at [Herman Hollerith Zentrum](https://www.hhz.de/de/home/) 
and was developed by:

Fethullah Misir (fethullah.misir@hotmail.de)

Hasan Akhuy     (hasanakhuy@msn.com)

Emre Kocyigit   (emre_k_16@hotmail.de)

 
