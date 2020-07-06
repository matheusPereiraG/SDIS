# Generalized Backup System

#### Compile 
```bash
sh compile.sh
```
#### How to Run
> First run our Server:
```bash
java -cp build -Djavax.net.ssl.keyStore=keys/server.key -Djavax.net.ssl.keyStorePassword=sdis2020 -Djavax.net.ssl.trustStore=keys/truststore -Djavax.net.ssl.trustStorePassword=sdis2020 app.Server <server_address> <server_port>
```
> Then run as many peers as you like with:
```bash
java -cp build -Djavax.net.ssl.keyStore=keys/peer.key -Djavax.net.ssl.keyStorePassword=sdis2020 -Djavax.net.ssl.trustStore=keys/truststore -Djavax.net.ssl.trustStorePassword=sdis2020 app.Peer <peer_id> <peer_address> <peer_port> <server_address> <server_port>
```
> To test our program run the client:
```bash
java -cp build -Djavax.net.ssl.keyStore=keys/client.key -Djavax.net.ssl.keyStorePassword=sdis2020 -Djavax.net.ssl.trustStore=keys/truststore -Djavax.net.ssl.trustStorePassword=sdis2020 app.Client <server_address> <server_port>
```

Done by: \
Duarte Faria \
Luis Spinola \
Matheus Gonçalves \
Tomás Figueiredo