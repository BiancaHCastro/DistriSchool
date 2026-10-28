import React, { useState, useEffect } from 'react';
import './App.css';

const API_BASE = 'http://localhost:8080/api';

export default function App() {
  const [activeTab, setActiveTab] = useState('chamada');
  const [turmaId, setTurmaId] = useState('TURMA-101');
  const [dataChamada, setDataChamada] = useState(new Date().toISOString().split('T')[0]);
  const [students, setStudents] = useState([]);
  const [attendanceStatus, setAttendanceStatus] = useState({});
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  // Student CRUD modal & form state
  const [studentForm, setStudentForm] = useState({ id: null, matricula: '', nome: '', dataNascimento: '', endereco: '', contato: '' });
  const [isEditing, setIsEditing] = useState(false);
  const [showModal, setShowModal] = useState(false);

  // Fetch students from aluno-service via API Gateway
  useEffect(() => {
    fetchStudents();
  }, []);

  const fetchStudents = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/students`);
      if (!res.ok) throw new Error(`Erro HTTP ao buscar alunos: ${res.status}`);
      const data = await res.json();
      setStudents(data);
      const initial = {};
      data.forEach(s => {
        initial[s.id] = 'PRESENCA';
      });
      setAttendanceStatus(initial);
      setMessage({ type: '', text: '' });
    } catch (err) {
      console.error('Erro ao conectar com aluno-service:', err);
      setMessage({ type: 'error', text: 'Não foi possível conectar ao backend (aluno-service). Verifique se o Docker está rodando.' });
      setStudents([]);
    } finally {
      setLoading(false);
    }
  };

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

  // Submit attendance to attendance-service (which emits Kafka event)
  const handleSubmitChamada = async (e) => {
    e.preventDefault();
    if (students.length === 0) {
      setMessage({ type: 'error', text: 'Nenhum aluno cadastrado para realizar a chamada.' });
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
        return fetch(`${API_BASE}/academic/presencas`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      });

      const responses = await Promise.all(promises);
      const failed = responses.find(r => !r.ok);
      if (failed) throw new Error('Erro ao salvar algumas presenças no servidor');

      setMessage({ type: 'success', text: 'Chamada registrada com sucesso e evento Kafka emitido!' });
      fetchReport();
    } catch (err) {
      setMessage({ type: 'error', text: `Erro ao enviar chamada: ${err.message}` });
    } finally {
      setLoading(false);
    }
  };

  const fetchReport = async () => {
    try {
      const res = await fetch(`${API_BASE}/academic/presencas/relatorio?turmaId=${turmaId}`);
      if (!res.ok) throw new Error('Falha ao carregar relatório');
      const data = await res.json();
      setReport(data);
    } catch (err) {
      console.error('Erro ao carregar relatório:', err);
      setReport({
        totalAulas: 0,
        presencas: 0,
        faltas: 0,
        percentualPresenca: 100.0,
        historico: []
      });
    }
  };

  useEffect(() => {
    if (activeTab === 'relatorio') {
      fetchReport();
    }
  }, [activeTab, turmaId]);

  // Student CRUD operations communicating directly with aluno-service
  // Note: aluno-service expects dataNascimento in dd/MM/yyyy format per @JsonFormat
  const handleSaveStudent = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const url = isEditing ? `${API_BASE}/students/${studentForm.id}` : `${API_BASE}/students`;
      const method = isEditing ? 'PUT' : 'POST';

      let formattedDate = studentForm.dataNascimento;
      if (formattedDate && formattedDate.includes('-')) {
        const [year, month, day] = formattedDate.split('-');
        formattedDate = `${day}/${month}/${year}`;
      }

      const payload = {
        ...studentForm,
        dataNascimento: formattedDate || null
      };

      const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!res.ok) throw new Error('Erro ao salvar aluno no servidor');

      setMessage({ type: 'success', text: isEditing ? 'Aluno atualizado com sucesso!' : 'Aluno cadastrado com sucesso!' });
      setShowModal(false);
      setStudentForm({ id: null, matricula: '', nome: '', dataNascimento: '', endereco: '', contato: '' });
      setIsEditing(false);
      fetchStudents();
    } catch (err) {
      setMessage({ type: 'error', text: `Erro no CRUD de Alunos: ${err.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleEditStudent = (student) => {
    // Convert dd/MM/yyyy back to yyyy-MM-dd for HTML date input if needed
    let rawDate = student.dataNascimento || '';
    if (rawDate && rawDate.includes('/')) {
      const [day, month, year] = rawDate.split('/');
      rawDate = `${year}-${month}-${day}`;
    }
    setStudentForm({
      ...student,
      dataNascimento: rawDate
    });
    setIsEditing(true);
    setShowModal(true);
  };

  const handleDeleteStudent = async (id) => {
    if (!window.confirm('Deseja realmente excluir este aluno?')) return;
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/students/${id}`, { method: 'DELETE' });
      if (!res.ok) throw new Error('Erro ao excluir aluno');
      setMessage({ type: 'success', text: 'Aluno excluído com sucesso!' });
      fetchStudents();
    } catch (err) {
      setMessage({ type: 'error', text: `Erro ao excluir: ${err.message}` });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="distrischool-container">
      <header className="app-header">
        <div className="header-brand">
          <h1>DistriSchool</h1>
          <span>Plataforma de Gestão Escolar Distribuída</span>
        </div>
        <nav className="header-nav">
          <button
            className={activeTab === 'chamada' ? 'active' : ''}
            onClick={() => setActiveTab('chamada')}
          >
            Chamada Diária
          </button>
          <button
            className={activeTab === 'alunos' ? 'active' : ''}
            onClick={() => setActiveTab('alunos')}
          >
            Gestão de Alunos (CRUD)
          </button>
          <button
            className={activeTab === 'relatorio' ? 'active' : ''}
            onClick={() => setActiveTab('relatorio')}
          >
            Relatório de Frequência
          </button>
        </nav>
      </header>

      {message.text && (
        <div className={`alert alert-${message.type}`}>
          {message.text}
        </div>
      )}

      <main className="app-main">
        {activeTab === 'chamada' && (
          <div className="card attendance-card">
            <div className="card-header">
              <h2>Registro de Chamada de Aula</h2>
              <div className="filters">
                <div className="filter-group">
                  <label>Turma:</label>
                  <select value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
                    <option value="TURMA-101">Turma 101 - Ensino Médio</option>
                    <option value="TURMA-102">Turma 102 - Ensino Médio</option>
                    <option value="TURMA-201">Turma 201 - Informática</option>
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
                      <th>Nome do Aluno</th>
                      <th>Status da Presença</th>
                    </tr>
                  </thead>
                  <tbody>
                    {students.length === 0 ? (
                      <tr>
                        <td colSpan="3" className="text-center">
                          Nenhum aluno cadastrado no microsserviço. Cadastre alunos na aba <strong>Gestão de Alunos</strong>.
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
                <button type="submit" className="btn-primary" disabled={loading || students.length === 0}>
                  {loading ? 'Salvando Chamada...' : 'Salvar Chamada e Emitir Evento Kafka'}
                </button>
              </div>
            </form>
          </div>
        )}

        {activeTab === 'alunos' && (
          <div className="card students-card">
            <div className="card-header">
              <h2>Gerenciamento de Alunos (aluno-service)</h2>
              <button
                className="btn-primary"
                onClick={() => {
                  setStudentForm({ id: null, matricula: '', nome: '', dataNascimento: '', endereco: '', contato: '' });
                  setIsEditing(false);
                  setShowModal(true);
                }}
              >
                + Cadastrar Novo Aluno
              </button>
            </div>

            <div className="table-responsive">
              <table className="attendance-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Matrícula</th>
                    <th>Nome</th>
                    <th>Nascimento</th>
                    <th>Endereço</th>
                    <th>Contato</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {students.length === 0 ? (
                    <tr>
                      <td colSpan="7" className="text-center">Nenhum aluno encontrado no banco de dados.</td>
                    </tr>
                  ) : (
                    students.map(student => (
                      <tr key={student.id}>
                        <td>{student.id}</td>
                        <td><code>{student.matricula}</code></td>
                        <td className="student-name">{student.nome}</td>
                        <td>{student.dataNascimento || '-'}</td>
                        <td>{student.endereco || '-'}</td>
                        <td>{student.contato || '-'}</td>
                        <td>
                          <div className="action-buttons">
                            <button className="btn-secondary" onClick={() => handleEditStudent(student)}>Editar</button>
                            <button className="btn-danger" onClick={() => handleDeleteStudent(student.id)}>Excluir</button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {showModal && (
              <div className="modal-overlay">
                <div className="modal-content">
                  <h3>{isEditing ? 'Editar Aluno' : 'Cadastrar Novo Aluno'}</h3>
                  <form onSubmit={handleSaveStudent}>
                    <div className="form-group">
                      <label>Matrícula:</label>
                      <input
                        type="text"
                        required
                        value={studentForm.matricula}
                        onChange={(e) => setStudentForm({ ...studentForm, matricula: e.target.value })}
                        placeholder="Ex: 2026101"
                      />
                    </div>
                    <div className="form-group">
                      <label>Nome Completo:</label>
                      <input
                        type="text"
                        required
                        value={studentForm.nome}
                        onChange={(e) => setStudentForm({ ...studentForm, nome: e.target.value })}
                        placeholder="Ex: Ana Beatriz Souza"
                      />
                    </div>
                    <div className="form-group">
                      <label>Data de Nascimento:</label>
                      <input
                        type="date"
                        value={studentForm.dataNascimento || ''}
                        onChange={(e) => setStudentForm({ ...studentForm, dataNascimento: e.target.value })}
                      />
                    </div>
                    <div className="form-group">
                      <label>Endereço:</label>
                      <input
                        type="text"
                        value={studentForm.endereco || ''}
                        onChange={(e) => setStudentForm({ ...studentForm, endereco: e.target.value })}
                        placeholder="Ex: Av. Beira Mar, 1000"
                      />
                    </div>
                    <div className="form-group">
                      <label>Contato:</label>
                      <input
                        type="text"
                        value={studentForm.contato || ''}
                        onChange={(e) => setStudentForm({ ...studentForm, contato: e.target.value })}
                        placeholder="Ex: (85) 99999-8888"
                      />
                    </div>
                    <div className="modal-actions">
                      <button type="submit" className="btn-primary" disabled={loading}>Salvar</button>
                      <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>Cancelar</button>
                    </div>
                  </form>
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'relatorio' && (
          <div className="card report-card">
            <div className="card-header">
              <h2>Relatório de Frequência - {turmaId}</h2>
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

                <div className="table-responsive" styleльники style={{ marginTop: '24px' }}>
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
      </main>
    </div>
  );
}
