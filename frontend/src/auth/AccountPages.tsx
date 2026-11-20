import { useState, type FormEvent } from 'react';
import { API_BASE } from './http';
import './auth.css';

const flows = {
  register: {
    title: 'Criar conta',
    instructions: 'Cadastre seu e-mail para ativar sua conta de aluno.',
    endpoint: 'register',
    action: 'Criar conta',
  },
  verify: {
    title: 'Verificar e-mail',
    instructions: 'Confirme o endereço de e-mail cadastrado para poder entrar.',
    endpoint: 'verify-email',
    action: 'Verificar e-mail',
  },
  resend: {
    title: 'Reenviar link de verificação',
    instructions: 'Informe o e-mail usado no cadastro para pedir um novo link.',
    endpoint: 'verify-email/resend',
    action: 'Pedir novo link',
  },
  requestReset: {
    title: 'Recuperar senha',
    instructions: 'Informe o e-mail da sua conta para pedir um link de redefinição.',
    endpoint: 'password-reset/request',
    action: 'Enviar link',
  },
  reset: {
    title: 'Definir nova senha',
    instructions: 'Depois de salvar, entre com a nova senha.',
    endpoint: 'password-reset/confirm',
    action: 'Salvar nova senha',
  },
};

export default function AccountPages({ pathname }: { pathname: string }) {
  const [token] = useState(() => new URLSearchParams(location.search).get('token'));
  const mode = pathname === '/register' ? 'register' : pathname === '/verify-email'
    ? (token ? 'verify' : 'resend') : (token ? 'reset' : 'requestReset');
  const { title, instructions, endpoint, action } = flows[mode];
  const hasEmail = mode === 'register' || mode === 'resend' || mode === 'requestReset';
  const hasPassword = mode === 'register' || mode === 'reset';
  const [pending, setPending] = useState(false);
  const [sentTo, setSentTo] = useState('');
  const [error, setError] = useState('');
  const [complete, setComplete] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;
    const fields = new FormData(event.currentTarget);
    const email = String(fields.get('email') ?? '').trim();
    const password = String(fields.get('password') ?? '');
    setError('');

    if (hasPassword && !password.trim()) {
      setError('Digite uma senha com pelo menos oito caracteres.');
      return;
    }
    if (hasPassword && new TextEncoder().encode(password).length > 72) {
      setError('A senha é longa demais. Use uma senha com até 72 bytes.');
      return;
    }
    if (mode === 'reset' && password !== fields.get('confirmPassword')) {
      setError('As senhas não coincidem. Confira a confirmação e tente novamente.');
      return;
    }

    const payload = {
      ...(hasEmail && { email }),
      ...(hasPassword && { password }),
      ...(mode === 'register' && { name: String(fields.get('name') ?? '').trim() }),
      ...(token && mode !== 'register' && { token }),
    };
    setPending(true);
    try {
      const response = await fetch(API_BASE + '/auth/' + endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        const detail = await response.json().catch(() => null);
        if (response.status === 400 && (mode === 'verify' || mode === 'reset')
          && (typeof detail?.message !== 'string' || /^token\b/i.test(detail.message))) {
          setError('O link pode ter expirado ou já ter sido usado. Use o link mais recente ou solicite outro abaixo.');
        } else if (response.status === 409 && mode === 'register') {
          setError('Este e-mail já está cadastrado. Você pode pedir outro link de verificação ou recuperar a senha.');
        } else {
          setError(response.status === 400 && typeof detail?.message === 'string'
            ? detail.message : 'Não foi possível concluir a solicitação agora. Tente novamente em instantes.');
        }
        return;
      }

      setSentTo(email);
      setComplete(true);
      if (token) history.replaceState(null, '', pathname);
    } catch {
      setError('Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.');
    } finally {
      setPending(false);
    }
  }

  return <main className="card account-page">
    <h2>{title}</h2>
    {complete ? <div role="status" className="account-result">
      {hasEmail ? <>
        <strong>Confira sua caixa de entrada</strong>
        <p>{mode === 'register'
          ? <>Seu cadastro foi criado. Procure o link de verificação em <strong>{sentTo}</strong>.</>
          : <>Se o endereço <strong>{sentTo}</strong> corresponder a uma conta elegível, enviaremos o link por e-mail.</>}</p>
        <p>Se o e-mail não chegar, confira o spam e aguarde um minuto antes de pedir outro link.</p>
      </> : <p>{mode === 'verify' ? 'E-mail verificado. Você já pode entrar.' : 'Senha redefinida. Entre com a nova senha.'}</p>}
    </div> : <>
      <p>{instructions}</p>
      <form onSubmit={submit} aria-busy={pending}>
        {mode === 'register' && <label>Nome <input name="name" autoComplete="name" required maxLength={255} /></label>}
        {hasEmail && <label>E-mail <input name="email" type="email" autoComplete="email" required maxLength={255} /></label>}
        {hasPassword && <>
          <label>{mode === 'reset' ? 'Nova senha' : 'Senha'}
            <input name="password" type={showPassword ? 'text' : 'password'} autoComplete="new-password"
              minLength={8} maxLength={72} required aria-describedby="account-password-hint" />
          </label>
          <small id="account-password-hint">Use pelo menos oito caracteres.</small>
          {mode === 'reset' && <label>Confirme a nova senha
            <input name="confirmPassword" type={showPassword ? 'text' : 'password'} autoComplete="new-password" required />
          </label>}
          <label className="account-show-password"><input type="checkbox" checked={showPassword}
            onChange={event => setShowPassword(event.target.checked)} /> Mostrar senha</label>
        </>}
        {error && <p className="account-error" role="alert">{error}</p>}
        <button className="btn-primary" disabled={pending}>{pending ? 'Aguarde…' : action}</button>
      </form>
    </>}
    <div className="account-links">
      <a href="/">Voltar para o login</a>
      {(mode === 'register' || mode === 'verify' || (mode === 'resend' && complete))
        && <a href="/verify-email">Reenviar link de verificação</a>}
      {(mode === 'reset' || (mode === 'requestReset' && complete) || (mode === 'register' && Boolean(error)))
        && <a href="/password-reset">Pedir novo link de senha</a>}
    </div>
  </main>;
}
