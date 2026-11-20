import Notification from './Notification';
import ExportButtons from './ExportButtons';
import {apiFetch, API_BASE} from '../auth/http';
import React, { useState, useEffect } from 'react';



export default function AttendanceComponent() {
  const [activeSubTab, setActiveSubTab] = useState('chamada');
  const [classes, setClasses] = useState([]);
  const [turmaId, setTurmaId] = useState('');
  const [dataChamada, setDataChamada] = useState(new Date().toISOString().split('T')[0]);
  const [allStudents, setAllStudents] = useState([]);
  const [enrolledStudentIds, setEnrolledStudentIds] = useState(new Set());
  const [students, setStudents] = useState([]);
  const [attendanceStatus, setAttendanceStatus] = useState({});
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  useEffect(() => {
    fetchClassesAndStudents();
  }, []);

  const fetchClassesAndStudents = async () => {
    setLoading(true);
    try {
      // 1. Fetch all students
      const studRes = await apiFetch(`${API_BASE}/students`);
      const studData = studRes.ok ? await studRes.json() : [];
      setAllStudents(studData);

      // 2. Fetch classes (turmas)
      const turmaRes = await apiFetch(`${API_BASE}/turmas`);
      const turmaData = turmaRes.ok ? await turmaRes.json() : [];
      setClasses(turmaData);

      if (turmaData.length > 0) {
        const firstTurmaId = turmaData[0].codigo || turmaData[0].id;
        setTurmaId(firstTurmaId);
        await fetchEnrolledForTurma(turmaData[0].id, studData);
      }
    } catch (err) {
      console.error('Erro ao carregar dados iniciais:', err);
      setMessage({ type: 'error', text: 'Erro ao conectar com os microsserviços.' });
    } finally {
      setLoading(false);
    }
  };

  const fetchEnrolledForTurma = async (turmaDbId, studentList = allStudents) => {
    try {
      const res = await apiFetch(`${API_BASE}/turmas/${turmaDbId}/alunos`);
      if (res.ok) {
        const matriculas = await res.json();
        const enrolledIds = new Set(matriculas.map(m => m.alunoId));
        setEnrolledStudentIds(enrolledIds);

        const filtered = studentList.filter(s => enrolledIds.has(s.id));
        setStudents(filtered);

        const initial = {};
        filtered.forEach(s => {
          initial[s.id] = 'PRESENCA';
        });
        setAttendanceStatus(initial);
      } else {
        setStudents([]);
        setAttendanceStatus({});
      }
    } catch (err) {
      console.error('Erro ao buscar alunos matriculados na turma:', err);
      setStudents([]);
    }
  };

  // When turmaId changes via dropdown selection
  useEffect(() => {
    if (turmaId && classes.length > 0) {
      const selectedClassObj = classes.find(c => String(c.codigo) === String(turmaId) || String(c.id) === String(turmaId));
      if (selectedClassObj) {
        fetchEnrolledForTurma(selectedClassObj.id, allStudents);
      }
    }
  }, [turmaId, classes]);

  const handleStatusChange = (studentId, status) => {
    setAttendanceStatus(prev => ({
      ...prev,
      [studentId]: status
    }));
  };

  const handleMarkAll = (status) => {
    const updated = {};
    students.forEach(s => {
      updated[s.id] = status;
    });
    setAttendanceStatus(updated);
  };

  const handleSubmitChamada = async (e) => {
    e.preventDefault();
    if (students.length === 0) {
      setMessage({ type: 'error', text: 'Nenhum aluno matriculado nesta turma para realizar a chamada.' });
      return;
    }
    if (!turmaId) {
      setMessage({ type: 'error', text: 'Selecione uma turma válida.' });
      return;
    }
    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      const promises = students.map(student => {
        const payload = {
          alunoId: student.id,
          turmaId: turmaId,
          data: dataChamada,
          status: attendanceStatus[student.id] || 'PRESENCA'
        };
        return apiFetch(`${API_BASE}/academic/presencas`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      });

      const responses = await Promise.all(promises);
      const failed = responses.find(r => !r.ok);
      if (failed) throw new Error('Erro ao salvar algumas presenças no servidor');

      setMessage({ type: 'success', text: 'Chamada registrada com sucesso para os alunos matriculados!' });
      fetchReport();
    } catch (err) {
      setMessage({ type: 'error', text: `Erro ao enviar chamada: ${err.message}` });
    } finally {
      setLoading(false);
    }
  };

  const fetchReport = async () => {
    if (!turmaId) return;
    try {
      const res = await apiFetch(`${API_BASE}/academic/presencas/relatorio?turmaId=${turmaId}`);
      if (!res.ok) throw new Error('Falha ao carregar relatório');
      const data = await res.json();
      setReport(data);
    } catch (err) {
      console.error('Erro ao carregar relatório:', err);
      setReport(null);
      setMessage({type:'error',text:err.message});
    }
  };

  useEffect(() => {
    if (activeSubTab === 'relatorio' && turmaId) {
      fetchReport();
    }
  }, [activeSubTab, turmaId]);

  return (
    <div>
      <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
        <button
          className={`btn-secondary ${activeSubTab === 'chamada' ? 'btn-primary' : ''}`}
          onClick={() => setActiveSubTab('chamada')}
        >
          Registro de Chamada
        </button>
        <button
          className={`btn-secondary ${activeSubTab === 'relatorio' ? 'btn-primary' : ''}`}
          onClick={() => setActiveSubTab('relatorio')}
        >
          Relatório de Frequência
        </button>
      </div>

      <Notification text={message.text} type={message.type} onClose={() => setMessage({ type: '', text: '' })}/>

      {activeSubTab === 'chamada' && (
        <div className="card attendance-card">
          <div className="card-header">
            <h2>Registro de Chamada de Aula (Alunos Matriculados na Turma)</h2>
            <div className="filters">
              <div className="filter-group">
                <label>Turma:</label>
                <select
                  value={turmaId}
                  onChange={(e) => setTurmaId(e.target.value)}
                  style={{ minWidth: '240px', fontWeight: '500' }}
                >
                  {classes.length === 0 ? (
                    <option value="">Nenhuma turma cadastrada</option>
                  ) : (
                    classes.map((c) => (
                      <option key={c.id} value={c.codigo || c.id}>
                        {c.codigo || `Turma ${c.id}`} - {c.nomeDisciplina || c.nome || 'Disciplina'}
                      </option>
                    ))
                  )}
                </select>
              </div>
              <div className="filter-group">
                <label>Data:</label>
                <input
                  type="date"
                  value={dataChamada}
                  onChange={(e) => setDataChamada(e.target.value)}
                />
              </div>
            </div>
          </div>

          <div className="batch-actions">
            <span>Marcar todos como:</span>
            <button type="button" className="btn-success" onClick={() => handleMarkAll('PRESENCA')}>Presente</button>
            <button type="button" className="btn-danger" onClick={() => handleMarkAll('FALTA')}>Falta</button>
            <button type="button" className="btn-warning" onClick={() => handleMarkAll('ATRASO')}>Atraso</button>
          </div>

          <form onSubmit={handleSubmitChamada}>
            <div className="table-responsive">
              <table className="attendance-table">
                <thead>
                  <tr>
                    <th>Matrícula</th>
                    <th>Nome do Aluno Matriculado</th>
                    <th>Status da Presença</th>
                  </tr>
                </thead>
                <tbody>
                  {students.length === 0 ? (
                    <tr>
                      <td colSpan="3" className="text-center">
                        Nenhum aluno matriculado nesta turma. Matricule alunos na aba <strong>Turmas & Horários</strong>.
                      </td>
                    </tr>
                  ) : (
                    students.map(student => (
                      <tr key={student.id}>
                        <td><code>{student.matricula}</code></td>
                        <td className="student-name">{student.nome}</td>
                        <td>
                          <div className="status-buttons">
                            <button
                              type="button"
                              className={`status-btn ${attendanceStatus[student.id] === 'PRESENCA' ? 'selected-presenca' : ''}`}
                              onClick={() => handleStatusChange(student.id, 'PRESENCA')}
                            >
                              Presente
                            </button>
                            <button
                              type="button"
                              className={`status-btn ${attendanceStatus[student.id] === 'FALTA' ? 'selected-falta' : ''}`}
                              onClick={() => handleStatusChange(student.id, 'FALTA')}
                            >
                              Falta
                            </button>
                            <button
                              type="button"
                              className={`status-btn ${attendanceStatus[student.id] === 'ATRASO' ? 'selected-atraso' : ''}`}
                              onClick={() => handleStatusChange(student.id, 'ATRASO')}
                            >
                              Atraso
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn-primary" disabled={loading || students.length === 0 || !turmaId}>
                {loading ? 'Salvando Chamada...' : 'Salvar chamada'}
              </button>
            </div>
          </form>
        </div>
      )}

      {activeSubTab === 'relatorio' && (
        <div className="card report-card">
          <div className="card-header">
            <h2>Relatório de Frequência - {turmaId || 'Selecione uma Turma'}</h2>
            <ExportButtons endpoint="/attendance/relatorio/export" filename="frequencia" turmaId={turmaId}/>
            <button className="btn-secondary" onClick={fetchReport}>Atualizar Relatório</button>
          </div>

          {report ? (
            <>
              <div className="stats-grid">
                <div className="stat-box">
                  <h3>Total de Aulas</h3>
                  <p className="stat-value">{report.totalAulas ?? 0}</p>
                </div>
                <div className="stat-box">
                  <h3>Presenças / Atrasos</h3>
                  <p className="stat-value success">{report.presencas ?? 0}</p>
                </div>
                <div className="stat-box">
                  <h3>Faltas Registradas</h3>
                  <p className="stat-value danger">{report.faltas ?? 0}</p>
                </div>
                <div className="stat-box">
                  <h3>Taxa de Presença</h3>
                  <p className="stat-value primary">{(report.percentualPresenca ?? 100).toFixed(1)}%</p>
                </div>
              </div>

              <div className="table-responsive" style={{ marginTop: '24px' }}>
                <h3>Histórico de Registros (attendance-service)</h3>
                <table className="attendance-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Aluno ID</th>
                      <th>Turma</th>
                      <th>Data</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.historico && report.historico.length > 0 ? (
                      report.historico.map(item => (
                        <tr key={item.id}>
                          <td>{item.id}</td>
                          <td>{item.alunoId}</td>
                          <td>{item.turmaId}</td>
                          <td>{item.data}</td>
                          <td>
                            <span className={`badge badge-${item.status.toLowerCase()}`}>
                              {item.status}
                            </span>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan="5" className="text-center">Nenhum registro encontrado para esta turma.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <p>Carregando relatório...</p>
          )}
        </div>
      )}
    </div>
  );
}
