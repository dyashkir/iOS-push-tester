import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.ApnsDelegate;
import com.notnoop.apns.DeliveryError;
import com.notnoop.apns.ApnsNotification;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.deploy.Verticle;
import org.vertx.java.core.http.RouteMatcher;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.SimpleHandler;

public class Server extends Verticle {

  JsonArray devices = new JsonArray();
  JsonArray apnsLog = new JsonArray();

  
  public void start() {
   

    //apns setup
    final ApnsService pushService = APNS.newService()
      .withCert("certs/cert.p12", "somepass")
      .withSandboxDestination()
      .withDelegate(  new ApnsDelegate(){
          public void connectionClosed(DeliveryError e, int messageId){
            System.out.println("Delivery error code: " + e.code() + " msg id: " + messageId);
            apnsLog.addObject(new JsonObject()
              .putString("outcome", "fail delivery error")
              .putNumber("code", e.code())); 
          }

          public void messageSendFailed(ApnsNotification message, Throwable e){
            apnsLog.addObject(new JsonObject()
              .putString("outcome", "fail")
              .putNumber("message_id", message.getIdentifier())
              .putString("exception", e.toString())); 
            String msg = "Send to APNS failed msg_id" + message.getIdentifier() + " " + message.getExpiry() + " " + e;
            System.out.println(msg);
          }

          public void messageSent(ApnsNotification message) {
            apnsLog.addObject(new JsonObject()
              .putString("outcome", "success")
              .putNumber("message_id", message.getIdentifier())); 
            //String msg = "Sent to APNS msg_id" + message.getIdentifier() + " " + message.getExpiry();
            //System.out.println(msg);
          }
        })
      .build();

  
    //router definition
    RouteMatcher router = new RouteMatcher();
    
    router.get("/", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        req.response.sendFile("index.html");
      }
    });
    
    router.get("/model.js", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        req.response.sendFile("model.js");
      }
    });

    router.get("/log", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        req.response.end(apnsLog.encode());
      }
    });

    //not functioning right, connection gets refused, this code is also syncronous
    router.get("/inactive", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        JsonArray ar = new JsonArray();
        Map<String, Date> inactiveDevices = pushService.getInactiveDevices();
        for (String deviceToken : inactiveDevices.keySet()) {
          Date inactiveAsOf = inactiveDevices.get(deviceToken);
          ar.addObject(new JsonObject()
            .putString("token", deviceToken)
            .putString("timestamp", inactiveAsOf.toString())
            );
        }
        req.response.end(ar.encode());
      }
    });

    router.post("/devices", new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        req.bodyHandler(new Handler<Buffer>() {
            public void handle(Buffer body) {
              // The entire body has now been received
              System.out.println("" + body.toString());
              JsonObject data = new JsonObject( body.toString());
              Object[] ar = devices.toArray();
              int i;
              for(i=0; i< ar.length; i++){
                
                System.out.println("" + ar[i]);
                if (((HashMap)(ar[i])).get("token").equals(data.getString("token"))){
                  break;
                }
              }
              if (i==ar.length){
                devices.addObject(data); 
              }
              req.response.end();
            }
        });
      }
    });

    router.get("/devices", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        
        //JsonArray res = new JsonArray();
        //JsonObject obj = new JsonObject().putString("name", "Dima iPhone 4S")
          //                       .putString("token", "ab18f221 c39b0098 17d96ed3 6277222e a2c900e7 6840908f 7eb5fa9a 280a4be6");
        //res.addObject(obj);
        System.out.println(devices.encode());
        req.response.end(devices.encode());
      }
    });

    router.post("/messages", new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
       req.bodyHandler(new Handler<Buffer>() {
            public void handle(Buffer body) {
              // The entire body has now been received

              JsonObject data = new JsonObject( body.toString());
              int badge = 0;
              if (data.getString("badge") != null){
                badge = Integer.parseInt(data.getString("badge"));
              }
              //System.out.println("The total body received was " + body.length() + " bytes " + body.toString());   
              String payload = APNS.newPayload()
                .alertBody(data.getString("messageBody"))
                .badge(badge)
                .customField("most_awesome", " secret data")
                .actionKey(data.getString("actionButton"))
                .sound("default")
                .build();
          
              String token = data.getObject("selectedDevice").getString("token");
              System.out.println("Sending: " + payload);
              pushService.push(token, payload);

              req.response.end();
            }
        });
      }
    });
    router.get("/send/JSON", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        Map param = req.params();
        System.out.println(req.params().toString());
        String payload = param.get("message").toString();
        System.out.println("Sending: " + payload);
        String token = param.get("token").toString();
        pushService.push(token, payload); 
        req.response.sendFile("submitted.html");
      }
    });


    router.get("/send", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        Map param = req.params();
        System.out.println(req.params().toString());
        int badge = 0;
        if (param.get("badge") != null){
          badge = Integer.parseInt(param.get("badge").toString());
        }

        String payload = APNS.newPayload()
          .alertBody(param.get("message").toString())
          .badge(badge)
          .customField("most_awesome", " secret data")
          .actionKey(param.get("actionKey").toString())
          .sound("default")
          .build();
          
          String token = param.get("token").toString();
          System.out.println("Sending: " + payload);
          pushService.push(token, payload);
         
          req.response.sendFile("submitted.html");
      }
    });

    router.noMatch(new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        req.response.end("Unknown resource");
      }
    });
    vertx.createHttpServer().requestHandler(router).listen(8090);
  }
}
