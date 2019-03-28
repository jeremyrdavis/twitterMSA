package io.vertx.starter;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;

import java.util.List;

public class MainVerticle extends AbstractVerticle {

  WebClient webClient;

  Twitter twitter;

  JsonObject config;

  @Override
  public void start(Future<Void> startFuture) {

    ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
      .addStore(new ConfigStoreOptions().setType("file").setFormat("properties").setConfig(new JsonObject().put("path", "twitter.properties"))));


    //TODO make both of these futures part of the initialization
    retriever.getConfig(ar ->{
      if (ar.failed()) {
        startFuture.fail(ar.cause());
      }else{
        config = ar.result();
        System.out.println(config.getString("oauth.consumerKey"));
        System.out.println(config.getString("oauth.consumerSecret"));
        System.out.println(config.getString("oauth.accessToken"));
        System.out.println(config.getString("oauth.accessTokenSecret"));
      }
    });



    webClient = WebClient.create(vertx);
    twitter = TwitterFactory.getSingleton();

    Router baseRouter = Router.router(vertx);
    baseRouter.route("/").handler(this::indexHandler);

    Router apiRouter = Router.router(vertx);
    apiRouter.route("/*").handler(BodyHandler.create());
    apiRouter.get("/timeline").handler(this::timelineHandler);

    baseRouter.mountSubRouter("/api", apiRouter);

    vertx
      .createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          startFuture.complete();
        }else {
          startFuture.fail(result.cause());
        }
      });
  }

  private void anotherTimelineHandler(RoutingContext routingContext) {

    webClient.get("https://api.twitter.com/1.1/statuses/user_timeline.json")
      .addQueryParam("screen_name", "argntprgrmr")
      .addQueryParam("count", "2")
      .putHeader("oauth_consumer_key", "grj2vHBZtjBccze1Q5gtQ5PPL")
      .putHeader("oauth_token", "702198386284433409-klAcdtGnou5l3mV6w5GT00AFiavyQSn")
      .putHeader("oauth_signature_method", "HMAC-SHA1")
      .putHeader("oauth_version", "1.0")
      .putHeader("oauth_signature", "YC03zJL%2F2X9YOsNV6m8NfOwhVWU%3D")
      .send(ar -> {
        if (ar.succeeded()) {
          // Obtain response
          HttpResponse<Buffer> result = ar.result();

          System.out.println("Received response with status code" + result.statusCode());
          HttpServerResponse response = routingContext.response();
          response
            .putHeader("Content-Type", "text/html")
            .end(result.bodyAsBuffer());
        } else {
          System.out.println("Something went wrong " + ar.cause().getMessage());
          HttpServerResponse response = routingContext.response();
          response
            .putHeader("Content-Type", "text/html")
            .end(ar.cause().getMessage());
        }
      });
  }

  private void timelineHandler(RoutingContext routingContext) {

    try{
        System.out.println(twitter.getOAuthAccessToken());
        List<Status> statuses = twitter.getUserTimeline("argntprgrmr");
        System.out.println("Showing home timeline.");
        for (Status status : statuses) {
          System.out.println(status.getUser().getName() + ":" +
            status.getText());
        }
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("Content-Type", "text/html")
          .end(statuses.toString());
      }catch(Exception e){
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("Content-Type", "text/html")
          .end("Hello, Twitter!");
    }

  }

  private void indexHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response
      .putHeader("Content-Type", "text/html")
      .end("Hello, Twitter!");
  }

}
