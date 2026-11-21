import Notification from './Notification';
import { useState } from 'react';
import { API_BASE, apiFetch } from '../auth/http';

type Format = 'csv' | 'pdf';

export default function ExportButtons({ endpoint, filename, turmaId, alunoId, allowUnfiltered = false }: {
  endpoint: string; filename: string; turmaId?: string; alunoId?: number | string; allowUnfiltered?: boolean;
}) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');

  async function exportReport(formato: Format) {
    setPending(true);
    setError('');
    try {
      const query = new URLSearchParams({ formato });
      if (turmaId) query.set('turmaId', turmaId);
      if (alunoId !== undefined && alunoId !== null && String(alunoId)) query.set('alunoId', String(alunoId));
      const response = await apiFetch(API_BASE + endpoint + '?' + query);
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement('a');
      link.href = url;
      link.download = filename + '.' + formato;
      document.body.append(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Falha ao exportar o relatório.');
    } finally {
      setPending(false);
    }
  }

  const disabled = pending || (!allowUnfiltered && !turmaId && !alunoId);

  return <div className="header-nav">
    <button type="button" disabled={disabled} onClick={() => exportReport('csv')}>Exportar CSV</button>
    <button type="button" disabled={disabled} onClick={() => exportReport('pdf')}>Exportar PDF</button>
    <Notification text={error} onClose={() => setError('')}/>
  </div>;
}
