javac src/Server.java -cp $VERTX_HOME/lib/jars/vert.x-core.jar:$VERTX_HOME/lib/jars/vert.x-platform.jar:.:/Users/dmytro.yashkir/java_crap/apns-0.1.5-jar-with-dependencies.jar -d destination

vertx run Server -cp ~/java_crap/apns-0.1.5-jar-with-dependencies.jar:.:destination
