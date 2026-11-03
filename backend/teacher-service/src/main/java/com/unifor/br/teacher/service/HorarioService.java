package com.unifor.br.teacher.service;

import com.unifor.br.teacher.model.Horario;
import com.unifor.br.teacher.repository.HorarioRepository;
import com.unifor.br.teacher.producer.HorarioEventProducer;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final HorarioEventProducer eventProducer;

    public HorarioService(HorarioRepository horarioRepository, HorarioEventProducer eventProducer) {
        this.horarioRepository = horarioRepository;
        this.eventProducer = eventProducer;
    }

    public Horario alocarHorario(Horario novoHorario) {
        List<Horario> conflitos = horarioRepository.encontrarConflitos(
                novoHorario.getProfessorId(),
                novoHorario.getTurma().getId(),
                novoHorario.getDiaSemana(),
                novoHorario.getHoraInicio(),
                novoHorario.getHoraFim());

        if (!conflitos.isEmpty()) {
            throw new RuntimeException(
                    "Conflito detetado: O professor ou a turma já possuem uma aula agendada neste horário.");
        }

        Horario horarioSalvo = horarioRepository.save(novoHorario);

        // Emite o evento Kafka para notificar o sistema
        eventProducer.notificarHorarioAtualizado(horarioSalvo);

        return horarioSalvo;
    }
}