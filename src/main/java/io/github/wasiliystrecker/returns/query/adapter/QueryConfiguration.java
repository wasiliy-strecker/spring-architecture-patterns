package io.github.wasiliystrecker.returns.query.adapter;

import io.github.wasiliystrecker.returns.query.ReturnCaseQueries;
import io.github.wasiliystrecker.returns.query.application.FindReturnCaseService;
import io.github.wasiliystrecker.returns.query.application.ProjectReturnCaseService;
import io.github.wasiliystrecker.returns.query.application.ReturnCaseProjectionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class QueryConfiguration {

  @Bean
  ProjectReturnCaseService projectReturnCaseService(ReturnCaseProjectionRepository projections) {
    return new ProjectReturnCaseService(projections);
  }

  @Bean
  ReturnCaseEventListeners returnCaseEventListeners(ProjectReturnCaseService service) {
    return new ReturnCaseEventListeners(service);
  }

  @Bean
  ReturnCaseQueries returnCaseQueries(ReturnCaseProjectionRepository projections) {
    return new FindReturnCaseService(projections);
  }
}
