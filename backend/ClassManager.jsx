import React, { useState, useMemo } from 'react';

const DIAS_OPCOES = [
  'Seg / Qua',
  'Ter / Qui',
  'Qua / Sex',
  'Seg / Qua / Sex',
  'Ter / Qui / Sáb',
  'Sábado Manhã'
];

const HORARIOS_OPCOES = [
  '07:30 - 09:10 (Manhã A)',
  '09:20 - 11:00 (Manhã B)',
  '11:10 - 12:50 (Manhã C)',
  '13:30 - 15:10 (Tarde A)',
  '15:20 - 17:00 (Tarde B)',
  '18:30 - 20:10 (Noite A)',
  '20:20 - 22:00 (Noite B)'
];

const BLOCOS_OPCOES = [
  'Bloco A (Exatas & Gestão)',
  'Bloco B (Humanas & Artes)',
  'Bloco M (Tecnologia da Informação)',
  'Complexo de Laboratórios',
  'Auditório Central'
];

const SALAS_OPCOES = [
  'Sala 101', 'Sala 102', 'Sala 103', 'Sala 104',
  'Sala 201', 'Sala 202', 'Sala 203', 'Sala 204',
  'Lab 01 (Eng. Software)', 'Lab 02 (Sistemas Distribuídos)',
  'Lab 03 (Redes & Cloud)', 'Lab 04 (Hardware & IoT)'
];

