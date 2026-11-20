import Notification from './Notification';
import AcademicSituation from './AcademicSituation';
import {useEffect,useState} from 'react';
import {api} from '../auth/http';
import {useAuth} from '../auth/AuthContext';
import {roleLabels} from '../auth/AuthHeader';
function Table({rows,fields,actions}){return <div className="table-responsive"><table className="academic-table"><thead><tr>{fields.map(([key,label])=><th key={key}>{label}</th>)}{actions&&<th>Ações</th>}</tr></thead><tbody>{rows.map((row,index)=><tr key={row.id??index}>{fields.map(([key])=><td key={key}>{row[key]??'—'}</td>)}{actions&&<td>{actions(row)}</td>}</tr>)}{!rows.length&&<tr><td colSpan={fields.length+(actions?1:0)}>Nenhum registro disponível.</td></tr>}</tbody></table></div>;}
function ErrorMessage({error}){return <Notification text={error}/>;}
export function Profile(){const {user}=useAuth();const [profile,setProfile]=useState(null),[error,setError]=useState(''),[message,setMessage]=useState('');
 useEffect(()=>{if(user.academicId)api('/teachers/'+user.academicId).then(setProfile).catch(e=>setError(e.message));},[user.academicId]);
 async function save(event){event.preventDefault();setError('');try{setProfile(await api('/teachers/'+user.academicId,{method:'PUT',body:JSON.stringify(profile)}));setMessage('Perfil atualizado.');}catch(e){setError(e.message);}}
 return <section className="card"><h3>Meu perfil docente</h3><ErrorMessage error={error}/>{message&&<p role="status">{message}</p>}{profile?<form className="record-form" onSubmit={save}>{['nome','email','telefone','especialidade'].map(key=><label key={key}>{key}<input required={['nome','email'].includes(key)} type={key==='email'?'email':'text'} value={profile[key]||''} onChange={e=>setProfile({...profile,[key]:e.target.value})}/></label>)}<button>Salvar perfil</button></form>:<p>{user.academicId?'Carregando perfil…':'Solicite à administração o vínculo do seu usuário ao cadastro docente.'}</p>}</section>;
}
export function TeacherClasses(){const [turmas,setTurmas]=useState([]),[students,setStudents]=useState([]),[error,setError]=useState('');useEffect(()=>{api('/turmas').then(setTurmas).catch(e=>setError(e.message));api('/students').then(setStudents).catch(e=>setError(e.message));},[]);return <div className="academic-grid"><section className="card"><h3>Minhas turmas</h3><ErrorMessage error={error}/><Table rows={turmas} fields={[["codigo","Código"],["nomeDisciplina","Disciplina"],["semestre","Semestre"],["horario","Horário"]]}/></section><section className="card"><h3>Discentes das minhas turmas</h3><Table rows={students} fields={[["matricula","Matrícula"],["nome","Nome"]]}/></section></div>;}
export function StudentArea(){const {user}=useAuth();const [data,setData]=useState({students:[],turmas:[],teachers:[],matriculas:[],frequency:null}),[errors,setErrors]=useState([]),[message,setMessage]=useState('');
 const load=()=>{const paths={students:'/students',turmas:'/turmas',teachers:'/teachers/directory',matriculas:'/matriculas',frequency:'/academic/presencas/relatorio'};setErrors([]);Object.entries(paths).forEach(([key,url])=>api(url).then(value=>setData(previous=>({...previous,[key]:value}))).catch(e=>setErrors(previous=>[...previous,e.message])));};useEffect(load,[]);
 async function request(turmaId){try{await api('/matriculas',{method:'POST',body:JSON.stringify({turmaId})});setMessage('Solicitação de matrícula enviada.');load();}catch(e){setErrors([e.message]);}}
 const teachersById = new Map(data.teachers.map(teacher => [String(teacher.id), teacher.nome]));
 const turmas = data.turmas.map(turma => ({
  ...turma,
  professor: teachersById.get(String(turma.teacherId)) ?? (turma.teacherId ? 'Professor indisponível' : 'A definir'),
 }));
 const turmasById = new Map(turmas.map(turma => [String(turma.id), turma]));
 const requests = data.matriculas.map(request => ({ ...request, ...turmaDetails(turmasById.get(String(request.turmaId)), request.turmaId) }));
 const profile=data.students.find(s=>s.id===user.academicId);
 return <div className="academic-grid"><Notification text={[...new Set(errors)].join(" ")} onClose={()=>setErrors([])}/>{message&&<p role="status">{message}</p>}<section className="card"><h3>Meu perfil</h3>{profile?<p>{profile.nome} · Matrícula {profile.matricula} · {profile.contato}</p>:<p>{user.name} · {user.email}{!user.academicId&&' — Solicite à administração o vínculo ao seu cadastro de aluno.'}</p>}</section><section className="card"><h3>Turmas e solicitação de matrícula</h3><Table rows={turmas} fields={[["codigo","Código"],["nomeDisciplina","Disciplina"],["semestre","Semestre"],["horario","Horário"],["professor","Professor"]]} actions={t=><button disabled={!user.academicId||data.matriculas.some(m=>String(m.turmaId)===String(t.id))} onClick={()=>request(t.id)}>Solicitar matrícula</button>}/></section><section className="card"><h3>Minhas solicitações</h3><Table rows={requests} fields={enrollmentFields}/></section><AcademicSituation key={user.academicId} alunoId={user.academicId}/><section className="card"><h3>Minha frequência</h3><p>{data.frequency?.percentualPresenca!=null?'Presença: '+data.frequency.percentualPresenca.toFixed(1)+'%':'Sem dados de frequência.'}</p><Table rows={data.frequency?.historico||[]} fields={[["turmaId","Turma"],["data","Data"],["status","Frequência"]]}/></section></div>;
}
const enrollmentFields = [
 ["turma","Turma"], ["semestre","Semestre"], ["horario","Horário"],
 ["professor","Professor"], ["status","Situação"],
];

