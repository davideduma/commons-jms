package co.com.bancolombia.jms.sample.domain.model;

import reactor.core.publisher.Mono;

public interface RequestGateway {
    Mono<Result> doRequest(Request request);
}
