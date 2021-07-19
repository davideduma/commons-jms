package co.com.bancolombia.commons.jms.internal.sender;

import co.com.bancolombia.commons.jms.api.MQDestinationProvider;
import co.com.bancolombia.commons.jms.api.MQMessageCreator;
import co.com.bancolombia.commons.jms.api.MQMessageSenderSync;
import co.com.bancolombia.commons.jms.api.MQProducerCustomizer;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MQMultiContextSenderSync implements MQMessageSenderSync {
    private final ConnectionFactory connectionFactory;
    private List<MQMessageSenderSync> adapterList;
    private final int connections;
    private final MQDestinationProvider provider;
    private final MQProducerCustomizer customizer;

    public MQMultiContextSenderSync(ConnectionFactory connectionFactory, int connections,
                                    MQDestinationProvider provider, MQProducerCustomizer customizer) {
        this.connectionFactory = connectionFactory;
        this.connections = connections;
        this.provider = provider;
        this.customizer = customizer;
        start();
    }

    public void start() {
        adapterList = IntStream.range(0, connections)
                .mapToObj(idx -> new MQContextSenderSync(connectionFactory.createContext(), provider, customizer))
                .collect(Collectors.toList());
    }

    @Override
    public String send(Destination destination, MQMessageCreator messageCreator) {
        int selectIndex = (int) (System.currentTimeMillis() % connections);
        return adapterList.get(selectIndex).send(destination, messageCreator);
    }

    @Override
    public String send(MQMessageCreator messageCreator) {
        int selectIndex = (int) (System.currentTimeMillis() % connections);
        return adapterList.get(selectIndex).send(messageCreator);
    }
}