function turmaDetails(turma, id) {
 return {
  turma: turma ? [turma.codigo, turma.nomeDisciplina].filter(Boolean).join(' · ') : 'Turma #' + id,
  semestre: turma?.semestre,
  horario: turma?.horario,
  professor: turma?.professor,
 };
}

export function EnrollmentRequests() {
 const [rows, setRows] = useState([]);
 const [students, setStudents] = useState([]);
 const [turmas, setTurmas] = useState([]);
 const [teachers, setTeachers] = useState([]);
 const [error, setError] = useState('');
 const load = () => api('/matriculas').then(setRows).catch(failure => setError(failure.message));
 useEffect(() => {
  load();
  api('/students').then(setStudents).catch(failure => setError(failure.message));
  api('/turmas').then(setTurmas).catch(failure => setError(failure.message));
  api('/teachers').then(setTeachers).catch(failure => setError(failure.message));
 }, []);

 async function approve(id) {
  setError('');
  try {
   await api('/matriculas/' + id + '/aprovar', { method: 'PUT' });
   await load();
  } catch (failure) {
   setError(failure.message);
  }
 }

 const studentsById = new Map(students.map(student => [String(student.id), student]));
 const teachersById = new Map(teachers.map(teacher => [String(teacher.id), teacher.nome]));
 const turmasById = new Map(turmas.map(turma => [String(turma.id), {
  ...turma, professor: teachersById.get(String(turma.teacherId)),
 }]));
 const details = rows.map(row => {
  const student = studentsById.get(String(row.alunoId));
  return {
   ...row,
   aluno: student ? [student.nome, student.matricula].filter(Boolean).join(' · ') : 'Aluno #' + row.alunoId,
   ...turmaDetails(turmasById.get(String(row.turmaId)), row.turmaId),
  };
 });

 return <section className="card">
  <h3>Solicitações de matrícula</h3>
  <Notification text={error} onClose={() => setError('')}/>
  <Table rows={details} fields={[["aluno","Aluno"], ...enrollmentFields]} actions={row =>
   row.status === 'PENDENTE' && <button onClick={() => approve(row.id)}>Aprovar matrícula</button>
  }/>
 </section>;
}
const emptyUser = { email: '', name: '', password: '', role: 'ROLE_ALUNO', academicId: '' };
const userFields = [
 ["name", "Nome"], ["email", "E-mail"], ["roleLabel", "Perfil"], ["academicId", "ID do cadastro acadêmico"],
];

