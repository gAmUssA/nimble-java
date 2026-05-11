package io.gamov.loyalty;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.nimbleway.sdk.NimbleClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class NimbleConfig {

  @ConfigProperty(name = "nimble.api.key")
  String apiKey;

  @Produces
  @ApplicationScoped
  public NimbleClient nimbleClient() {
    return NimbleClient.builder().apiKey(apiKey).build();
  }

  void onStart(@Observes StartupEvent ev) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "NIMBLE_API_KEY is not set. Add it to .env or export it before starting the app.");
    }
  }
}
