package com.unifor.br.distrischool.nota_service.repository;

import com.unifor.br.distrischool.nota_service.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository
        extends JpaRepository<Avaliacao, Long> {
}