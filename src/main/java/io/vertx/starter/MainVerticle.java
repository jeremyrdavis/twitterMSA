package io.vertx.starter;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.sql.Date;
import java.time.Instant;
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
    retriever.getConfig(ar -> {
      if (ar.failed()) {
        startFuture.fail(ar.cause());
      } else {
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
    apiRouter.get("/timeline").handler(this::twitter4jTimelineHandler);
    apiRouter.get("/status").handler(this::twtitter4jStatusHandler);
    apiRouter.get("/mentions").handler(this::twitter4jMentions);
    apiRouter.get("/dm").handler(this::twitter4jDirectMessageHandler);

    baseRouter.mountSubRouter("/api", apiRouter);

    vertx
      .createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          startFuture.complete();
        } else {
          startFuture.fail(result.cause());
        }
      });
  }

  private void twitter4jDirectMessageHandler(RoutingContext routingContext) {

    vertx.executeBlocking((Future<Object> future) -> {
      try {
        twitter.sendDirectMessage(1113238476600893445L, "Hi");
        System.out.println("Sending direct message to @vertxdemo");
        future.complete(new JsonObject().put("result", "message sent"));
      } catch (Exception e) {
        future.fail(e.getMessage());
      }
    }, res -> {
      if (res.succeeded()) {
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("Content-Type", "application/json")
          .end(res.result().toString());
      } else {
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("error", res.cause().getMessage()).toBuffer());
      }
    });
  }

  private void twitter4jMentions(RoutingContext routingContext) {

    vertx.executeBlocking((Future<Object> future) -> {

      try {
        List<Status> statuses = twitter.getMentionsTimeline();
        System.out.println("Showing @jbossdemo's mentions.");
        JsonArray result = new JsonArray();
        for (Status s : statuses) {
          JsonObject mention = new JsonObject()
            .put("screen_name", s.getUser().getScreenName())
            .put("user", s.getUser().getName())
            .put("status", s.getText());
          result.add(mention);
        }
        future.complete(result);
      } catch (Exception e) {
        future.fail(e.getMessage());
      }
    }, res -> {
      if (res.succeeded()) {
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("Content-Type", "application/json")
          .end(res.result().toString());
      } else {
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("error", res.cause().getMessage()).toBuffer());
      }
    });
  }

  private void twtitter4jStatusHandler(RoutingContext routingContext) {

    StringBuilder stringBuilder = new StringBuilder()
      .append("Tweet sent from Vert.x at ")
      .append(Date.from(Instant.now()).toString());

    vertx.executeBlocking((Future<Object> future) -> {
      try {
        Status status = twitter.updateStatus(stringBuilder.toString());
        System.out.println("Successfully updated the status to [" + status.getText() + "].");
        future.complete(status.getText());
      } catch (Exception e) {
        future.fail(e.getMessage());
      }
    }, res -> {
      if (res.succeeded()) {
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("status", res.result().toString()).toBuffer());
      } else {
        HttpServerResponse response = routingContext.response();
        response
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("error", res.cause().getMessage()).toBuffer());
      }

    });
  }

  private void twitter4jTimelineHandler(RoutingContext routingContext) {

    vertx.executeBlocking((Future<Object> future) -> {
        try {

          // The factory instance is re-useable and thread safe.
          List<Status> statuses = twitter.getHomeTimeline();
          System.out.println("Showing home timeline.");
          JsonArray result = new JsonArray();
          for (Status status : statuses) {
            result.add(new JsonObject().put("status", status.getText()));
          }
          future.complete(result);
        } catch (Exception e) {
          future.fail(e.getMessage());
        }
      },
      res -> {
        if (res.succeeded()) {
          HttpServerResponse response = routingContext.response();
          response
            .putHeader("Content-Type", "application/json")
            .end(res.result().toString());
        } else {
          HttpServerResponse response = routingContext.response();
          response
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("error", res.cause().getMessage()).toBuffer());
        }
      });
  }

  private void indexHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response
      .putHeader("Content-Type", "text/html")
      .end("Hello, Twitter!");
  }

}
