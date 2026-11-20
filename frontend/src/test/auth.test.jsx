import {describe,it,expect,vi,beforeEach} from 'vitest';
import {render,screen,waitFor,within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from '../App';
import {AuthProvider} from '../auth/AuthContext';
import {apiFetch,SESSION_KEY} from '../auth/http';
import {readFileSync} from 'node:fs';
const styles=readFileSync(process.cwd()+'/src/auth/auth.css','utf8');
vi.mock('../components/AlunoComponent',()=>({default:()=> <div>Cadastro de alunos</div>}));
vi.mock('../components/ProfessorComponent',()=>({default:()=> <div>Cadastro de docentes</div>}));
vi.mock('../components/TurmaComponent',()=>({default:()=> <div>Cadastro de turmas</div>}));
vi.mock('../components/AttendanceComponent',()=>({default:()=> <div>Controle de frequência</div>}));
vi.mock('../components/GradeComponent',()=>({default:()=> <div>Controle de notas</div>}));
vi.mock('../components/AcademicAreas',()=>({StudentArea:()=> <div>Vida acadêmica do aluno</div>,Profile:()=> <div>Perfil docente</div>,TeacherClasses:()=> <div>Turmas do docente</div>,AdminRecords:()=> <div>Administração</div>,EnrollmentRequests:()=> <div>Solicitações</div>}));
function session(role='ROLE_ALUNO'){return {token:'header.'+btoa(JSON.stringify({exp:Math.floor(Date.now()/1000)+3600}))+'.signature',user:{id:1,name:'Ana',email:'ana@universidade.edu',role,academicId:12}};}
function mount(){return render(<AuthProvider><App/></AuthProvider>);}
beforeEach(()=>{vi.stubGlobal('fetch',vi.fn());});
describe('Autenticação global',()=>{
 it('abre modal pelo botão no topo direito e permite fechar com Escape',async()=>{const user=userEvent.setup();mount();const button=screen.getByRole('button',{name:'Entrar'});expect(button.closest('header')).toHaveClass('global-header');expect(button.parentElement).toHaveClass('auth-actions');expect(styles).toMatch(/.auth-actions\{margin-left:auto/);await user.click(button);expect(screen.getByRole('dialog')).toHaveAttribute('aria-modal','true');expect(screen.getByLabelText('E-mail')).toHaveFocus();await user.keyboard('{Escape}');expect(screen.queryByRole('dialog')).not.toBeInTheDocument();expect(button).toHaveFocus();});
 it('faz login, persiste a sessão e restaura ao remontar',async()=>{const user=userEvent.setup();fetch.mockResolvedValue(new Response(JSON.stringify(session()),{status:200,headers:{'Content-Type':'application/json'}}));let view=mount();await user.click(screen.getByRole('button',{name:'Entrar'}));await user.type(screen.getByLabelText('E-mail'),'ana@universidade.edu');await user.type(screen.getByLabelText('Senha'),'Universidade123!');await user.click(within(screen.getByRole('dialog')).getByRole('button',{name:'Entrar'}));expect(await screen.findByText('Ana · Aluno')).toBeInTheDocument();expect(JSON.parse(localStorage.getItem(SESSION_KEY)).user.name).toBe('Ana');view.unmount();mount();expect(screen.getByText('Ana · Aluno')).toBeInTheDocument();expect(screen.queryByRole('dialog')).not.toBeInTheDocument();});
 for(const [role,title]of [['ROLE_ALUNO','Área do Aluno'],['ROLE_PROFESSOR','Área Docente'],['ROLE_ADMIN','Área Administrativa']])it('mostra somente a área de '+role,()=>{localStorage.setItem(SESSION_KEY,JSON.stringify(session(role)));mount();expect(screen.getByRole('heading',{name:title})).toBeInTheDocument();for(const other of ['Área do Aluno','Área Docente','Área Administrativa'].filter(t=>t!==title))expect(screen.queryByRole('heading',{name:other})).not.toBeInTheDocument();if(role==='ROLE_ALUNO')expect(screen.queryByRole('button',{name:'Usuários'})).not.toBeInTheDocument();});
 it('sai e limpa o estado persistido',async()=>{localStorage.setItem(SESSION_KEY,JSON.stringify(session()));mount();await userEvent.click(screen.getByRole('button',{name:'Sair'}));expect(localStorage.getItem(SESSION_KEY)).toBeNull();expect(screen.getByRole('button',{name:'Entrar'})).toBeInTheDocument();});
 it('descarta uma sessão expirada',()=>{const expired=session();expired.token='x.'+btoa(JSON.stringify({exp:1}))+'.x';localStorage.setItem(SESSION_KEY,JSON.stringify(expired));mount();expect(screen.getByRole('button',{name:'Entrar'})).toBeInTheDocument();});
 it('sincroniza logout entre abas',async()=>{localStorage.setItem(SESSION_KEY,JSON.stringify(session()));mount();localStorage.removeItem(SESSION_KEY);window.dispatchEvent(new StorageEvent('storage',{key:SESSION_KEY}));await waitFor(()=>expect(screen.getByRole('button',{name:'Entrar'})).toBeInTheDocument());});
 it('mantém o modal e informa erro em login inválido',async()=>{fetch.mockResolvedValue(new Response(JSON.stringify({message:'E-mail ou senha inválidos'}),{status:401}));mount();const user=userEvent.setup();await user.click(screen.getByRole('button',{name:'Entrar'}));await user.type(screen.getByLabelText('E-mail'),'ana@universidade.edu');await user.type(screen.getByLabelText('Senha'),'errada');await user.click(within(screen.getByRole('dialog')).getByRole('button',{name:'Entrar'}));expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos');expect(screen.getByRole('dialog')).not.toContainElement(screen.getByRole('alert'));expect(screen.getByRole('dialog')).toBeInTheDocument();});
});
describe('Cliente HTTP',()=>{
 it('injeta o Bearer token',async()=>{const data=session();localStorage.setItem(SESSION_KEY,JSON.stringify(data));fetch.mockResolvedValue(new Response('[]'));await apiFetch('http://localhost:8080/api/students');expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer '+data.token);});
 for(const status of [401,403,503])it('trata '+status+' com mensagem e mantém a semântica da sessão',async()=>{localStorage.setItem(SESSION_KEY,JSON.stringify(session()));mount();fetch.mockResolvedValue(new Response('{}',{status}));await expect(apiFetch('http://localhost:8080/api/students')).rejects.toMatchObject({status});await waitFor(()=>expect(screen.getByRole('alert')).toBeInTheDocument());expect(Boolean(localStorage.getItem(SESSION_KEY))).toBe(status!==401);});
});
