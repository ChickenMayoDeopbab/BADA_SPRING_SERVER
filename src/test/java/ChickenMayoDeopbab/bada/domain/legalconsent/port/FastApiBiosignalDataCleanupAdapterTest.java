package ChickenMayoDeopbab.bada.domain.legalconsent.port;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientResponseException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FastApiBiosignalDataCleanupAdapterTest {

    private final List<String> requests = new ArrayList<>();
    private HttpServer server;
    private int status = 204;
    private FastApiBiosignalDataCleanupAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath()
                    + " " + exchange.getRequestHeaders().getFirst("X-Internal-Secret"));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        adapter = new FastApiBiosignalDataCleanupAdapter();
        ReflectionTestUtils.setField(adapter, "aiBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        ReflectionTestUtils.setField(adapter, "internalSecret", "internal-secret");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void deletesTheUsersBiosignalsWithTheInternalSecret() {
        adapter.deleteByUserId(7L);

        assertThat(requests).containsExactly("DELETE /internal/v1/users/7/biosignals internal-secret");
    }

    @Test
    void failsWhenFastApiFails() {
        status = 500;

        assertThatThrownBy(() -> adapter.deleteByUserId(7L)).isInstanceOf(RestClientResponseException.class);
    }
}
