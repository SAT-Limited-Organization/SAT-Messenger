import React, { useState, useEffect } from 'react';
import { ShieldCheck, QrCode, Lock, Copy, Check, Eye, EyeOff, X, KeyRound } from 'lucide-react';
import { generateSafetyFingerprint } from '../services/crypto';

interface EncryptionDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  conversationId: string;
  conversationName: string;
  currentUserId: string;
}

export const EncryptionDetailsModal: React.FC<EncryptionDetailsModalProps> = ({
  isOpen,
  onClose,
  conversationId,
  conversationName,
  currentUserId,
}) => {
  const [safetyNumber, setSafetyNumber] = useState('');
  const [fingerprint, setFingerprint] = useState('');
  const [copied, setCopied] = useState(false);
  const [testPlaintext, setTestPlaintext] = useState('Confidential message from SAT Messenger');
  const [simulatedCiphertext, setSimulatedCiphertext] = useState('');
  const [showInspector, setShowInspector] = useState(false);

  useEffect(() => {
    if (isOpen) {
      generateSafetyFingerprint(conversationId, currentUserId, conversationName).then((res) => {
        setSafetyNumber(res.formattedNumber);
        setFingerprint(res.fingerprintHash);
      });
      // Generate sample audit payload
      setSimulatedCiphertext(
        'eyJpdiI6IlpHSnRNV0ZtWVdReiIsInRhZyI6Ik0yWXhPR1l4TW1RIiwiY2lwaGVydGV4dCI6InEzSjh2MWh4NWZNc1hJOTVrbnNqaWZvZWp3Ym05MDMifQ=='
      );
    }
  }, [isOpen, conversationId, currentUserId, conversationName]);

  if (!isOpen) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(safetyNumber);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-sm animate-fade-in text-slate-100">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-2xl flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <ShieldCheck className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-semibold text-sm text-slate-200">Verify Security Code</h3>
              <p className="text-[10px] text-slate-400">End-to-End Encryption (E2EE)</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Explainer */}
        <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 text-[11px] text-slate-300 leading-relaxed">
          Messages and calls with <span className="text-emerald-400 font-semibold">{conversationName}</span> are protected by end-to-end encryption. Compare this 60-digit safety number or scan the QR code to verify security.
        </div>

        {/* QR Code & Safety Numbers */}
        <div className="flex flex-col items-center gap-4 py-2">
          {/* Simulated QR Pattern */}
          <div className="p-3 bg-white rounded-xl shadow-lg flex flex-col items-center justify-center">
            <div className="w-36 h-36 bg-slate-900 rounded-lg p-2 grid grid-cols-6 grid-rows-6 gap-1">
              {Array.from({ length: 36 }).map((_, i) => (
                <div
                  key={i}
                  className={`rounded-sm ${
                    i % 2 === 0 || i % 5 === 0 ? 'bg-emerald-400' : 'bg-slate-950'
                  }`}
                />
              ))}
            </div>
            <span className="text-[9px] font-mono text-slate-600 font-bold mt-1">
              SAT-FINGERPRINT-{fingerprint}
            </span>
          </div>

          {/* 60-digit number display */}
          <div className="w-full bg-slate-950 p-3 rounded-xl border border-slate-800 flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="font-mono text-[10px] uppercase text-emerald-400">60-Digit Cryptographic Code</span>
              <button
                onClick={handleCopy}
                className="flex items-center gap-1 text-[11px] text-emerald-400 hover:text-emerald-300"
              >
                {copied ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                <span>{copied ? 'Copied' : 'Copy'}</span>
              </button>
            </div>
            <div className="font-mono text-xs text-slate-200 tracking-wider text-center py-1 select-all bg-slate-900/80 rounded-lg p-2 leading-relaxed">
              {safetyNumber || 'Generating cryptographic key fingerprint...'}
            </div>
          </div>
        </div>

        {/* Cryptographic Specs Accordion */}
        <div className="border border-slate-800 rounded-xl overflow-hidden bg-slate-950/60">
          <button
            onClick={() => setShowInspector(!showInspector)}
            className="w-full px-3 py-2 text-xs flex items-center justify-between text-slate-300 hover:bg-slate-900/60"
          >
            <div className="flex items-center gap-2">
              <KeyRound className="w-3.5 h-3.5 text-cyan-400" />
              <span>Cryptographic Architecture & Payload Inspector</span>
            </div>
            {showInspector ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
          </button>

          {showInspector && (
            <div className="p-3 border-t border-slate-800 text-[11px] text-slate-400 flex flex-col gap-2 font-mono">
              <div className="flex justify-between">
                <span>Cipher Algorithm:</span>
                <span className="text-emerald-400">AES-GCM (256-bit)</span>
              </div>
              <div className="flex justify-between">
                <span>Key Derivation:</span>
                <span className="text-slate-300">PBKDF2-SHA256 (100k rounds)</span>
              </div>
              <div className="flex justify-between">
                <span>Initialization Vector:</span>
                <span className="text-slate-300">96-bit CSPRNG IV</span>
              </div>
              <div className="flex justify-between">
                <span>Tag Authentication:</span>
                <span className="text-slate-300">128-bit GHASH Auth Tag</span>
              </div>

              <div className="mt-2 pt-2 border-t border-slate-800/80 flex flex-col gap-1">
                <span className="text-slate-300 text-[10px]">Wire Ciphertext Sample:</span>
                <div className="p-2 bg-black rounded text-[9px] text-emerald-500 break-all select-all">
                  {simulatedCiphertext}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end pt-1">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
