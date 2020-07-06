#!/bin/sh
# for now the usage is the server port, but later we must have an address
# these keys were provided by professor, check readme on how to generate new ones
java -cp build -Djavax.net.ssl.keyStore=keys/peer.key -Djavax.net.ssl.keyStorePassword=sdis2020 -Djavax.net.ssl.trustStore=keys/truststore -Djavax.net.ssl.trustStorePassword=sdis2020 app.Peer Peer2 127.0.0.89 8005 127.0.0.100 2000