package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.valueprojects.mock_spring.controller.JogoController;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Participante;
import br.com.valueprojects.mock_spring.model.Resultado;

// teste unitario simples do controller; o MockMvc fica para a pratica 3
public class JogoControllerTest {

	@Test
	public void deveCriarListarEJulgarJogo() {
		JogoController controller = new JogoController();

		Jogo jogo = controller.criarJogo("Cata moedas");
		jogo.anota(new Resultado(new Participante(1, "Joao"), 90.0));
		jogo.anota(new Resultado(new Participante(2, "Maria"), 120.0));

		assertEquals(1, controller.listarJogos().size());
		assertEquals("Primeiro colocado: 120.0, Último colocado: 90.0", controller.julgarJogo(0));
	}
}
