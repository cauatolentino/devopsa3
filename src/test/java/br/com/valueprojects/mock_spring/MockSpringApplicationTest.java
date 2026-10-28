package br.com.valueprojects.mock_spring;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class MockSpringApplicationTest {

	@Test
	public void mainDeveIniciarAAplicacao() {
		// sem servidor web, a aplicacao sobe e o metodo retorna
		assertDoesNotThrow(() -> Mock_SpringApplication.main(new String[] { "--spring.main.web-application-type=none" }));
	}
}
