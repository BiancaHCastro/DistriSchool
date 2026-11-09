import React, { useState, useEffect } from 'react';

const API_BASE = 'http://localhost:8080/api';

export default function ProfessorComponent() {
  const [professors, setProfessors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  // Professor CRUD modal & form state
  const [professorForm, setProfessorForm] = useState({ id: null, nome: '', email: '', telefone: '', especialidade: '' });
  const [isEditingProfessor, setIsEditingProfessor] = useState(false);
  const [showProfessorModal, setShowProfessorModal] = useState(false);

  useEffect(() => {
    fetchProfessors();
  }, []);

  const fetchProfessors = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/teachers`);
      if (!res.ok) throw new Error(`Erro HTTP ao buscar professores: ${res.status}`);
      const data = await res.json();
      setProfessors(data);
      setMessage({ type: '', text: '' });
    } catch (err) {
      console.error('Erro ao conectar com professor-service:', err);
      setMessage({ type: 'error', text: 'Não foi possível conectar ao professor-service. Verifique se o Docker está rodando.' });
      setProfessors([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProfessor = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const url = isEditingProfessor ? `${API_BASE}/teachers/${professorForm.id}` : `${API_BASE}/teachers`;
      const method = isEditingProfessor ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(professorForm)
      });

      if (!res.ok) {
        const errText = await res.text();
        throw new Error(errText || 'Erro ao salvar professor no servidor');
      }

      setMessage({ type: 'success', text: isEditingProfessor ? 'Professor atualizado com sucesso!' : 'Professor cadastrado com sucesso!' });
      setShowProfessorModal(false);
      setProfessorForm({ id: null, nome: '', email: '', telefone: '', especialidade: '' });
      setIsEditingProfessor(false);
      fetchProfessors();
    } catch (err) {
      setMessage({ type: 'error', text: `Erro no CRUD de Professores: ${err.message}` });
    } finally {
      setLoading(false);
    }
  };

  const handleEditProfessor = (professor) => {
    setProfessorForm({ ...professor });
    setIsEditingProfessor(true);
    setShowProfessorModal(true);
  };

  const handleDeleteProfessor = async (id) => {
    if (!window.confirm('Deseja realmente excluir este professor?')) return;
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/teachers/${id}`, { method: 'DELETE' });
      if (!res.ok) throw new Error('Erro ao excluir professor');
      setMessage({ type: 'success', text: 'Professor excluído com sucesso!' });
      fetchProfessors();
    } catch (err) {
      setMessage({ type: 'error', text: `Erro ao excluir professor: ${err.message}` });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card students-card">
      <div className="card-header">
        <h2>Gerenciamento de Professores (professor-service)</h2>
        <button
          className="btn-primary"
          onClick={() => {
            setProfessorForm({ id: null, nome: '', email: '', telefone: '', especialidade: '' });
            setIsEditingProfessor(false);
            setShowProfessorModal(true);
          }}
        >
          + Cadastrar Novo Professor
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
              <th>Nome</th>
              <th>E-mail</th>
              <th>Telefone</th>
              <th>Especialidade</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {professors.length === 0 ? (
              <tr>
                <td colSpan="6" className="text-center">Nenhum professor encontrado no banco de dados.</td>
              </tr>
            ) : (
              professors.map(prof => (
                <tr key={prof.id}>
                  <td>{prof.id}</td>
                  <td className="student-name">{prof.nome}</td>
                  <td><code>{prof.email}</code></td>
                  <td>{prof.telefone || '-'}</td>
                  <td>{prof.especialidade || '-'}</td>
                  <td>
                    <div className="action-buttons">
                      <button className="btn-secondary" onClick={() => handleEditProfessor(prof)}>Editar</button>
                      <button className="btn-danger" onClick={() => handleDeleteProfessor(prof.id)}>Excluir</button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showProfessorModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>{isEditingProfessor ? 'Editar Professor' : 'Cadastrar Novo Professor'}</h3>
            <form onSubmit={handleSaveProfessor}>
              <div className="form-group">
                <label>Nome Completo:</label>
                <input
                  type="text"
                  required
                  value={professorForm.nome}
                  onChange={(e) => setProfessorForm({ ...professorForm, nome: e.target.value })}
                  placeholder="Ex: Carlos Alberto"
                />
              </div>
              <div className="form-group">
                <label>E-mail:</label>
                <input
                  type="email"
                  required
                  value={professorForm.email}
                  onChange={(e) => setProfessorForm({ ...professorForm, email: e.target.value })}
                  placeholder="Ex: carlos@escola.com"
                />
              </div>
              <div className="form-group">
                <label>Telefone:</label>
                <input
                  type="text"
                  value={professorForm.telefone || ''}
                  onChange={(e) => setProfessorForm({ ...professorForm, telefone: e.target.value })}
                  placeholder="Ex: (85) 98888-7777"
                />
              </div>
              <div className="form-group">
                <label>Especialidade / Matéria:</label>
                <input
                  type="text"
                  value={professorForm.especialidade || ''}
                  onChange={(e) => setProfessorForm({ ...professorForm, especialidade: e.target.value })}
                  placeholder="Ex: Matemática"
                />
              </div>
              <div className="modal-actions">
                <button type="submit" className="btn-primary" disabled={loading}>Salvar</button>
                <button type="button" className="btn-secondary" onClick={() => setShowProfessorModal(false)}>Cancelar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