export default function ClassManager({
  classes,
  teachers,
  onSaveClass,
  onDeleteClass,
  loading
}) {
  const [showModal, setShowModal] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // Filtros de busca na listagem
  const [filtroTexto, setFiltroTexto] = useState('');
  const [filtroSemestre, setFiltroSemestre] = useState('');
  const [filtroProfessor, setFiltroProfessor] = useState('');

  // Formulário com campos estruturados e validados
  const [classForm, setClassForm] = useState({
    id: null,
    codigo: '',
    nomeDisciplina: '',
    semestre: '2026.1',
    diasSemana: 'Seg / Qua',
    faixaHorario: '07:30 - 09:10 (Manhã A)',
    bloco: 'Bloco M (Tecnologia da Informação)',
    salaNumero: 'Lab 02 (Sistemas Distribuídos)',
    capacidadeMaxima: 40,
    teacherId: ''
  });

  const openNewModal = () => {
    setClassForm({
      id: null,
      codigo: '',
      nomeDisciplina: '',
      semestre: '2026.1',
      diasSemana: 'Seg / Qua',
      faixaHorario: '07:30 - 09:10 (Manhã A)',
      bloco: 'Bloco M (Tecnologia da Informação)',
      salaNumero: 'Lab 02 (Sistemas Distribuídos)',
      capacidadeMaxima: 40,
      teacherId: teachers.length > 0 ? teachers[0].id : ''
    });
    setIsEditing(false);
    setShowModal(true);
  };

  const openEditModal = (turma) => {
    // Tenta desmembrar horario e sala caso existam
    let dias = 'Seg / Qua';
    let faixa = '07:30 - 09:10 (Manhã A)';
    if (turma.horario && turma.horario.includes(' - ')) {
      const parts = turma.horario.split(' ');
      if (parts.length >= 3) {
        dias = parts.slice(0, 3).join(' ');
        faixa = parts.slice(3).join(' ') || faixa;
      }
    }

    let bloco = 'Bloco M (Tecnologia da Informação)';
    let salaNum = 'Sala 101';
    if (turma.sala && turma.sala.includes(' - ')) {
      const parts = turma.sala.split(' - ');
      bloco = parts[0] || bloco;
      salaNum = parts[1] || salaNum;
    }

    setClassForm({
      id: turma.id,
      codigo: turma.codigo,
      nomeDisciplina: turma.nomeDisciplina,
      semestre: turma.semestre,
      diasSemana: dias,
      faixaHorario: faixa,
      bloco: bloco,
      salaNumero: salaNum,
      capacidadeMaxima: turma.capacidadeMaxima || 40,
      teacherId: turma.teacherId || (turma.teacher ? turma.teacher.id : '')
    });
    setIsEditing(true);
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const horarioComposto = `${classForm.diasSemana} ${classForm.faixaHorario}`;
    const salaComposta = `${classForm.bloco} - ${classForm.salaNumero}`;

    const payload = {
      id: classForm.id,
      codigo: classForm.codigo.trim().toUpperCase(),
      nomeDisciplina: classForm.nomeDisciplina.trim(),
      semestre: classForm.semestre.trim(),
      horario: horarioComposto,
      sala: salaComposta,
      capacidadeMaxima: Number(classForm.capacidadeMaxima),
      teacherId: classForm.teacherId ? Number(classForm.teacherId) : null
    };

    const success = await onSaveClass(payload, isEditing);
    if (success) {
      setShowModal(false);
    }
  };

  // Filtragem dinâmica de turmas
  const turmasFiltradas = useMemo(() => {
    return classes.filter((turma) => {
      const matchesTexto =
        !filtroTexto ||
        turma.codigo.toLowerCase().includes(filtroTexto.toLowerCase()) ||
        turma.nomeDisciplina.toLowerCase().includes(filtroTexto.toLowerCase());

      const matchesSemestre = !filtroSemestre || turma.semestre === filtroSemestre;

      const matchesProfessor =
        !filtroProfessor ||
        (turma.teacher && String(turma.teacher.id) === String(filtroProfessor)) ||
        (turma.teacherId && String(turma.teacherId) === String(filtroProfessor));

      return matchesTexto && matchesSemestre && matchesProfessor;
    });
  }, [classes, filtroTexto, filtroSemestre, filtroProfessor]);

  const semestresUnicos = useMemo(() => {
    const s = new Set(classes.map((c) => c.semestre).filter(Boolean));
    return Array.from(s);
  }, [classes]);

  return (
    <div className="card classes-card">
      <div className="card-header">
        <div>
          <h2>Turmas & Disciplinas (class-service)</h2>
          <span style={{ fontSize: '13px', color: '#64748b' }}>
            Planejamento acadêmico com campos estruturados para horários, salas e corpo docente
          </span>
        </div>
        <button className="btn-primary" onClick={openNewModal}>
          + Criar Nova Turma
        </button>
      </div>

      {/* Barra de Filtros e Busca de Turmas */}
      <div
        style={{
          display: 'flex',
          gap: '12px',
          alignItems: 'center',
          flexWrap: 'wrap',
          marginBottom: '20px',
          background: '#f8fafc',
          padding: '16px',
          borderRadius: '8px',
          border: '1px solid #e2e8f0'
        }}
      >
        <div style={{ flex: '1 1 200px' }}>
          <input
            type="text"
            placeholder="Filtrar por código ou disciplina..."
            value={filtroTexto}
            onChange={(e) => setFiltroTexto(e.target.value)}
            style={{ width: '100%', boxSizing: 'border-box' }}
          />
        </div>

        <div style={{ minWidth: '150px' }}>
          <select
            value={filtroSemestre}
            onChange={(e) => setFiltroSemestre(e.target.value)}
            style={{ width: '100%' }}
          >
            <option value="">Todos os Semestres</option>
            {semestresUnicos.map((sem) => (
              <option key={sem} value={sem}>{sem}</option>
            ))}
          </select>
        </div>

        <div style={{ minWidth: '180px' }}>
          <select
            value={filtroProfessor}
            onChange={(e) => setFiltroProfessor(e.target.value)}
            style={{ width: '100%' }}
          >
            <option value="">Todos os Professores</option>
            {teachers.map((t) => (
              <option key={t.id} value={t.id}>{t.nome}</option>
            ))}
          </select>
        </div>

        {(filtroTexto || filtroSemestre || filtroProfessor) && (
          <button
            type="button"
            className="btn-secondary"
            onClick={() => {
              setFiltroTexto('');
              setFiltroSemestre('');
              setFiltroProfessor('');
            }}
          >
            Limpar Filtros
          </button>
        )}
      </div>

      <div className="table-responsive">
        <table className="attendance-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Código</th>
              <th>Disciplina</th>
              <th>Semestre</th>
              <th>Docente Alocado (REST)</th>
              <th>Horário (Dias / Turno)</th>
              <th>Espaço / Sala</th>
              <th>Capacidade</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {turmasFiltradas.length === 0 ? (
              <tr>
                <td colSpan="9" className="text-center">
                  Nenhuma turma encontrada com os filtros selecionados.
                </td>
              </tr>
            ) : (
              turmasFiltradas.map((turma) => (
                <tr key={turma.id}>
                  <td>{turma.id}</td>
                  <td><code>{turma.codigo}</code></td>
                  <td className="student-name">{turma.nomeDisciplina}</td>
                  <td>
                    <span style={{
                      backgroundColor: '#f1f5f9',
                      padding: '4px 8px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: '600'
                    }}>
                      {turma.semestre}
                    </span>
                  </td>
                  <td>
                    {turma.teacher ? (
                      <div>
                        <strong>{turma.teacher.nome}</strong>
                        {turma.teacher.fallback && (
                          <span style={{ display: 'block', fontSize: '11px', color: '#b45309' }}>
                            (Fallback Resiliente)
                          </span>
                        )}
                        <span style={{ display: 'block', fontSize: '12px', color: '#64748b' }}>
                          {turma.teacher.especialidade}
                        </span>
                      </div>
                    ) : (
                      <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Não alocado</span>
                    )}
                  </td>
                  <td>
                    <div style={{ fontWeight: '500', color: '#0f172a' }}>
                      {turma.horario || '-'}
                    </div>
                  </td>
                  <td>
                    <div style={{ fontSize: '13px', color: '#334155', fontWeight: '500' }}>
                      {turma.sala || '-'}
                    </div>
                  </td>
                  <td>
                    <span style={{ fontWeight: '600', color: '#166534' }}>
                      {turma.capacidadeMaxima || 40} vagas
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button className="btn-secondary" onClick={() => openEditModal(turma)}>
                        Editar
                      </button>
                      <button className="btn-danger" onClick={() => onDeleteClass(turma.id)}>
                        Excluir
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" style={{ maxWidth: '580px' }} onClick={(e) => e.stopPropagation()}>
            <h3>{isEditing ? 'Editar Turma' : 'Criar Nova Turma'}</h3>
            <form onSubmit={handleSubmit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '12px' }}>
                <div className="form-group">
                  <label>Código da Turma:</label>
                  <input
                    type="text"
                    required
                    value={classForm.codigo}
                    onChange={(e) => setClassForm({ ...classForm, codigo: e.target.value })}
                    placeholder="Ex: TURMA-301"
                  />
                </div>
                <div className="form-group">
                  <label>Nome da Disciplina:</label>
                  <input
                    type="text"
                    required
                    value={classForm.nomeDisciplina}
                    onChange={(e) => setClassForm({ ...classForm, nomeDisciplina: e.target.value })}
                    placeholder="Ex: Sistemas Distribuídos e Microsserviços"
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div className="form-group">
                  <label>Semestre Letivo:</label>
                  <select
                    value={classForm.semestre}
                    onChange={(e) => setClassForm({ ...classForm, semestre: e.target.value })}
                  >
                    <option value="2026.1">2026.1</option>
                    <option value="2026.2">2026.2</option>
                    <option value="2027.1">2027.1</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Capacidade Máxima (Vagas):</label>
                  <input
                    type="number"
                    min="5"
                    max="120"
                    required
                    value={classForm.capacidadeMaxima}
                    onChange={(e) => setClassForm({ ...classForm, capacidadeMaxima: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Professor Responsável (teacher-service):</label>
                <select
                  value={classForm.teacherId}
                  onChange={(e) => setClassForm({ ...classForm, teacherId: e.target.value })}
                >
                  <option value="">-- Selecione o Professor Responsável --</option>
                  {teachers.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.nome} - {t.especialidade} ({t.matricula})
                    </option>
                  ))}
                </select>
              </div>

              {/* Seção Estruturada de Horário */}
              <div style={{
                background: '#f1f5f9',
                padding: '12px',
                borderRadius: '8px',
                marginBottom: '16px'
              }}>
                <span style={{ fontSize: '13px', fontWeight: '700', color: '#1e293b', display: 'block', marginBottom: '8px' }}>
                  Definição de Horário (Dias e Faixa Horária):
                </span>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <div className="form-group" style={{ margin: 0 }}>
                    <label>Dias da Semana:</label>
                    <select
                      value={classForm.diasSemana}
                      onChange={(e) => setClassForm({ ...classForm, diasSemana: e.target.value })}
                    >
                      {DIAS_OPCOES.map((d) => (
                        <option key={d} value={d}>{d}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group" style={{ margin: 0 }}>
                    <label>Faixa de Horário:</label>
                    <select
                      value={classForm.faixaHorario}
                      onChange={(e) => setClassForm({ ...classForm, faixaHorario: e.target.value })}
                    >
                      {HORARIOS_OPCOES.map((h) => (
                        <option key={h} value={h}>{h}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              {/* Seção Estruturada de Local / Sala */}
              <div style={{
                background: '#f1f5f9',
                padding: '12px',
                borderRadius: '8px',
                marginBottom: '16px'
              }}>
                <span style={{ fontSize: '13px', fontWeight: '700', color: '#1e293b', display: 'block', marginBottom: '8px' }}>
                  Alocação de Espaço Físico (Bloco e Sala/Lab):
                </span>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <div className="form-group" style={{ margin: 0 }}>
                    <label>Bloco / Edifício:</label>
                    <select
                      value={classForm.bloco}
                      onChange={(e) => setClassForm({ ...classForm, bloco: e.target.value })}
                    >
                      {BLOCOS_OPCOES.map((b) => (
                        <option key={b} value={b}>{b}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group" style={{ margin: 0 }}>
                    <label>Sala ou Laboratório:</label>
                    <select
                      value={classForm.salaNumero}
                      onChange={(e) => setClassForm({ ...classForm, salaNumero: e.target.value })}
                    >
                      {SALAS_OPCOES.map((s) => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              <div className="modal-actions">
                <button type="submit" className="btn-primary" disabled={loading}>
                  {loading ? 'Salvando...' : 'Salvar Turma'}
                </button>
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
