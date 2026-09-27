package iuh.fit.socialservice.infrastructure.external.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocialEventKafkaPublisherTest {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final SocialEventKafkaPublisher publisher = new SocialEventKafkaPublisher(kafka, new ObjectMapper(), "social-test");

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void insideATransactionTheEventWaitsForTheCommit() {
        when(kafka.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());
        TransactionSynchronizationManager.initSynchronization();
        UUID aggregate = UUID.randomUUID();

        publisher.publish("social.friend.requested.v1", UUID.randomUUID(), UUID.randomUUID(), aggregate, Map.of(), null);
        verify(kafka, never()).send(anyString(), anyString(), any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafka).send(eq("social-test"), eq(aggregate.toString()), any());
    }

    @Test
    void aRolledBackTransactionSendsNothing() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publish("social.friend.requested.v1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Map.of(), null);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(kafka, never()).send(anyString(), anyString(), any());
    }

    @Test
    void outsideATransactionTheEventGoesOutImmediately() {
        when(kafka.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        publisher.publish("social.post.liked.v1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Map.of(), "like:x:y");

        verify(kafka).send(anyString(), anyString(), any());
    }
}
