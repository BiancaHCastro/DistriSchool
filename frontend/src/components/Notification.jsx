import { createPortal } from 'react-dom';

export default function Notification({ text, type = 'error', onClose }) {
  if (!text) return null;
  return createPortal(
    <aside className={`app-notification app-notification--${type}`}>
      <div role={type === 'error' ? 'alert' : 'status'}>{text}</div>
      {onClose && <button type="button" aria-label="Fechar aviso" onClick={onClose}>×</button>}
    </aside>,
    document.getElementById('notification-stack') ?? document.body,
  );
}
