package co.com.bancolombia.jms.sample.drivenadapters;

import co.com.bancolombia.commons.jms.api.MQMessageSender;
import co.com.bancolombia.commons.jms.mq.EnableMQMessageSender;
import co.com.bancolombia.jms.sample.domain.exceptions.ParseMessageException;
import co.com.bancolombia.jms.sample.domain.model.Request;
import co.com.bancolombia.jms.sample.domain.model.RequestGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@EnableMQMessageSender
public class MyMQSender implements RequestGateway {
    private final MQMessageSender sender;
    private final ObjectMapper mapper;

    @Override
    public Mono<String> send(Request request) {
        return sender.send(ctx -> {
            String json;
            try {
                json = mapper.writeValueAsString(request);
            } catch (JsonProcessingException e) {
                throw new ParseMessageException(e);
            }
            return ctx.createTextMessage(json);
        });
    }
}