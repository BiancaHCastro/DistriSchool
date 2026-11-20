import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, expect, it, vi } from 'vitest';
import { api } from '../auth/http';
import { AdminRecords } from '../components/AcademicAreas';

vi.mock('../auth/http', () => ({ api: vi.fn() }));

beforeEach(() => {
  api.mockReset();
  api.mockImplementation(async (path, options) => {
    if (options?.method === 'POST') return { id: 9, ...JSON.parse(options.body), password: undefined };
    if (path === '/students') return [{ id: 42, nome: 'Ana Silva', matricula: '2026001' }];
    if (path === '/teachers') return [{ id: 12, nome: 'Eva Souza', email: 'eva@example.org' }];
    return [];
  });
});

it('cria uma conta de aluno com o ID do cadastro escolhido', async () => {
  const user = userEvent.setup();
  render(<AdminRecords />);
  await user.type(screen.getByLabelText('Nome'), 'Ana Silva');
  await user.type(screen.getByLabelText('E-mail'), 'ana@example.org');
  await user.type(screen.getByLabelText('Senha'), 'senha-segura');
  await screen.findByRole('option', { name: 'Ana Silva · 2026001' });
  await user.selectOptions(screen.getByLabelText('ID do cadastro acadêmico'), '42');
  await user.click(screen.getByRole('button', { name: 'Criar usuário' }));

  await waitFor(() => expect(api).toHaveBeenCalledWith('/users', {
    method: 'POST',
    body: JSON.stringify({
      email: 'ana@example.org', name: 'Ana Silva', password: 'senha-segura', role: 'ROLE_ALUNO', academicId: 42,
    }),
  }));
  expect(await screen.findByRole('status')).toHaveTextContent('Conta criada');
  const row = screen.getByRole('row', { name: /ana@example.org/ });
  expect(within(row).getByRole('cell', { name: '42' })).toBeInTheDocument();
  expect(screen.getByLabelText('Senha')).toHaveValue('');
});

it('limpa o vínculo ao trocar de perfil e lista somente cadastros do novo perfil', async () => {
  const user = userEvent.setup();
  render(<AdminRecords />);
  const link = screen.getByLabelText('ID do cadastro acadêmico');
  await screen.findByRole('option', { name: 'Ana Silva · 2026001' });
  await user.selectOptions(link, '42');
  await user.selectOptions(screen.getByLabelText('Perfil'), 'ROLE_PROFESSOR');
  expect(link).toHaveValue('');
  expect(await screen.findByRole('option', { name: /Eva Souza/ })).toBeInTheDocument();
  expect(screen.queryByRole('option', { name: /Ana Silva/ })).not.toBeInTheDocument();
});
