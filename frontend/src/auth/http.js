export const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
export const SESSION_KEY = 'distrischool.session';
export class HttpError extends Error { constructor(status,message){super(message);this.status=status;} }
export async function apiFetch(url,options={}) {
 let session;try{session=JSON.parse(localStorage.getItem(SESSION_KEY));}catch{session=null;}
 const headers=new Headers(options.headers);
 if(session?.token)headers.set('Authorization', 'Bearer '+session.token);
 const response=await fetch(url,{...options,headers});
 if(!response.ok){
  let message;try{const data=await response.clone().json();message=data.message||data.error;}catch{/* Respostas sem JSON usam a mensagem padrão. */}
  const defaults={401:'Sessão expirada ou credenciais inválidas.',403:'Seu perfil não possui permissão para esta ação.',503:'Serviço temporariamente indisponível. Tente novamente em instantes.'};
  const error=new HttpError(response.status,message||defaults[response.status]||'Não foi possível concluir a solicitação.');
  if([401,403,503].includes(response.status))window.dispatchEvent(new CustomEvent('api-error',{detail:{status:response.status,message:error.message,login:url.endsWith('/auth/login')}}));
  throw error;
 }
 return response;
}
export async function api(path,options={}){const response=await apiFetch(API_BASE+path,{...options,headers:{'Content-Type':'application/json',...options.headers}});return response.status===204?null:response.json();}
