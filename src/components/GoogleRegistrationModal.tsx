import React, { useState } from 'react';
import { Shield, Building2, UserCheck, AlertCircle, CheckCircle2, X, ExternalLink, Lock } from 'lucide-react';

interface GoogleRegistrationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirmGoogleSignIn: () => Promise<void>;
}

export const GoogleRegistrationModal: React.FC<GoogleRegistrationModalProps> = ({
  isOpen,
  onClose,
  onConfirmGoogleSignIn,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleProceed = async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      await onConfirmGoogleSignIn();
      setIsLoading(false);
      onClose();
    } catch (err: any) {
      setIsLoading(false);
      setErrorMsg(err?.message || 'Google registration was cancelled or failed.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-md animate-fade-in text-slate-100">
      <div className="w-full max-w-md bg-slate-900 border border-emerald-500/40 rounded-2xl p-5 shadow-2xl flex flex-col gap-4 max-h-[92vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 font-bold text-sm">
              SAT
            </div>
            <div>
              <h3 className="font-bold text-sm text-slate-100">Google Registration & Linkage</h3>
              <p className="text-[10px] text-slate-400">The SAT Limited Organization</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Primary Notification Box: SAT Limited is the one you are linking to */}
        <div className="p-4 rounded-xl bg-gradient-to-r from-emerald-950/90 to-slate-950 border-2 border-emerald-500/60 shadow-lg flex flex-col gap-2">
          <div className="flex items-center gap-2 text-emerald-400 font-bold text-xs">
            <Building2 className="w-4 h-4 shrink-0 text-emerald-300" />
            <span className="text-emerald-300 uppercase tracking-wide text-[11px]">Organization Linkage Notice</span>
          </div>

          <div className="text-sm font-bold text-white leading-snug">
            SAT Limited is the organization you are linking your account to.
          </div>

          <p className="text-[11px] text-slate-300 leading-relaxed">
            During this Google registration, your Google credentials will be authenticated with{' '}
            <strong className="text-emerald-300">The SAT Limited Organization</strong>, founded and led by{' '}
            <strong className="text-slate-100">Mr Simbarashe Tembo</strong>.
          </p>
        </div>

        {/* Organization & Security Details */}
        <div className="p-3.5 bg-slate-950 rounded-xl border border-slate-800 space-y-2 text-xs">
          <div className="flex items-center justify-between text-slate-400 text-[11px] border-b border-slate-800/80 pb-2">
            <span>Entity Name:</span>
            <span className="font-semibold text-slate-200">The SAT Limited Organization</span>
          </div>
          <div className="flex items-center justify-between text-slate-400 text-[11px] border-b border-slate-800/80 pb-2">
            <span>Founder & CEO:</span>
            <span className="font-semibold text-amber-300">Mr Simbarashe Tembo</span>
          </div>
          <div className="flex items-center justify-between text-slate-400 text-[11px] border-b border-slate-800/80 pb-2">
            <span>Founder Contact:</span>
            <span className="font-mono text-emerald-400 text-[10px]">simbarasheaugustustembo@gmail.com</span>
          </div>
          <div className="flex items-center justify-between text-slate-400 text-[11px]">
            <span>App Identification:</span>
            <span className="font-mono text-slate-300 text-[10px]">com.sat_commuity.satmessenger</span>
          </div>
        </div>

        {/* Sovereign E2EE Privacy Guarantee */}
        <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px] text-slate-400 flex items-start gap-2.5">
          <Lock className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <p className="leading-relaxed">
            <strong className="text-slate-200">Sovereign Privacy Guarantee:</strong> Linking your Google Account to{' '}
            <strong>SAT Limited</strong> provides multi-device authentication and cloud backup. Your 256-bit AES-GCM
            encryption keys stay stored exclusively on your local device.
          </p>
        </div>

        {errorMsg && (
          <div className="p-2.5 rounded-xl bg-rose-950/80 border border-rose-800 text-xs text-rose-300 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex flex-col gap-2 pt-1 border-t border-slate-800">
          <button
            onClick={handleProceed}
            disabled={isLoading}
            className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg transition-all disabled:opacity-50"
          >
            <UserCheck className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            <span>
              {isLoading
                ? 'Connecting to SAT Limited...'
                : 'Continue Google Registration with SAT Limited'}
            </span>
          </button>

          <button
            onClick={onClose}
            disabled={isLoading}
            className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs transition-colors"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
};
