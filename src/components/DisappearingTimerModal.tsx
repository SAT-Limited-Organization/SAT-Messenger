import React from 'react';
import { Clock, Flame, ShieldAlert, Check, X } from 'lucide-react';

interface DisappearingTimerModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentTimer: number; // in seconds
  onSelectTimer: (seconds: number) => void;
}

const TIMER_OPTIONS = [
  { label: 'Off', seconds: 0, description: 'Messages persist normally until manually deleted' },
  { label: '10 Seconds', seconds: 10, description: 'Instant burn timer for ultra-sensitive secrets & testing' },
  { label: '1 Minute', seconds: 60, description: 'Ephemeral quick view' },
  { label: '1 Hour', seconds: 3600, description: 'Disappears 1 hour after send' },
  { label: '24 Hours', seconds: 86400, description: 'Daily automatic shredding (WhatsApp standard)' },
  { label: '7 Days', seconds: 604800, description: 'Weekly retention window' },
];

export const DisappearingTimerModal: React.FC<DisappearingTimerModalProps> = ({
  isOpen,
  onClose,
  currentTimer,
  onSelectTimer,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-sm animate-fade-in text-slate-100">
      <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-2xl flex flex-col gap-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
              <Flame className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-semibold text-sm text-slate-200">Disappearing Messages</h3>
              <p className="text-[10px] text-slate-400">Self-destruct timer for this conversation</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-[11px] text-slate-300 leading-relaxed flex items-start gap-2">
          <Clock className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <span>
            When enabled, all new messages sent in this chat will automatically self-destruct for all participants after the timer expires.
          </span>
        </div>

        <div className="flex flex-col gap-1.5 py-1">
          {TIMER_OPTIONS.map((opt) => {
            const isSelected = currentTimer === opt.seconds;
            return (
              <button
                key={opt.seconds}
                onClick={() => {
                  onSelectTimer(opt.seconds);
                  onClose();
                }}
                className={`w-full flex items-center justify-between p-3 rounded-xl border text-left transition-all ${
                  isSelected
                    ? 'bg-emerald-950/40 border-emerald-500/50 text-emerald-300 shadow-sm'
                    : 'bg-slate-950/60 border-slate-800/80 text-slate-300 hover:bg-slate-800/60'
                }`}
              >
                <div className="flex flex-col">
                  <span className="font-semibold text-xs text-slate-200">{opt.label}</span>
                  <span className="text-[10px] text-slate-400">{opt.description}</span>
                </div>
                {isSelected && <Check className="w-4 h-4 text-emerald-400" />}
              </button>
            );
          })}
        </div>

        <div className="text-[10px] text-slate-500 text-center">
          Encrypted cryptographic shredding by SAT Messenger
        </div>
      </div>
    </div>
  );
};
