import Notification from './Notification';
import { useState } from 'react';
import { API_BASE, apiFetch } from '../auth/http';

type Format = 'csv' | 'pdf';

export default function ExportButtons({ endpoint, filename, turmaId }: {
  endpoint: string; filename: string; turmaId: string;
}) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');

  async function exportReport(formato: Format) {
    setPending(true);
    setError('');
    try {
      const query = new URLSearchParams({ formato, turmaId });
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

  return <div className="header-nav">
    <button type="button" disabled={!turmaId || pending} onClick={() => exportReport('csv')}>Exportar CSV</button>
    <button type="button" disabled={!turmaId || pending} onClick={() => exportReport('pdf')}>Exportar PDF</button>
    <Notification text={error} onClose={() => setError('')}/>
  </div>;
}
