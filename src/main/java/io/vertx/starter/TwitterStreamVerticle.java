package io.vertx.starter;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import twitter4j.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TwitterStreamVerticle extends AbstractVerticle {

  Twitter twitter;

  @Override
  public void start() {

    twitter = TwitterFactory.getSingleton();

    Scheduler scheduler = RxHelper.scheduler(vertx);
    Observable<Long> timer = Observable.interval(2000, 2000, TimeUnit.MILLISECONDS, scheduler);

    timer.subscribe(res -> {
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
        System.out.println(result);
      });

  }
}
