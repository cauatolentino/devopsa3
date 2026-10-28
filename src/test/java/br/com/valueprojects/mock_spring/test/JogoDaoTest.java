package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Participante;
import infra.JogoDao;

// o banco real e substituido por mocks do JDBC injetados pelo construtor
public class JogoDaoTest {

	private Connection conexao;
	private PreparedStatement ps;
	private PreparedStatement ps2;
	private ResultSet rs;
	private ResultSet rs2;
	private JogoDao dao;

	@BeforeEach
	public void setUp() {
		conexao = mock(Connection.class);
		ps = mock(PreparedStatement.class);
		ps2 = mock(PreparedStatement.class);
		rs = mock(ResultSet.class);
		rs2 = mock(ResultSet.class);
		dao = new JogoDao(conexao);
	}

	@Test
	public void construtorPadraoDeveFalharSemBancoDisponivel() {
		// sem driver/servidor MySQL a conexao falha e vira RuntimeException
		assertThrows(RuntimeException.class, JogoDao::new);
	}

	@Test
	public void construtorPadraoDeveAbrirConexaoPeloDriverManager() throws SQLException {
		// mockStatic troca o DriverManager so dentro deste bloco
		try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
			driver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString())).thenReturn(conexao);
			when(conexao.prepareStatement(anyString())).thenReturn(ps);

			new JogoDao().atualiza(new Jogo("Cata moedas"));

			driver.verify(() -> DriverManager.getConnection("jdbc:mysql://localhost/mocks", "root", ""));
			verify(ps).execute();
		}
	}

	@Test
	public void deveSalvarJogoComResultadosEGuardarIdGerado() throws SQLException {
		Jogo jogo = new CriadorDeJogo().para("Cata moedas")
				.resultado(new Participante(1, "Joao"), 150.0)
				.resultado(new Participante(2, "Maria"), 200.0)
				.constroi();

		when(conexao.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
		when(conexao.prepareStatement(contains("RESULTADOS"))).thenReturn(ps2);
		when(ps.getGeneratedKeys()).thenReturn(rs);
		when(rs.next()).thenReturn(true);
		when(rs.getInt(1)).thenReturn(10);

		dao.salva(jogo);

		assertEquals(10, jogo.getId());
		verify(ps).setString(1, "Cata moedas");
		verify(ps).execute();
		verify(ps2, times(2)).execute();
		verify(ps2).setInt(2, 1);
		verify(ps2).setDouble(3, 200.0);
		verify(ps).close();
	}

	@Test
	public void deveSalvarJogoSemIdGeradoESemResultados() throws SQLException {
		Jogo jogo = new CriadorDeJogo().para("Cata moedas").constroi();

		when(conexao.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
		when(ps.getGeneratedKeys()).thenReturn(rs);
		when(rs.next()).thenReturn(false);

		dao.salva(jogo);

		assertEquals(0, jogo.getId());
		verify(conexao, never()).prepareStatement(contains("RESULTADOS"));
	}

	@Test
	public void salvaDeveConverterSQLExceptionEmRuntimeException() throws SQLException {
		Jogo jogo = new CriadorDeJogo().para("Cata moedas").constroi();
		when(conexao.prepareStatement(anyString(), anyInt())).thenThrow(new SQLException("erro"));

		assertThrows(RuntimeException.class, () -> dao.salva(jogo));
	}

	@Test
	public void deveListarJogosFinalizadosComResultados() throws SQLException {
		when(conexao.prepareStatement(contains("FROM JOGO"))).thenReturn(ps);
		when(conexao.prepareStatement(contains("FROM RESULTADOS"))).thenReturn(ps2);
		when(ps.executeQuery()).thenReturn(rs);
		when(ps2.executeQuery()).thenReturn(rs2);

		when(rs.next()).thenReturn(true, false);
		when(rs.getString("descricao")).thenReturn("Cata moedas");
		when(rs.getDate("data")).thenReturn(new Date(System.currentTimeMillis()));
		when(rs.getInt("id")).thenReturn(7);
		when(rs.getBoolean("finalizado")).thenReturn(true);

		when(rs2.next()).thenReturn(true, false);
		when(rs2.getInt("participante_id")).thenReturn(1);
		when(rs2.getString("nome")).thenReturn("Joao");
		when(rs2.getDouble("valor")).thenReturn(150.0);

		List<Jogo> jogos = dao.finalizados();

		assertEquals(1, jogos.size());
		Jogo jogo = jogos.get(0);
		assertEquals(7, jogo.getId());
		assertTrue(jogo.isFinalizado());
		assertEquals(1, jogo.getResultados().size());
		assertEquals(new Participante(1, "Joao"), jogo.getResultados().get(0).getParticipante());
		verify(conexao).prepareStatement(contains("FINALIZADO = true"));
		verify(rs2).close();
		verify(rs).close();
	}

	@Test
	public void deveListarJogosEmAndamento() throws SQLException {
		when(conexao.prepareStatement(contains("FROM JOGO"))).thenReturn(ps);
		when(conexao.prepareStatement(contains("FROM RESULTADOS"))).thenReturn(ps2);
		when(ps.executeQuery()).thenReturn(rs);
		when(ps2.executeQuery()).thenReturn(rs2);

		when(rs.next()).thenReturn(true, false);
		when(rs.getString("descricao")).thenReturn("Derruba barreiras");
		when(rs.getDate("data")).thenReturn(new Date(System.currentTimeMillis()));
		when(rs.getBoolean("finalizado")).thenReturn(false);
		when(rs2.next()).thenReturn(false);

		List<Jogo> jogos = dao.emAndamento();

		assertEquals(1, jogos.size());
		assertFalse(jogos.get(0).isFinalizado());
		verify(conexao).prepareStatement(contains("FINALIZADO = false"));
	}

	@Test
	public void listagemDeveConverterSQLExceptionEmRuntimeException() throws SQLException {
		when(conexao.prepareStatement(anyString())).thenThrow(new SQLException("erro"));

		assertThrows(RuntimeException.class, () -> dao.finalizados());
	}

	@Test
	public void deveAtualizarJogoGravandoOVencedor() throws SQLException {
		Participante vencedor = new Participante(3, "Maria");
		Jogo jogo = new CriadorDeJogo().para("Cata moedas").comId(5).constroi();
		jogo.finaliza();
		jogo.setVencedor(vencedor);
		when(conexao.prepareStatement(anyString())).thenReturn(ps);

		dao.atualiza(jogo);

		verify(conexao).prepareStatement(contains("VENCEDOR_ID=?"));
		verify(ps).setBoolean(3, true);
		verify(ps).setInt(4, 3);
		verify(ps).setInt(5, 5);
		verify(ps).execute();
	}

	@Test
	public void deveAtualizarJogoSemVencedorGravandoNulo() throws SQLException {
		Jogo jogo = new CriadorDeJogo().para("Cata moedas").comId(5).constroi();
		when(conexao.prepareStatement(anyString())).thenReturn(ps);

		dao.atualiza(jogo);

		verify(ps).setNull(4, Types.INTEGER);
		verify(ps).execute();
	}

	@Test
	public void atualizaDeveConverterSQLExceptionEmRuntimeException() throws SQLException {
		Jogo jogo = new Jogo("Cata moedas", Calendar.getInstance());
		when(conexao.prepareStatement(anyString())).thenThrow(new SQLException("erro"));

		assertThrows(RuntimeException.class, () -> dao.atualiza(jogo));
	}

	@Test
	public void metodoXDeveRetornarDez() {
		assertEquals(10, dao.x());
	}
}