export function AdminRecords() {
 const [rows, setRows] = useState([]);
 const [directory, setDirectory] = useState(null);
 const [form, setForm] = useState(emptyUser);
 const [error, setError] = useState('');
 const [message, setMessage] = useState('');
 const [loading, setLoading] = useState(true);
 const [busy, setBusy] = useState(false);

 useEffect(() => {
  let active = true;
  api('/users').then(data => { if (active) setRows(data); })
   .catch(failure => { if (active) setError(failure.message); })
   .finally(() => { if (active) setLoading(false); });
  return () => { active = false; };
 }, []);

 useEffect(() => {
  const role = form.role;
  if (role === 'ROLE_ADMIN') return;
  let active = true;
  api(role === 'ROLE_PROFESSOR' ? '/teachers' : '/students')
   .then(profiles => { if (active) setDirectory({ role, profiles }); })
   .catch(failure => { if (active) setError(failure.message); });
  return () => { active = false; };
 }, [form.role]);

 const profiles = directory?.role === form.role ? directory.profiles : [];
 const linkedId = form.academicId ?? '';
 const hasLinkedProfile = profiles.some(profile => String(profile.id) === String(linkedId));
 const updateField = (key, value) => setForm(previous => ({ ...previous, [key]: value }));

 async function save(event) {
  event.preventDefault();
  setError('');
  setMessage('');
  const { id, email, name, password, role } = form;
  if (password && (password.trim().length === 0 || new TextEncoder().encode(password).length > 72)) {
   setError('Informe uma senha de pelo menos 8 caracteres e até 72 bytes.');
   return;
  }
  const academicId = role === 'ROLE_ADMIN' || linkedId === '' ? null : Number(linkedId);
  const payload = { email: email.trim(), name: name.trim(), password, role, academicId };
  setBusy(true);
  try {
   const saved = await api(id ? '/users/' + id : '/users', {
    method: id ? 'PUT' : 'POST',
    body: JSON.stringify(payload),
   });
   setRows(previous => id ? previous.map(row => row.id === id ? saved : row) : [...previous, saved]);
   setForm(emptyUser);
   setMessage(id ? 'Conta atualizada.' : 'Conta criada. O usuário já pode entrar com a senha definida.');
  } catch (failure) {
   setError(failure.status === 409 ? 'Este e-mail já está cadastrado.' : failure.message);
  } finally {
   setBusy(false);
  }
 }

 async function remove(id) {
  setError('');
  setMessage('');
  setBusy(true);
  try {
   await api('/users/' + id, { method: 'DELETE' });
   setRows(previous => previous.filter(row => row.id !== id));
   if (form.id === id) setForm(emptyUser);
   setMessage('Usuário excluído.');
  } catch (failure) {
   setError(failure.message);
  } finally {
   setBusy(false);
  }
 }

 return <section className="card">
  <h3>Usuários</h3>
  <Notification text={error} onClose={() => setError('')}/>
  {message && <p role="status">{message}</p>}
  <h4>{form.id ? 'Editar usuário' : 'Criar usuário'}</h4>
  <p>Para vincular um aluno ou professor, cadastre a pessoa na aba do perfil correspondente e selecione o cadastro abaixo. Após mudar o perfil ou vínculo, o usuário precisa entrar novamente.</p>
  <form className="record-form" onSubmit={save}>
   <label>Nome<input required maxLength={255} value={form.name} onChange={e => updateField('name', e.target.value)}/></label>
   <label>E-mail<input type="email" required maxLength={255} value={form.email} onChange={e => updateField('email', e.target.value)}/></label>
   <label>Perfil<select value={form.role} onChange={e => setForm({ ...form, role: e.target.value, academicId: '' })}>
    {Object.entries(roleLabels).map(([role, label]) => <option key={role} value={role}>{label}</option>)}
   </select></label>
   <label>ID do cadastro acadêmico
    <select disabled={form.role === 'ROLE_ADMIN'} value={linkedId} onChange={e => updateField('academicId', e.target.value)}>
     <option value="">Sem vínculo</option>
     {linkedId !== '' && !hasLinkedProfile && <option value={linkedId}>Cadastro #{linkedId}</option>}
     {profiles.map(profile => <option key={profile.id} value={profile.id}>
      {profile.nome}{profile.matricula ? ' · ' + profile.matricula : ' · ' + profile.email}
     </option>)}
    </select>
   </label>
   <label>{form.id ? 'Nova senha (opcional)' : 'Senha'}
    <input type="password" minLength={8} autoComplete="new-password" required={!form.id}
     value={form.password} onChange={e => updateField('password', e.target.value)}/>
   </label>
   <button disabled={busy || loading}>{form.id ? 'Salvar alterações' : 'Criar usuário'}</button>
   {form.id && <button disabled={busy} type="button" onClick={() => setForm(emptyUser)}>Cancelar edição</button>}
  </form>
  <Table rows={rows.map(row => ({ ...row, roleLabel: roleLabels[row.role] }))} fields={userFields} actions={row => <>
   <button disabled={busy} onClick={() => { setMessage(''); setForm({ ...row, password: '' }); }}>Editar</button>
   <button disabled={busy} onClick={() => remove(row.id)}>Excluir</button>
  </>}/>
 </section>;
}
