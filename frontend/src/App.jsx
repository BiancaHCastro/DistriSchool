import Notification from './components/Notification';
import {useState} from 'react';
import './App.css';
import {useAuth} from './auth/AuthContext';
import AuthHeader from './auth/AuthHeader';
import AttendanceComponent from './components/AttendanceComponent';
import AlunoComponent from './components/AlunoComponent';
import ProfessorComponent from './components/ProfessorComponent';
import TurmaComponent from './components/TurmaComponent';
import GradeComponent from './components/GradeComponent';
import {StudentArea,Profile,AdminRecords,EnrollmentRequests,TeacherClasses} from './components/AcademicAreas';
export default function App(){
 const {user,notice,setNotice}=useAuth();const [tab,setTab]=useState('');
 const role=user?.role;
 const areas={ROLE_ADMIN:{title:'Área Administrativa',tabs:{alunos:['Alunos',AlunoComponent],professores:['Professores',ProfessorComponent],turmas:['Turmas',TurmaComponent],usuarios:['Usuários',AdminRecords],matriculas:['Matrículas',EnrollmentRequests],notas:['Notas',GradeComponent],frequencia:['Frequência e relatórios',AttendanceComponent]}},ROLE_PROFESSOR:{title:'Área Docente',tabs:{turmas:['Minhas turmas',TeacherClasses],perfil:['Meu perfil',Profile],notas:['Notas',GradeComponent],frequencia:['Frequência',AttendanceComponent]}},ROLE_ALUNO:{title:'Área do Aluno',tabs:{academico:['Vida acadêmica',StudentArea]}}};
 const area=areas[role];const current=area?(area.tabs[tab]?tab:Object.keys(area.tabs)[0]):null;const Content=current?area.tabs[current][1]:null;
 return <div className="distrischool-container"><AuthHeader/><Notification text={notice} onClose={()=>setNotice('')}/>{!user?<main className="card"><h2>Portal universitário</h2><p>Entre para acessar sua área acadêmica. Para obter uma conta ou trocar sua senha, procure a administração.</p></main>:<main key={user.id+role}><h2>{area.title}</h2><nav className="header-nav" aria-label="Navegação da área">{Object.entries(area.tabs).map(([id,[label]])=><button key={id} className={current===id?'active':''} onClick={()=>setTab(id)}>{label}</button>)}</nav><section className="area-content"><Content key={current}/></section></main>}</div>;
}
