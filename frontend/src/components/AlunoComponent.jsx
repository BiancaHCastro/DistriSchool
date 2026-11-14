import React, { useState, useEffect } from 'react';

const API_BASE = 'http://localhost:8080/api';

export default function AlunoComponent() {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  // Student CRUD modal & form state
  const [studentForm, setStudentForm] = useState({ id: null, matricula: '', nome: '', dataNascimento: '', endereco: '', contato: '' });
  const [isEditing, setIsEditing] = useState(false);
  const [showModal, setShowModal] = useState(false);

  // Student Details Modal (Turmas Matriculadas)
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [studentTurmas, setStudentTurmas] = useState([]);

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
      setMessage({ type: '', text: '' });
    } catch (err) {
      console.error('Erro ao conectar com aluno-service:', err);
      setMessage({ type: 'error', text: 'Não foi possível conectar ao aluno-service (Verifique se o Docker está rodando).' });
      setStudents([]);
    } finally {
      setLoading(false);
    }
  };

  const openStudentDetails = async (student) => {
    setSelectedStudent(student);
    setShowDetailsModal(true);
    setLoading(true);
    try {
      // 1. Fetch all turmas
      const turmaRes = await fetch(`${API_BASE}/turmas`);
      if (!turmaRes.ok) throw new Error('Erro ao buscar turmas');
      const allTurmas = await turmaRes.json();

      // 2. Check enrollments for each turma
      const enrolledTurmas = [];
      for (const turma of allTurmas) {
        const matRes = await fetch(`${API_BASE}/turmas/${turma.id}/alunos`);
        if (matRes.ok) {
          const matriculas = await matRes.json();
          if (matriculas.some(m => m.alunoId === student.id)) {
            enrolledTurmas.push(turma);
          }
        }
      }
      setStudentTurmas(enrolledTurmas);
    } catch (err) {
      console.error('Erro ao carregar turmas do aluno:', err);
      setStudentTurmas([]);
    } finally {
      setLoading(false);
    }
  };

  // Limites de data de nascimento (Idade entre 14 e 100 anos)
  const today = new Date();
  const maxBirthDateYear = today.getFullYear() - 14;
  const minBirthDateYear = today.getFullYear() - 100;
  const maxDateStr = `${maxBirthDateYear}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  const minDateStr = `${minBirthDateYear}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

  // Formatação de Contato: (85) 98888-8888
  const formatPhone = (value) => {
    const digits = value.replace(/\D/g, '').slice(0, 11);
    if (digits.length <= 2) {
      return digits.length ? `(${digits}` : '';
    } else if (digits.length <= 7) {
      return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
    } else {
      return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
    }
  };

  const openNewModal = () => {
    const currentYear = today.getFullYear();
    const seq = students.length + 1;
    const matriculaGerada = `${currentYear}2${String(seq).padStart(4, '0')}`;

    setStudentForm({
      id: null,
      matricula: matriculaGerada,
      nome: '',
      dataNascimento: '',
      endereco: '',
      contato: ''
    });
    setIsEditing(false);
    setShowModal(true);
  };

  const handleSaveStudent = async (e) => {
    e.preventDefault();

    if (studentForm.dataNascimento) {
      const birthDate = new Date(studentForm.dataNascimento);
      let age = today.getFullYear() - birthDate.getFullYear();
      const m = today.getMonth() - birthDate.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }

      if (age < 14) {
        setMessage({ type: 'error', text: 'Erro de validação: O aluno deve ter no mínimo 14 anos de idade.' });
        return;
      }
      if (age > 100) {
        setMessage({ type: 'error', text: 'Erro de validação: A data de nascimento informada excede 100 anos.' });
        return;
      }
    }

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
    <div className="card students-card">
      <div className="card-header">
        <div>
          <h2>Gestão de Alunos (aluno-service)</h2>
          <span style={{ fontSize: '13px', color: '#64748b' }}>
            Microsserviço responsável pelo cadastro, edição e eventos de alunos
          </span>
        </div>
        <button className="btn-primary" onClick={openNewModal}>
          + Cadastrar Novo Aluno
        </button>
      </div>

      {message.text && (
        <div className={`alert alert-${message.type}`} style={{ margin: '16px 0' }}>
          {message.text}
        </div>
      )}

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
                      <button
                        className="btn-success"
                        style={{ backgroundColor: '#0284c7', color: '#fff', border: 'none', padding: '6px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '12px' }}
                        onClick={() => openStudentDetails(student)}
                      >
                        Detalhes / Turmas
                      </button>
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

      {/* Modal de Detalhes / Turmas do Aluno */}
      {showDetailsModal && selectedStudent && (
        <div className="modal-overlay" onClick={() => setShowDetailsModal(false)}>
          <div className="modal-content" style={{ maxWidth: '600px' }} onClick={(e) => e.stopPropagation()}>
            <h3>Detalhes do Aluno: {selectedStudent.nome}</h3>
            <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '16px' }}>
              Matrícula: <code>{selectedStudent.matricula}</code> | Contato: {selectedStudent.contato || '-'}
            </p>

            <h4 style={{ marginBottom: '10px', color: '#0f172a' }}>Turmas em que o aluno está matriculado:</h4>
            <div className="table-responsive" style={{ maxHeight: '250px', overflowY: 'auto' }}>
              <table className="attendance-table">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Disciplina</th>
                    <th>Semestre</th>
                    <th>Horário</th>
                  </tr>
                </thead>
                <tbody>
                  {studentTurmas.length === 0 ? (
                    <tr>
                      <td colSpan="4" className="text-center">Este aluno não está matriculado em nenhuma turma.</td>
                    </tr>
                  ) : (
                    studentTurmas.map(t => (
                      <tr key={t.id}>
                        <td><code>{t.codigo}</code></td>
                        <td className="student-name">{t.nomeDisciplina}</td>
                        <td>{t.semestre}</td>
                        <td>{t.horario || '-'}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="modal-actions" style={{ marginTop: '20px' }}>
              <button type="button" className="btn-secondary" onClick={() => setShowDetailsModal(false)}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Cadastro / Edição */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3>{isEditing ? 'Editar Aluno' : 'Cadastrar Novo Aluno'}</h3>
            <form onSubmit={handleSaveStudent}>
              <div className="form-group">
                <label>Matrícula (Gerada Dinamicamente):</label>
                <input
                  type="text"
                  required
                  readOnly={!isEditing}
                  value={studentForm.matricula}
                  onChange={(e) => setStudentForm({ ...studentForm, matricula: e.target.value })}
                  style={{ backgroundColor: isEditing ? '#fff' : '#f1f5f9', cursor: isEditing ? 'text' : 'not-allowed' }}
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
                <label>Data de Nascimento (Idade entre 14 e 100 anos):</label>
                <input
                  type="date"
                  required
                  min={minDateStr}
                  max={maxDateStr}
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
                <label>Contato (Formato: (85) 98888-8888):</label>
                <input
                  type="text"
                  required
                  maxLength={15}
                  value={studentForm.contato || ''}
                  onChange={(e) => setStudentForm({ ...studentForm, contato: formatPhone(e.target.value) })}
                  placeholder="(85) 98888-8888"
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
  );
}
