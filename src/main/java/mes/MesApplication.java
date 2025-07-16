package mes;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class MesApplication {
	public static void main(String[] args) {

		// .env 파일 로드
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		// 시스템 속성으로 설정
		String apiKey = dotenv.get("OPENAI_API_KEY");
		if (apiKey != null) {
			System.setProperty("OPENAI_API_KEY", apiKey);
		}

		SpringApplication.run(MesApplication.class, args);
	}
}
