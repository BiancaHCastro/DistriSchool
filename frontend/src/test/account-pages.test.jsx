import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AccountPages from '../auth/AccountPages';

afterEach(() => {
  vi.unstubAllGlobals();
  history.replaceState(null, '', '/');
});

describe('Fluxos públicos da conta', () => {
  it('envia o cadastro pelo gateway e mostra a instrução de verificação', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true });
    vi.stubGlobal('fetch', fetch);
    const user = userEvent.setup();
    render(<AccountPages pathname="/register" />);
    await user.type(screen.getByLabelText('Nome'), 'Aluno');
    await user.type(screen.getByLabelText('E-mail'), 'aluno@example.org');
    await user.type(screen.getByLabelText('Senha'), 'exemplo123');
    await user.click(screen.getByRole('button', { name: 'Criar conta' }));

    expect(await screen.findByRole('status')).toHaveTextContent('link de verificação');
    const [url, request] = fetch.mock.calls[0];
    expect(url.endsWith('/api/auth/register')).toBe(true);
    expect(JSON.parse(request.body)).toEqual({
      name: 'Aluno', email: 'aluno@example.org', password: 'exemplo123',
    });
  });

  it('verifica o token do link e limpa a URL', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true });
    vi.stubGlobal('fetch', fetch);
    history.replaceState(null, '', '/verify-email?token=teste');
    render(<AccountPages pathname="/verify-email" />);
    await userEvent.click(screen.getByRole('button', { name: 'Verificar e-mail' }));

    expect(await screen.findByRole('status')).toHaveTextContent('E-mail verificado');
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ token: 'teste' });
    expect(location.search).toBe('');
  });

  it.each([
    ['/password-reset', '/password-reset/request', 'Enviar link'],
    ['/verify-email', '/verify-email/resend', 'Pedir novo link'],
  ])('confirma a solicitação de %s sem informar se a conta existe', async (path, endpoint, action) => {
    const fetch = vi.fn().mockResolvedValue({ ok: true });
    vi.stubGlobal('fetch', fetch);
    const user = userEvent.setup();
    render(<AccountPages pathname={path} />);
    await user.type(screen.getByLabelText('E-mail'), 'aluno@example.org');
    await user.click(screen.getByRole('button', { name: action }));

    expect(await screen.findByRole('status')).toHaveTextContent('Se o endereço');
    expect(fetch.mock.calls[0][0]).toContain('/api/auth' + endpoint);
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ email: 'aluno@example.org' });
  });

  it('valida a confirmação da senha e envia somente a senha nova e o token', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true });
    vi.stubGlobal('fetch', fetch);
    history.replaceState(null, '', '/password-reset?token=teste');
    const user = userEvent.setup();
    render(<AccountPages pathname="/password-reset" />);
    await user.type(screen.getByLabelText('Nova senha', { exact: true }), 'novaSenha123');
    const confirmation = screen.getByLabelText('Confirme a nova senha');
    await user.type(confirmation, 'outraSenha123');
    const submit = screen.getByRole('button', { name: 'Salvar nova senha' });
    await user.click(submit);
    expect(screen.getByRole('alert')).toHaveTextContent('As senhas não coincidem');
    expect(fetch).not.toHaveBeenCalled();

    await user.clear(confirmation);
    await user.type(confirmation, 'novaSenha123');
    await user.click(submit);
    expect(await screen.findByRole('status')).toHaveTextContent('Senha redefinida');
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ token: 'teste', password: 'novaSenha123' });
    expect(location.search).toBe('');
  });

  it('mostra como renovar um link de verificação expirado', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 400 })));
    history.replaceState(null, '', '/verify-email?token=expirado');
    render(<AccountPages pathname="/verify-email" />);
    await userEvent.click(screen.getByRole('button', { name: 'Verificar e-mail' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('expirado');
    expect(screen.getByRole('link', { name: 'Reenviar link de verificação' })).toHaveAttribute('href', '/verify-email');
  });
});
