package io.github.wasiliystrecker.returns.resolution.adapter;

import io.github.wasiliystrecker.returns.resolution.application.IdentifierGenerator;
import io.github.wasiliystrecker.returns.resolution.application.ResolutionDecisionRepository;
import io.github.wasiliystrecker.returns.resolution.application.ResolutionEventPublisher;
import io.github.wasiliystrecker.returns.resolution.application.ResolveInspectionService;
import io.github.wasiliystrecker.returns.resolution.domain.ResolutionPolicy;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ResolutionConfiguration {

  @Bean
  IdentifierGenerator resolutionIdentifierGenerator() {
    return UUID::randomUUID;
  }

  @Bean
  ResolutionPolicy resolutionPolicy() {
    return new ResolutionPolicy();
  }

  @Bean
  ResolutionEventPublisher resolutionEventPublisher(ApplicationEventPublisher publisher) {
    return new SpringResolutionEventPublisher(publisher);
  }

  @Bean
  ResolveInspectionService resolveInspectionService(
      ResolutionDecisionRepository decisions,
      ResolutionEventPublisher events,
      IdentifierGenerator identifiers,
      ResolutionPolicy policy) {
    return new ResolveInspectionService(decisions, events, identifiers, policy);
  }

  @Bean
  InspectionCompletedListener inspectionCompletedListener(ResolveInspectionService service) {
    return new InspectionCompletedListener(service);
  }
}
