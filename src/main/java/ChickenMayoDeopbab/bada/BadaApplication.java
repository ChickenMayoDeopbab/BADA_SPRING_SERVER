package ChickenMayoDeopbab.bada;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BadaApplication {

	public static void main(String[] args) {
		SpringApplication.run(BadaApplication.class, args);
	}

}
