package com.unifor.br.distrischool.turma_service;

import com.unifor.br.distrischool.turma_service.controller.TurmaController;
import com.unifor.br.distrischool.turma_service.model.Turma;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@org.springframework.security.test.context.support.WithMockUser(roles="ADMIN")
@SpringBootTest
class TurmaServiceApplicationTests {

	@Autowired
	private TurmaController turmaController;

	@Test
	void contextLoads() {
		assertNotNull(turmaController);
	}

	@Test
	void testDynamicCodeGenerationAndTeacherConflict() {
		// 1. Cadastrar turma de Computacao Distribuida sem codigo
		Turma t1 = new Turma(null, "Computacao Distribuida", "2026.1", "Seg / Qua 07:30 - 09:10 (Manhã A)", 40, 10L);
		ResponseEntity<?> response1 = turmaController.cadastrar(t1);
		assertEquals(200, response1.getStatusCode().value());
		Turma saved1 = (Turma) response1.getBody();
		assertNotNull(saved1);
		assertEquals("COM-01", saved1.getCodigo());

		// 2. Tentar cadastrar outra turma para o MESMO professor (10L) no MESMO horario e MESMO dia (Seg / Qua 07:30 - 09:10) -> Deve dar conflito (400 Bad Request)
		Turma t2 = new Turma(null, "Sistemas Operacionais", "2026.1", "Seg / Qua 07:30 - 09:10 (Manhã A)", 30, 10L);
		ResponseEntity<?> response2 = turmaController.cadastrar(t2);
		assertEquals(400, response2.getStatusCode().value());

		// 3. Tentar cadastrar outra turma para o MESMO professor (10L) no MESMO horario e MESMO dia (Seg / Qua 07:30 - 09:10) -> Deve dar conflito (400 Bad Request)
		Turma t3 = new Turma(null, "Redes de Computadores", "2026.1", "Seg / Qua 07:30 - 09:10 (Manhã A)", 30, 10L);
		ResponseEntity<?> response3 = turmaController.cadastrar(t3);
		assertEquals(400, response3.getStatusCode().value());

		// 4. Cadastrar outra turma para o MESMO professor (10L) no MESMO horario MAS EM DIAS DIFERENTES (Ter / Qui 07:30 - 09:10) -> Deve funcionar
		Turma t4 = new Turma(null, "Banco de Dados", "2026.1", "Ter / Qui 07:30 - 09:10 (Manhã A)", 30, 10L);
		ResponseEntity<?> response4 = turmaController.cadastrar(t4);
		assertEquals(200, response4.getStatusCode().value());
		Turma saved4 = (Turma) response4.getBody();
		assertNotNull(saved4);
		assertEquals("BAN-01", saved4.getCodigo());
	}

}
