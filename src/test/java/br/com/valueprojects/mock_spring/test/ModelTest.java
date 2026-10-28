package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Pagamento;
import br.com.valueprojects.mock_spring.model.Participante;
import br.com.valueprojects.mock_spring.model.Resultado;

public class ModelTest {

	@Test
	public void pagamentoDeveGuardarValorEData() {
		Calendar data = Calendar.getInstance();
		Pagamento pagamento = new Pagamento(100.0, data);

		assertEquals(100.0, pagamento.getValor(), 0.00001);
		assertSame(data, pagamento.getData());
	}

	@Test
	public void participantesDevemSerComparadosPorIdENome() {
		Participante joao = new Participante(1, "Joao");

		assertEquals(joao, joao);
		assertEquals(joao, new Participante(1, "Joao"));
		assertEquals("Joao", joao.getNome());
		assertNotEquals(joao, null);
		assertNotEquals(joao, "Joao");
		assertNotEquals(joao, new Participante(2, "Joao"));
		assertNotEquals(joao, new Participante(1, "Maria"));
		assertNotEquals(new Participante(1, null), joao);
		assertEquals(new Participante(1, null), new Participante(1, null));
	}

	@Test
	public void criadorDeJogoDeveMontarJogoComIdEFinalizacao() {
		CriadorDeJogo criador = new CriadorDeJogo().para("Cata moedas").comId(3);

		assertFalse(criador.isFinalizado());
		criador.finaliza();

		assertTrue(criador.isFinalizado());
		assertEquals(3, criador.getId());
		assertEquals("Cata moedas", criador.constroi().getDescricao());
	}

	@Test
	public void naoDeveAceitarMaisDeCincoResultadosDoMesmoParticipante() {
		Participante joao = new Participante(1, "Joao");
		Participante maria = new Participante(2, "Maria");
		Jogo jogo = new Jogo("Cata moedas");

		// resultados alternados: Joao e Maria, 6 vezes cada
		for (int i = 0; i < 6; i++) {
			jogo.anota(new Resultado(joao, i));
			jogo.anota(new Resultado(maria, i));
		}

		assertEquals(10, jogo.getResultados().size());
	}
}
