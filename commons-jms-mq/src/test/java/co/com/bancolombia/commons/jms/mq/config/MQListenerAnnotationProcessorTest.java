package co.com.bancolombia.commons.jms.mq.config;

import co.com.bancolombia.commons.jms.api.MQBrokerUtils;
import co.com.bancolombia.commons.jms.api.MQQueueCustomizer;
import co.com.bancolombia.commons.jms.api.MQQueuesContainer;
import co.com.bancolombia.commons.jms.api.exceptions.MQHealthListener;
import co.com.bancolombia.commons.jms.mq.MQListener;
import co.com.bancolombia.commons.jms.mq.config.exceptions.MQInvalidListenerException;
import lombok.extern.java.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import reactor.core.publisher.Mono;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MQListenerAnnotationProcessorTest {
    @Mock
    private ConfigurableBeanFactory factory;
    @Mock
    private MQQueuesContainer container;
    @Mock
    private ConnectionFactory cf;
    @Mock
    private MQQueueCustomizer customizer;
    @Mock
    private MQBrokerUtils brokerUtils;
    @Mock
    private MQHealthListener healthListener;
    @InjectMocks
    private MQListenerAnnotationProcessor processor;

    @BeforeEach
    void setup() {
        processor.setBeanFactory(factory);
        doReturn(customizer).when(factory).getBean(MQQueueCustomizer.class);
        when(factory.resolveEmbeddedValue(anyString()))
                .thenAnswer((Answer<String>) invocation -> (String) invocation.getArguments()[0]);
    }

    @Test
    void shouldProcessAnnotated() {
        // Arrange
        Object bean = new MyListener();
        doReturn(new MQProperties()).when(factory).getBean(MQProperties.class);
        doReturn(container).when(factory).getBean(MQQueuesContainer.class);
        doReturn(healthListener).when(factory).getBean(MQHealthListener.class);
        doReturn(brokerUtils).when(factory).getBean(MQBrokerUtils.class);
        doReturn(cf).when(factory).getBean(ConnectionFactory.class);
        doReturn(cf).when(factory).getBean("custom", ConnectionFactory.class);
        // Act
        Object result = processor.postProcessAfterInitialization(bean, "MyListener");
        // Assert
        assertEquals(bean, result);
    }

    @Test
    void shouldProcessAnnotatedReactive() {
        // Arrange
        Object bean = new MyReactiveListener();
        MQProperties properties = new MQProperties();
        properties.setReactive(true);
        doReturn(properties).when(factory).getBean(MQProperties.class);
        doReturn(container).when(factory).getBean(MQQueuesContainer.class);
        doReturn(brokerUtils).when(factory).getBean(MQBrokerUtils.class);
        doReturn(healthListener).when(factory).getBean(MQHealthListener.class);
        doReturn(cf).when(factory).getBean(ConnectionFactory.class);
        doReturn(cf).when(factory).getBean("custom", ConnectionFactory.class);
        // Act
        Object result = processor.postProcessAfterInitialization(bean, "MyReactiveListener");
        // Assert
        assertEquals(bean, result);
    }

    @Test
    void shouldWorksWithInvalidConcurrency() {
        // Arrange
        MQProperties properties = new MQProperties();
        properties.setInputConcurrency(0);
        properties.setReactive(true);
        doReturn(properties).when(factory).getBean(MQProperties.class);
        doReturn(container).when(factory).getBean(MQQueuesContainer.class);
        doReturn(cf).when(factory).getBean(ConnectionFactory.class);
        Object bean = new MyReactiveListenerInvalidConcurrency();
        // Act
        Object result = processor.postProcessAfterInitialization(bean, "MyReactiveListenerInvalidConcurrency");
        // Assert
        assertEquals(bean, result);
    }

    @Test
    void shouldFailWithInvalidBothQueues() {
        // Arrange
        MQProperties properties = new MQProperties();
        properties.setReactive(true);
        doReturn(properties).when(factory).getBean(MQProperties.class);
        Object bean = new MyReactiveListenerInvalidBothQueues();
        // Assert
        assertThrows(MQInvalidListenerException.class, () -> {
            // Act
            processor.postProcessAfterInitialization(bean, "MyReactiveListenerInvalidBothQueues");
        });
    }

    @Test
    void shouldFailWithInvalidNoQueues() {
        // Arrange
        MQProperties properties = new MQProperties();
        properties.setReactive(true);
        doReturn(properties).when(factory).getBean(MQProperties.class);
        Object bean = new MyReactiveListenerInvalidNoQueues();
        // Assert
        assertThrows(MQInvalidListenerException.class, () -> {
            // Act
            processor.postProcessAfterInitialization(bean, "MyReactiveListenerInvalidNoQueues");
        });
    }

    @Log
    public static class MyListener {

        @MQListener(value = "QUEUE.NAME", connectionFactory = "custom")
        public void process(Message message) throws JMSException {
            log.info("message: " + message.getJMSMessageID());
        }

        @MQListener("QUEUE.NAME2")
        public void process2(Message message) throws JMSException {
            log.info("message: " + message.getJMSMessageID());
        }
    }

    public static class MyReactiveListener {

        @MQListener(value = "QUEUE.NAME", connectionFactory = "custom")
        public Mono<Void> process(Message message) {
            return Mono.empty();
        }

        @MQListener("QUEUE.NAME2")
        public Mono<Void> process2(Message message) {
            return Mono.empty();
        }
    }

    public static class MyReactiveListenerInvalidConcurrency {

        @MQListener(value = "QUEUE.NAME", concurrency = "-1")
        public Mono<Void> process(Message message) {
            return Mono.empty();
        }
    }

    public static class MyReactiveListenerInvalidBothQueues {

        @MQListener(value = "QUEUE.NAME", tempQueueAlias = "alias")
        public Mono<Void> process(Message message) {
            return Mono.empty();
        }
    }

    public static class MyReactiveListenerInvalidNoQueues {

        @MQListener
        public Mono<Void> process(Message message) {
            return Mono.empty();
        }
    }
}
