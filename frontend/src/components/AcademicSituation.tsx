import Notification from './Notification';
import { useEffect, useState } from 'react';
import { api } from '../auth/http';

type Status = 'APROVADO' | 'EM_AVF' | 'REPROVADO' | 'PENDENTE';
type Situation = {
  turmaId: string;
  nota1: number | null;
  nota2: number | null;
  notaFinal: number | null;
  media: number | null;
  mediaFinal: number | null;
  situacao: Status;
};

export default function AcademicSituation({ alunoId }: { alunoId?: number }) {
  const [rows, setRows] = useState<Situation[] | null>(null);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!alunoId) return;
    const controller = new AbortController();
    api('/notas/aluno/' + alunoId + '/situacao', { signal: controller.signal })
      .then(setRows).catch((failure: Error) => {
        if (!controller.signal.aborted) setError(failure.message);
      });
    return () => controller.abort();
  }, [alunoId]);

  return <section className="card">
    <h3>Minha situação acadêmica</h3>
    {!alunoId ? <p>Seu perfil precisa estar vinculado a um cadastro de aluno.</p> :
      error ? <Notification text={error}/> :
      !rows ? <p role="status">Carregando notas…</p> :
      rows.length === 0 ? <p>Sem notas lançadas.</p> :
      <div className="table-container"><table>
        <thead><tr>{['Turma', 'AV1', 'AV2', 'AVF', 'Média', 'Média Final', 'Situação'].map(column =>
          <th key={column} scope="col">{column}</th>)}</tr></thead>
        <tbody>{rows.map(row => <tr key={row.turmaId}>
          <td>{row.turmaId}</td>
          {[row.nota1, row.nota2, row.notaFinal, row.media, row.mediaFinal].map((value, index) =>
            <td key={index}>{value == null ? '—' : value.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}</td>)}
          <td><span className={'situation-badge ' + row.situacao.toLowerCase()}>{row.situacao}</span></td>
        </tr>)}</tbody>
      </table></div>}
  </section>;
}
