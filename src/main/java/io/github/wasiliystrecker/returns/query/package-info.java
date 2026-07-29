@org.springframework.modulith.ApplicationModule(
    displayName = "Return Case Query",
    allowedDependencies = {
      "intake::events",
      "inspection::events",
      "resolution::events",
      "refund::events"
    })
package io.github.wasiliystrecker.returns.query;
