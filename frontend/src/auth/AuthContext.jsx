import {createContext,useContext,useEffect,useState} from 'react';
import {api,SESSION_KEY} from './http';
const AuthContext=createContext(null);
function readSession(){try{const value=JSON.parse(localStorage.getItem(SESSION_KEY));const claims=JSON.parse(atob(value.token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/')));if(claims.exp*1000>Date.now()&&['ROLE_ADMIN','ROLE_PROFESSOR','ROLE_ALUNO'].includes(value.user.role))return value;}catch{/* Sessão ausente ou inválida. */}localStorage.removeItem(SESSION_KEY);return null;}
export function AuthProvider({children}){
 const [session,setSession]=useState(readSession),[notice,setNotice]=useState('');
 const logout=()=>{localStorage.removeItem(SESSION_KEY);setSession(null);};
 useEffect(()=>{
  const sync=()=>setSession(readSession());
  const onError=event=>{setNotice(event.detail.message);if(event.detail.status===401&&!event.detail.login)logout();};
  window.addEventListener('storage',sync);window.addEventListener('api-error',onError);
  return()=>{window.removeEventListener('storage',sync);window.removeEventListener('api-error',onError);};
 },[]);
 useEffect(()=>{if(!session)return;const claims=JSON.parse(atob(session.token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/')));const timer=setTimeout(()=>{logout();setNotice('Sua sessão expirou. Entre novamente.');},Math.max(0,claims.exp*1000-Date.now()));return()=>clearTimeout(timer);},[session]);
 const login=async(email,password)=>{const next=await api('/auth/login',{method:'POST',body:JSON.stringify({email,password})});localStorage.setItem(SESSION_KEY,JSON.stringify(next));setSession(next);setNotice('');};
 return <AuthContext.Provider value={{user:session?.user,login,logout,notice,setNotice}}>{children}</AuthContext.Provider>;
}
export function useAuth(){return useContext(AuthContext);}
