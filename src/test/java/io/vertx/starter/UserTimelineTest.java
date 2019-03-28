package io.vertx.starter;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class UserTimelineTest {
  private Vertx vertx;

  @Test
  @DisplayName("Test Server Starts")
  public void testThatTheUserTimelineIsNotEmpty(Vertx vertx, VertxTestContext tc) {

    WebClient webClient = WebClient.create(vertx);
    Checkpoint deploymentCheckpoint = tc.checkpoint();
    Checkpoint requestCheckpoint = tc.checkpoint();

    vertx.deployVerticle(new MainVerticle(), tc.succeeding(id -> {

      deploymentCheckpoint.flag();

      webClient.get(8080, "localhost", "/api/timeline")
        .as(BodyCodec.string())
        .send(tc.succeeding(resp -> {
          tc.verify(() -> {
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(resp.body()).isNotEmpty();
            requestCheckpoint.flag();
          });
        }));
    }));

  }
}