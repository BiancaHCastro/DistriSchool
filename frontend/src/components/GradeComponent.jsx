import {apiFetch, API_BASE} from '../auth/http';
import React, { useState, useEffect, useMemo } from 'react';



export default function GradeComponent() {
  const [classes, setClasses] = useState([]);
  const [allStudents, setAllStudents] = useState([]);
  const [enrolledStudents, setEnrolledStudents] = useState([]);
  const [grades, setGrades] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  const [selectedTurmaId, setSelectedTurmaId] = useState('');
  const [filtroSituacao, setFiltroSituacao] = useState('ALL');
  const [filtroTexto, setFiltroTexto] = useState('');

  // Modal para lançamento de notas
  const [showModal, setShowModal] = useState(false);
  const [gradeForm, setGradeForm] = useState({
    id: null,
    alunoId: '',
    turmaId: '',
    nota1: '',
    nota2: '',
    notaFinal: ''
  });

  useEffect(() => {
    fetchInitialData();
    fetchGrades();
  }, []);

  const fetchInitialData = async () => {
    try {
      const studRes = await apiFetch(`${API_BASE}/students`);
      const studData = studRes.ok ? await studRes.json() : [];
      setAllStudents(studData);

      const turmaRes = await apiFetch(`${API_BASE}/turmas`);
      const turmaData = turmaRes.ok ? await turmaRes.json() : [];
      setClasses(turmaData);

      if (turmaData.length > 0) {
        const firstTurma = turmaData[0];
        const firstTurmaId = firstTurma.codigo || firstTurma.id;
        setSelectedTurmaId(firstTurmaId);
        await fetchEnrolledForTurma(firstTurma.id, studData);
      }
    } catch (err) {
      console.error('Erro ao buscar turmas/alunos:', err);
    }
  };

  const fetchEnrolledForTurma = async (turmaDbId, studentList = allStudents) => {
    try {
      const res = await apiFetch(`${API_BASE}/turmas/${turmaDbId}/alunos`);
      if (res.ok) {
        const matriculas = await res.json();
        const enrolledIds = new Set(matriculas.map(m => m.alunoId));
        const filtered = studentList.filter(s => enrolledIds.has(s.id));
        setEnrolledStudents(filtered);
      } else {
        setEnrolledStudents([]);
      }
    } catch (err) {
      console.error('Erro ao buscar alunos matriculados na turma:', err);
      setEnrolledStudents([]);
    }
  };

  useEffect(() => {
    if (selectedTurmaId && classes.length > 0) {
      const selectedClassObj = classes.find(c => String(c.codigo) === String(selectedTurmaId) || String(c.id) === String(selectedTurmaId));
      if (selectedClassObj) {
        fetchEnrolledForTurma(selectedClassObj.id, allStudents);
      }
    }
  }, [selectedTurmaId, classes]);

  const fetchStudents = async () => {
    // mantido para compatibilidade ou uso interno se necessário
  };

  const fetchGrades = async () => {
    setLoading(true);
    try {
      const res = await apiFetch(`${API_BASE}/notas`);
      if (res.ok) {
        const data = await res.json();
        setGrades(data);
      }
    } catch (err) {
      console.error('Erro ao buscar notas:', err);
      setMessage({ type: 'error', text: 'Não foi possível conectar ao nota-service.' });
    } finally {
      setLoading(false);
    }
  };

  const selectedClass = useMemo(() => {
    return classes.find((c) => c.codigo === selectedTurmaId || String(c.id) === String(selectedTurmaId)) || classes[0] || null;
  }, [classes, selectedTurmaId]);

  const openGradeModal = (aluno) => {
    const turmaCode = selectedTurmaId || (selectedClass ? selectedClass.codigo : 'TURMA-101');
    const existing = grades.find(
      (g) => Number(g.alunoId) === Number(aluno.id) && (g.turmaId === turmaCode || String(g.turmaId) === String(selectedTurmaId))
    );
    setGradeForm({
      id: existing?.id || null,
      alunoId: aluno.id,
      turmaId: turmaCode,
      nota1: existing?.nota1 ?? '',
      nota2: existing?.nota2 ?? '',
      notaFinal: existing?.notaFinal ?? ''
    });
    setShowModal(true);
  };

  const handleSaveGrade = async (formData) => {
    setLoading(true);
    try {
      const payload = {
        alunoId: Number(formData.alunoId),
        turmaId: String(formData.turmaId),
        nota1: formData.nota1 !== '' ? Number(formData.nota1) : null,
        nota2: formData.nota2 !== '' ? Number(formData.nota2) : null,
        notaFinal: formData.notaFinal !== '' ? Number(formData.notaFinal) : null
      };

      const res = await apiFetch(`${API_BASE}/notas`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!res.ok) throw new Error('Erro ao salvar nota no servidor');

      setMessage({ type: 'success', text: 'Nota salva com sucesso!' });
      fetchGrades();
      return true;
    } catch (err) {
      setMessage({ type: 'error', text: `Erro ao lançar nota: ${err.message}` });
      return false;
    } finally {
      setLoading(false);
    }
  };

  const handleModalSubmit = async (e) => {
    e.preventDefault();
    const success = await handleSaveGrade(gradeForm);
    if (success) {
      setShowModal(false);
    }
  };

  const gradeMap = useMemo(() => {
    const map = {};
    grades.forEach((g) => {
      const tId = String(g.turmaId);
      if (tId === String(selectedTurmaId) || (selectedClass && tId === String(selectedClass.codigo))) {
        map[g.alunoId] = g;
      }
    });
    return map;
  }, [grades, selectedTurmaId, selectedClass]);

  const alunosFiltrados = useMemo(() => {
    return enrolledStudents.filter((s) => {
      const matchText =
        !filtroTexto ||
        s.nome.toLowerCase().includes(filtroTexto.toLowerCase()) ||
        s.matricula.toLowerCase().includes(filtroTexto.toLowerCase());

      const g = gradeMap[s.id];
      const n1 = g?.nota1 ?? 0;
      const n2 = g?.nota2 ?? 0;
      const media = (n1 + n2) / 2;
      let situacao = 'EM_ANDAMENTO';
      if (g?.nota1 !== undefined && g?.nota2 !== undefined && g?.nota1 !== null && g?.nota2 !== null) {
        situacao = media >= 7 ? 'APROVADO' : media >= 4 ? 'RECUPERACAO' : 'REPROVADO';
      }

      const matchSituacao = filtroSituacao === 'ALL' || situacao === filtroSituacao;

      return matchText && matchSituacao;
    });
  }, [enrolledStudents, filtroTexto, filtroSituacao, gradeMap]);

  return (
    <div className="card grades-card">
      <div className="card-header">
        <div>
          <h2>Gestão de Notas e Rendimento Acadêmico (nota-service)</h2>
          <span style={{ fontSize: '13px', color: '#64748b' }}>
            Lançamento de notas com publicação de eventos assíncronos no <strong>RabbitMQ</strong>
          </span>
        </div>

        <div className="filters">
          <div className="filter-group">
            <label>Selecione a Turma:</label>
            <select
              value={selectedTurmaId}
              onChange={(e) => setSelectedTurmaId(e.target.value)}
              style={{ minWidth: '240px', fontWeight: '500' }}
            >
              {classes.length === 0 ? (
                <option value="TURMA-101">TURMA-101 - Padrão</option>
              ) : (
                classes.map((c) => (
                  <option key={c.id} value={c.codigo || c.id}>
                    {c.codigo || `Turma ${c.id}`} - {c.nomeDisciplina || c.nome || 'Disciplina'}
                  </option>
                ))
              )}
            </select>
          </div>
        </div>
      </div>

      {message.text && (
        <div className={`alert alert-${message.type}`} style={{ margin: '16px 0' }}>
          {message.text}
        </div>
      )}

      {/* Barra de Filtros e Busca de Notas */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
          marginBottom: '20px',
          background: '#f8fafc',
          padding: '14px',
          borderRadius: '8px',
          border: '1px solid #e2e8f0'
        }}
      >
        <div style={{ flex: '1 1 220px' }}>
          <input
            type="text"
            placeholder="Buscar aluno por nome ou matrícula..."
            value={filtroTexto}
            onChange={(e) => setFiltroTexto(e.target.value)}
            style={{ width: '100%', boxSizing: 'border-box' }}
          />
        </div>

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span style={{ fontSize: '13px', color: '#64748b' }}>Filtrar Situação:</span>
          <select
            value={filtroSituacao}
            onChange={(e) => setFiltroSituacao(e.target.value)}
          >
            <option value="ALL">Todas as Situações</option>
            <option value="APROVADO">Aprovado (Média ≥ 7.0)</option>
            <option value="RECUPERACAO">Recuperação (4.0 a 6.9)</option>
            <option value="REPROVADO">Reprovado (&lt; 4.0)</option>
            <option value="EM_ANDAMENTO">Em Andamento</option>
          </select>
        </div>
      </div>

      <div className="table-responsive">
        <table className="attendance-table">
          <thead>
            <tr>
              <th>Matrícula</th>
              <th>Nome do Aluno</th>
              <th>Avaliação 1 (AV1)</th>
              <th>Avaliação 2 (AV2)</th>
              <th>Exame Final (AF)</th>
              <th>Média Calculada</th>
              <th>Situação Acadêmica</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {alunosFiltrados.length === 0 ? (
              <tr>
                <td colSpan="8" className="text-center">
                  Nenhum aluno encontrado para os critérios selecionados nesta turma.
                </td>
              </tr>
            ) : (
              alunosFiltrados.map((aluno) => {
                const g = gradeMap[aluno.id];
                const n1 = g?.nota1 ?? null;
                const n2 = g?.nota2 ?? null;
                const nf = g?.notaFinal ?? null;
                
                let media = null;
                let sit = 'EM_ANDAMENTO';
                if (n1 !== null && n2 !== null) {
                  media = (n1 + n2) / 2;
                  if (nf !== null) {
                    media = (media + nf) / 2;
                    sit = media >= 5 ? 'APROVADO' : 'REPROVADO';
                  } else {
                    sit = media >= 7 ? 'APROVADO' : media >= 4 ? 'RECUPERACAO' : 'REPROVADO';
                  }
                }

                let sitClass = 'badge-atraso';
                let sitLabel = 'Em Andamento';

                if (sit === 'APROVADO') {
                  sitClass = 'badge-presenca';
                  sitLabel = 'Aprovado';
                } else if (sit === 'RECUPERACAO') {
                  sitClass = 'badge-atraso';
                  sitLabel = 'Em Recuperação';
                } else if (sit === 'REPROVADO') {
                  sitClass = 'badge-falta';
                  sitLabel = 'Reprovado';
                }

                return (
                  <tr key={aluno.id}>
                    <td><code>{aluno.matricula}</code></td>
                    <td className="student-name">{aluno.nome}</td>
                    <td>
                      <strong>{n1 !== null ? Number(n1).toFixed(1) : '-'}</strong>
                    </td>
                    <td>
                      <strong>{n2 !== null ? Number(n2).toFixed(1) : '-'}</strong>
                    </td>
                    <td>
                      {nf !== null ? (
                        <strong>{Number(nf).toFixed(1)}</strong>
                      ) : (
                        <span style={{ color: '#94a3b8' }}>-</span>
                      )}
                    </td>
                    <td>
                      {media !== null ? (
                        <span style={{ fontSize: '15px', fontWeight: '700', color: '#0f172a' }}>
                          {media.toFixed(2)}
                        </span>
                      ) : (
                        <span style={{ color: '#94a3b8' }}>-</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${sitClass}`}>
                        {sitLabel}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="btn-secondary"
                        style={{ padding: '6px 12px', fontSize: '13px' }}
                        onClick={() => openGradeModal(aluno)}
                      >
                        Lançar / Editar
                      </button>
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
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3>Lançamento de Notas - {selectedTurmaId}</h3>
            <form onSubmit={handleModalSubmit}>
              <div className="form-group">
                <label>Aluno Selecionado:</label>
                <div style={{ fontWeight: '600', padding: '8px 0', color: '#1e293b' }}>
                  {allStudents.find((s) => s.id === gradeForm.alunoId)?.nome}
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div className="form-group">
                  <label>Avaliação 1 (AV1):</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    value={gradeForm.nota1}
                    onChange={(e) => setGradeForm({ ...gradeForm, nota1: e.target.value })}
                    placeholder="0.0 a 10.0"
                  />
                </div>
                <div className="form-group">
                  <label>Avaliação 2 (AV2):</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    value={gradeForm.nota2}
                    onChange={(e) => setGradeForm({ ...gradeForm, nota2: e.target.value })}
                    placeholder="0.0 a 10.0"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Exame Final / Recuperação (AF - Opcional):</label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  max="10"
                  value={gradeForm.notaFinal}
                  onChange={(e) => setGradeForm({ ...gradeForm, notaFinal: e.target.value })}
                  placeholder="Preencher apenas se em recuperação"
                />
              </div>

              <div className="modal-actions">
                <button type="submit" className="btn-primary" disabled={loading}>
                  {loading ? 'Publicando...' : 'Salvar notas'}
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
