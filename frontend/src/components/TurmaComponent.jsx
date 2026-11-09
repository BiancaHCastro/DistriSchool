import React, { useState, useEffect, useMemo } from 'react';

const API_BASE = 'http://localhost:8080/api';

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

export default function TurmaComponent() {
  const [classes, setClasses] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  const [showModal, setShowModal] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // Filtros de busca na listagem
  const [filtroTexto, setFiltroTexto] = useState('');
  const [filtroSemestre, setFiltroSemestre] = useState('');
  const [filtroProfessor, setFiltroProfessor] = useState('');

  // Formulário sem localização física
  const [classForm, setClassForm] = useState({
    id: null,
    codigo: '',
    nomeDisciplina: '',
    semestre: '2026.1',
    diasSemana: 'Seg / Qua',
    faixaHorario: '07:30 - 09:10 (Manhã A)',
    capacidadeMaxima: 40,
    teacherId: ''
  });

  useEffect(() => {
    fetchClasses();
    fetchTeachers();
  }, []);

  const fetchClasses = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/turmas`);
      if (!res.ok) throw new Error('Erro ao buscar turmas');
      const data = await res.json();
      setClasses(data);
    } catch (err) {
      console.error('Erro ao conectar com turma-service:', err);
      setMessage({ type: 'error', text: 'Não foi possível conectar ao turma-service.' });
    } finally {
      setLoading(false);
    }
  };

  const fetchTeachers = async () => {
    try {
      const res = await fetch(`${API_BASE}/teachers`);
      if (res.ok) {
        const data = await res.json();
        setTeachers(data);
      }
    } catch (err) {
      console.error('Erro ao buscar professores:', err);
    }
  };

  const openNewModal = () => {
    setClassForm({
      id: null,
      codigo: '',
      nomeDisciplina: '',
      semestre: '2026.1',
      diasSemana: 'Seg / Qua',
      faixaHorario: '07:30 - 09:10 (Manhã A)',
      capacidadeMaxima: 40,
      teacherId: teachers.length > 0 ? teachers[0].id : ''
    });
    setIsEditing(false);
    setShowModal(true);
  };

  const openEditModal = (turma) => {
    let dias = 'Seg / Qua';
    let faixa = '07:30 - 09:10 (Manhã A)';
    if (turma.horario && turma.horario.includes(' - ')) {
      const parts = turma.horario.split(' ');
      if (parts.length >= 3) {
        dias = parts.slice(0, 3).join(' ');
        faixa = parts.slice(3).join(' ') || faixa;
      }
    }

    setClassForm({
      id: turma.id,
      codigo: turma.codigo,
      nomeDisciplina: turma.nomeDisciplina,
      semestre: turma.semestre,
      diasSemana: dias,
      faixaHorario: faixa,
      capacidadeMaxima: turma.capacidadeMaxima || 40,
      teacherId: turma.teacherId || ''
    });
    setIsEditing(true);
    setShowModal(true);
  };

  const handleSaveClass = async (payload, editing) => {
    setLoading(true);
    try {
      const url = editing ? `${API_BASE}/turmas/${payload.id}` : `${API_BASE}/turmas`;
      const method = editing ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!res.ok) throw new Error('Erro ao salvar turma no servidor');

      setMessage({ type: 'success', text: editing ? 'Turma atualizada com sucesso!' : 'Turma criada com sucesso e evento RabbitMQ emitido!' });
      fetchClasses();
      return true;
    } catch (err) {
      setMessage({ type: 'error', text: `Erro ao salvar turma: ${err.message}` });
      return false;
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClass = async (id) => {
    if (!window.confirm('Deseja realmente excluir esta turma?')) return;
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/turmas/${id}`, { method: 'DELETE' });
      if (!res.ok) throw new Error('Erro ao excluir turma');
      setMessage({ type: 'success', text: 'Turma excluída com sucesso!' });
      fetchClasses();
    } catch (err) {
      setMessage({ type: 'error', text: `Erro ao excluir: ${err.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const horarioComposto = `${classForm.diasSemana} ${classForm.faixaHorario}`;

    const payload = {
      id: classForm.id,
      codigo: classForm.codigo.trim().toUpperCase(),
      nomeDisciplina: classForm.nomeDisciplina.trim(),
      semestre: classForm.semestre.trim(),
      horario: horarioComposto,
      capacidadeMaxima: Number(classForm.capacidadeMaxima),
      teacherId: classForm.teacherId ? Number(classForm.teacherId) : null
    };

    const success = await handleSaveClass(payload, isEditing);
    if (success) {
      setShowModal(false);
    }
  };

  const turmasFiltradas = useMemo(() => {
    return classes.filter((turma) => {
      const matchesTexto =
        !filtroTexto ||
        turma.codigo.toLowerCase().includes(filtroTexto.toLowerCase()) ||
        turma.nomeDisciplina.toLowerCase().includes(filtroTexto.toLowerCase());

      const matchesSemestre = !filtroSemestre || turma.semestre === filtroSemestre;

      const matchesProfessor =
        !filtroProfessor ||
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
          <h2>Gestão de Turmas & Horários (turma-service)</h2>
          <span style={{ fontSize: '13px', color: '#64748b' }}>
            Planejamento acadêmico com horários e corpo docente (sem localização física)
          </span>
        </div>
        <button className="btn-primary" onClick={openNewModal}>
          + Criar Nova Turma
        </button>
      </div>

      {message.text && (
        <div className={`alert alert-${message.type}`} style={{ margin: '16px 0' }}>
          {message.text}
        </div>
      )}

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
              <th>Docente Alocado</th>
              <th>Horário (Dias / Turno)</th>
              <th>Capacidade</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {turmasFiltradas.length === 0 ? (
              <tr>
                <td colSpan="8" className="text-center">
                  Nenhuma turma encontrada com os filtros selecionados.
                </td>
              </tr>
            ) : (
              turmasFiltradas.map((turma) => {
                const professorObj = teachers.find(t => t.id === turma.teacherId);
                return (
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
                      {professorObj ? (
                        <div>
                          <strong>{professorObj.nome}</strong>
                          <span style={{ display: 'block', fontSize: '12px', color: '#64748b' }}>
                            {professorObj.especialidade}
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
                      <span style={{ fontWeight: '600', color: '#166534' }}>
                        {turma.capacidadeMaxima || 40} vagas
                      </span>
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="btn-secondary" onClick={() => openEditModal(turma)}>
                          Editar
                        </button>
                        <button className="btn-danger" onClick={() => handleDeleteClass(turma.id)}>
                          Excluir
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" style={{ maxWidth: '540px' }} onClick={(e) => e.stopPropagation()}>
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
                    placeholder="Ex: Sistemas Distribuídos"
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
                <label>Professor Responsável:</label>
                <select
                  value={classForm.teacherId}
                  onChange={(e) => setClassForm({ ...classForm, teacherId: e.target.value })}
                >
                  <option value="">-- Selecione o Professor Responsável --</option>
                  {teachers.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.nome} - {t.especialidade}
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
