import React, { useState } from 'react';
import './App.css';
import AttendanceComponent from './components/AttendanceComponent';
import AlunoComponent from './components/AlunoComponent';
import ProfessorComponent from './components/ProfessorComponent';
import TurmaComponent from './components/TurmaComponent';
import GradeComponent from './components/GradeComponent';

export default function App() {
  const [activeTab, setActiveTab] = useState('chamada');

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
            Chamada & Frequência
          </button>
          <button
            className={activeTab === 'alunos' ? 'active' : ''}
            onClick={() => setActiveTab('alunos')}
          >
            Gestão de Alunos
          </button>
          <button
            className={activeTab === 'professores' ? 'active' : ''}
            onClick={() => setActiveTab('professores')}
          >
            Gestão de Professores
          </button>
          <button
            className={activeTab === 'turmas' ? 'active' : ''}
            onClick={() => setActiveTab('turmas')}
          >
            Turmas & Horários
          </button>
          <button
            className={activeTab === 'notas' ? 'active' : ''}
            onClick={() => setActiveTab('notas')}
          >
            Notas & Rendimento
          </button>
        </nav>
      </header>

      <main className="app-main">
        {activeTab === 'chamada' && <AttendanceComponent />}
        {activeTab === 'alunos' && <AlunoComponent />}
        {activeTab === 'professores' && <ProfessorComponent />}
        {activeTab === 'turmas' && <TurmaComponent />}
        {activeTab === 'notas' && <GradeComponent />}
      </main>
    </div>
  );
}
