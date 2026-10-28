package infra;

import br.com.valueprojects.mock_spring.model.Participante;



public interface EnviadorDeSms {

	void envia(Participante destinatario, String mensagem);
}
