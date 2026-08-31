package ChickenMayoDeopbab.bada.domain.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class DashboardTimeConfig {
    @Bean
    public Clock dashboardClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
