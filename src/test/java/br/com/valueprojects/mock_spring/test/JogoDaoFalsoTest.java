package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.valueprojects.mock_spring.model.Jogo;
import infra.JogoDaoFalso;

public class JogoDaoFalsoTest {

	@Test
	public void deveSepararJogosFinalizadosDosEmAndamento() {
		JogoDaoFalso dao = new JogoDaoFalso();

		Jogo finalizado = new Jogo("Cata moedas");
		finalizado.finaliza();
		Jogo emAndamento = new Jogo("Derruba barreiras");

		dao.salva(finalizado);
		dao.salva(emAndamento);
		dao.atualiza(finalizado);

		// a lista do DAO falso e estatica, por isso so verificamos se cada jogo esta no lugar certo
		assertTrue(dao.finalizados().contains(finalizado));
		assertTrue(!dao.finalizados().contains(emAndamento));
		assertTrue(dao.emAndamento().contains(emAndamento));
		assertTrue(!dao.emAndamento().contains(finalizado));
	}
}
