<%_
let idProtoWrappedType;
if (databaseType === 'sql') {
    idProtoWrappedType = 'Int64Value';
} else {
    idProtoWrappedType = 'StringValue';
}
_%>
package <%= packageName %>.grpc;

import <%= packageName %>.<%=mainClass%>;
import <%= packageName %>.domain.PersistentAuditEvent;
import <%= packageName %>.repository.PersistenceAuditEventRepository;
import <%= packageName %>.service.AuditEventService;

import com.google.protobuf.<%=idProtoWrappedType%>;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

@SpringBootTest(classes = <%= mainClass %>.class)<% if (databaseType == 'sql') { %>
@Transactional<% } %>
public class AuditGrpcServiceIntTest {

    private static final String SAMPLE_PRINCIPAL = "SAMPLE_PRINCIPAL";
    private static final String SAMPLE_TYPE = "SAMPLE_TYPE";
    private static final Instant SAMPLE_TIMESTAMP = Instant.parse("2015-08-04T10:11:30Z");

    @Autowired
    private PersistenceAuditEventRepository auditEventRepository;

    @Autowired
    private AuditEventService auditEventService;

    private PersistentAuditEvent auditEvent;

    private Server mockServer;

    private AuditServiceGrpc.AuditServiceBlockingStub stub;

    @BeforeEach
    public void setUp() throws IOException {
        AuditGrpcService service = new AuditGrpcService(auditEventService);
        String uniqueServerName = "Mock server for " + AuditGrpcService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = AuditServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockServer.shutdownNow();
        mockServer.awaitTermination(10, TimeUnit.SECONDS);
    }

    @BeforeEach
    public void initTest() {
        auditEventRepository.deleteAll();
        auditEvent = new PersistentAuditEvent();
        auditEvent.setAuditEventType(SAMPLE_TYPE);
        auditEvent.setPrincipal(SAMPLE_PRINCIPAL);
        auditEvent.setAuditEventDate(SAMPLE_TIMESTAMP);
    }

    @Test
    public void getAllAudits() throws Exception {
        // Initialize the database
        auditEventRepository.save(auditEvent);

        assertThat(stub.getAuditEvents(AuditRequest.newBuilder().build())).extracting("principal").contains(SAMPLE_PRINCIPAL);
    }

    @Test
    public void getAudit() throws Exception {
        // Initialize the database
        auditEventRepository.save(auditEvent);

        // Get the audit
        AuditEvent event = stub.getAuditEvent(<%=idProtoWrappedType%>.newBuilder().setValue(auditEvent.getId()).build());
        assertThat(event.getPrincipal()).isEqualTo(SAMPLE_PRINCIPAL);
    }

    @Test
    public void getAuditsByDate() throws Exception {
        // Initialize the database
        auditEventRepository.save(auditEvent);

        AuditRequest request = AuditRequest.newBuilder()
            .setFromDate(Date.newBuilder().setYear(2015).setMonth(8).setDay(1))
            .setToDate(Date.newBuilder().setYear(2015).setMonth(8).setDay(20))
            .build();
        assertThat(stub.getAuditEvents(request)).extracting("principal").contains(SAMPLE_PRINCIPAL);
    }

    @Test
    public void getNonExistingAuditsByDate() throws Exception {
        // Initialize the database
        auditEventRepository.save(auditEvent);

        // Query audits but expect no results
        AuditRequest request = AuditRequest.newBuilder()
            .setFromDate(Date.newBuilder().setYear(2015).setMonth(9).setDay(1))
            .setToDate(Date.newBuilder().setYear(2015).setMonth(9).setDay(20))
            .build();
        assertThat(stub.getAuditEvents(request)).isEmpty();
    }

    @Test
    public void getNonExistingAudit() throws Exception {
        // Get the audit
        try {
            stub.getAuditEvent(<%=idProtoWrappedType%>.newBuilder().setValue(<% if (databaseType == 'sql') { %>Long.MAX_VALUE<% } else { %>"invalid id"<% } %>).build());
            failBecauseExceptionWasNotThrown(StatusException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }
}
