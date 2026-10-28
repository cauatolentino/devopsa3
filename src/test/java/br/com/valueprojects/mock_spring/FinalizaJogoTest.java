package br.com.valueprojects.mock_spring;




import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.model.FinalizaJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Participante;
import infra.EnviadorDeSms;
import infra.JogoDao;




public class FinalizaJogoTest {

	private JogoDao daoFalso;
	private EnviadorDeSms smsFalso;
	private Calendar antiga;
	private Participante joao;
	private Participante maria;

	@BeforeEach
	public void setUp() {
		daoFalso = mock(JogoDao.class);
		smsFalso = mock(EnviadorDeSms.class);

		antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		joao = new Participante(1, "Joao");
		maria = new Participante(2, "Maria");
	}

	 @Test
	    public void deveFinalizarJogosDaSemanaAnterior() {

	        Jogo jogo1 = new CriadorDeJogo().para("Caça moedas")
	            .naData(antiga).constroi();
	        Jogo jogo2 = new CriadorDeJogo().para("Derruba barreiras")
	            .naData(antiga).constroi();

	        // mock no lugar de dao falso

	        List<Jogo> jogosAnteriores = Arrays.asList(jogo1, jogo2);

	        when(daoFalso.emAndamento()).thenReturn(jogosAnteriores);

	        FinalizaJogo finalizador = new FinalizaJogo(daoFalso, smsFalso);
	        finalizador.finaliza();

	        assertTrue(jogo1.isFinalizado());
	        assertTrue(jogo2.isFinalizado());
	        assertEquals(2, finalizador.getTotalFinalizados());
	    }

	 @Test
		public void deveVerificarSeMetodoAtualizaFoiInvocado() {

			Jogo jogo1 = new CriadorDeJogo().para("Cata moedas").naData(antiga).constroi();
			Jogo jogo2 = new CriadorDeJogo().para("Derruba barreiras").naData(antiga).constroi();

			// mock no lugar de dao falso

			List<Jogo> jogosAnteriores = Arrays.asList(jogo1, jogo2);

			when(daoFalso.emAndamento()).thenReturn(jogosAnteriores);

			FinalizaJogo finalizador = new FinalizaJogo(daoFalso, smsFalso);
			finalizador.finaliza();

			verify(daoFalso, times(1)).atualiza(jogo1);
		}

	// Pratica 2: regra salvar -> SMS

	 @Test
		public void deveSalvarOJogoAntesDeEnviarOSms() {

			Jogo jogo = new CriadorDeJogo().para("Cata moedas").naData(antiga)
					.resultado(joao, 150.0).resultado(maria, 200.0).constroi();

			when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo));

			new FinalizaJogo(daoFalso, smsFalso).finaliza();

			// InOrder garante a sequencia: primeiro salva no banco, depois envia o SMS
			InOrder ordem = inOrder(daoFalso, smsFalso);
			ordem.verify(daoFalso).emAndamento();
			ordem.verify(daoFalso).atualiza(jogo);
			ordem.verify(smsFalso).envia(eq(maria), anyString());
			ordem.verifyNoMoreInteractions();
		}

	 @Test
		public void deveIdentificarOVencedorEEnviarSmsParaEle() {

			Jogo jogo = new CriadorDeJogo().para("Derruba barreiras").naData(antiga)
					.resultado(joao, 300.0).resultado(maria, 200.0).constroi();

			when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo));

			new FinalizaJogo(daoFalso, smsFalso).finaliza();

			assertEquals(joao, jogo.getVencedor());
			verify(smsFalso, times(1)).envia(joao, "Parabens! Voce venceu o jogo Derruba barreiras");
			verify(smsFalso, never()).envia(eq(maria), anyString());
		}

	 @Test
		public void naoDeveEnviarSmsSeFalharAoSalvar() {

			Jogo jogo = new CriadorDeJogo().para("Cata moedas").naData(antiga)
					.resultado(joao, 150.0).resultado(maria, 200.0).constroi();

			when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo));
			doThrow(new RuntimeException("banco fora do ar")).when(daoFalso).atualiza(jogo);

			FinalizaJogo finalizador = new FinalizaJogo(daoFalso, smsFalso);

			assertThrows(RuntimeException.class, finalizador::finaliza);
			// em hipotese alguma o SMS pode sair sem o jogo salvo
			verifyNoInteractions(smsFalso);
		}

	 @Test
		public void naoDeveFinalizarNemEnviarSmsParaJogosRecentes() {

			Calendar ontem = Calendar.getInstance();
			ontem.add(Calendar.DAY_OF_MONTH, -1);

			Jogo jogo = new CriadorDeJogo().para("Cata moedas").naData(ontem)
					.resultado(joao, 150.0).resultado(maria, 200.0).constroi();

			when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo));

			FinalizaJogo finalizador = new FinalizaJogo(daoFalso, smsFalso);
			finalizador.finaliza();

			assertFalse(jogo.isFinalizado());
			assertEquals(0, finalizador.getTotalFinalizados());
			verify(daoFalso, never()).atualiza(any(Jogo.class));
			verifyNoInteractions(smsFalso);
		}

	 @Test
		public void naoDeveFazerNadaQuandoNaoHaJogosEmAndamento() {

			when(daoFalso.emAndamento()).thenReturn(Collections.emptyList());

			new FinalizaJogo(daoFalso, smsFalso).finaliza();

			verify(daoFalso, never()).atualiza(any(Jogo.class));
			verifyNoInteractions(smsFalso);
		}

	 @Test
		public void jogoSemResultadoDeveSerSalvoMasSemEnviarSms() {

			Jogo jogo = new CriadorDeJogo().para("Cata moedas").naData(antiga).constroi();

			when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo));

			new FinalizaJogo(daoFalso, smsFalso).finaliza();

			verify(daoFalso, times(1)).atualiza(jogo);
			verifyNoInteractions(smsFalso);
		}

	 @Test
		public void deveEnviarUmSmsParaCadaJogoSalvo() {

			Jogo jogo1 = new CriadorDeJogo().para("Cata moedas").naData(antiga)
					.resultado(joao, 300.0).resultado(maria, 200.0).constroi();
			Jogo jogo2 = new CriadorDeJogo().para("Derruba barreiras").naData(antiga)
					.resultado(joao, 100.0).resultado(maria, 200.0).constroi();

			when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo1, jogo2));

			new FinalizaJogo(daoFalso, smsFalso).finaliza();

			InOrder ordem = inOrder(daoFalso, smsFalso);
			ordem.verify(daoFalso).atualiza(jogo1);
			ordem.verify(smsFalso).envia(eq(joao), anyString());
			ordem.verify(daoFalso).atualiza(jogo2);
			ordem.verify(smsFalso).envia(eq(maria), anyString());
		}

	}
