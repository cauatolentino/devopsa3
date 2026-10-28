package br.com.valueprojects.mock_spring.model;

import java.util.Calendar;
import java.util.List;

import infra.EnviadorDeSms;
import infra.JogoDao;



public class FinalizaJogo {

	private int total = 0;
	private final JogoDao dao;
	private final EnviadorDeSms sms;

	public FinalizaJogo(JogoDao dao, EnviadorDeSms sms) {
		this.dao = dao;
		this.sms = sms;
	}

	public void finaliza() {
		List<Jogo> todosJogosEmAndamento = dao.emAndamento();

		for (Jogo jogo : todosJogosEmAndamento) {
			if (iniciouSemanaAnterior(jogo)) {
				jogo.finaliza();
				total++;
				identificaVencedor(jogo);

				// o SMS so e enviado depois que o jogo foi salvo; se atualiza falhar, a excecao interrompe o fluxo
				dao.atualiza(jogo);
				if (jogo.getVencedor() != null) {
					sms.envia(jogo.getVencedor(), "Parabens! Voce venceu o jogo " + jogo.getDescricao());
				}
			}
		}
	}

	private void identificaVencedor(Jogo jogo) {
		if (jogo.getResultados().isEmpty()) return;

		// um Juiz novo por jogo, pois ele guarda os maiores e menores pontos entre chamadas
		Juiz juiz = new Juiz();
		juiz.julga(jogo);
		jogo.setVencedor(juiz.getVencedor());
	}

	private boolean iniciouSemanaAnterior(Jogo jogo) {
		return diasEntre(jogo.getData(), Calendar.getInstance()) >= 7;
	}

	private int diasEntre(Calendar inicio, Calendar fim) {
		Calendar data = (Calendar) inicio.clone();
		int diasNoIntervalo = 0;
		while (data.before(fim)) {
			data.add(Calendar.DAY_OF_MONTH, 1);
			diasNoIntervalo++;
		}

		return diasNoIntervalo;
	}

	public int getTotalFinalizados() {
		return total;
	}
}
