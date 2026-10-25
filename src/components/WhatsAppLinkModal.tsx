import React, { useState, useEffect } from 'react';
import {
  QrCode,
  Smartphone,
  Phone,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  ExternalLink,
  X,
  Shield,
  Unlink,
  MessageCircle,
  ArrowRight,
  Copy,
  Check,
} from 'lucide-react';
import { WhatsAppLinkInfo } from '../types';
import {
  generatePairingCode,
  generateWhatsAppQrSession,
  openWhatsAppWebChat,
  WHATSAPP_SUPPORTED_COUNTRIES,
} from '../services/whatsappBridge';

interface WhatsAppLinkModalProps {
  isOpen: boolean;
  onClose: () => void;
  linkedInfo?: WhatsAppLinkInfo;
  onLinkAccount: (info: WhatsAppLinkInfo) => void;
  onUnlinkAccount: () => void;
  onImportWhatsAppChats: (phoneNumber: string) => void;
}

export const WhatsAppLinkModal: React.FC<WhatsAppLinkModalProps> = ({
  isOpen,
  onClose,
  linkedInfo,
  onLinkAccount,
  onUnlinkAccount,
  onImportWhatsAppChats,
}) => {
  const [method, setMethod] = useState<'qr' | 'phone'>('qr');
  const [countryCode, setCountryCode] = useState('+263');
  const [phoneInput, setPhoneInput] = useState('');
  const [pairingCode, setPairingCode] = useState<string | null>(null);
  const [codeCountdown, setCodeCountdown] = useState(60);
  const [isVerifying, setIsVerifying] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);
  const [qrSession, setQrSession] = useState(generateWhatsAppQrSession());
  const [syncNotice, setSyncNotice] = useState(false);

  // Refresh QR session every 60 seconds
  useEffect(() => {
    if (!isOpen || linkedInfo) return;
    const interval = setInterval(() => {
      setQrSession(generateWhatsAppQrSession());
    }, 60000);
    return () => clearInterval(interval);
  }, [isOpen, linkedInfo]);

  // Countdown timer for pairing code
  useEffect(() => {
    if (!pairingCode || codeCountdown <= 0) return;
    const timer = setInterval(() => {
      setCodeCountdown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [pairingCode, codeCountdown]);

  if (!isOpen) return null;

  const handleGeneratePairingCode = (e: React.FormEvent) => {
    e.preventDefault();
    if (!phoneInput.trim()) return;

    const fullPhone = `${countryCode}${phoneInput.trim().replace(/^0+/, '')}`;
    const code = generatePairingCode(fullPhone);
    setPairingCode(code);
    setCodeCountdown(60);
  };

  const handleSimulateSuccessfulLink = (phoneNumber: string) => {
    setIsVerifying(true);
    setTimeout(() => {
      const newLink: WhatsAppLinkInfo = {
        phoneNumber,
        linkedAt: Date.now(),
        deviceName: 'WhatsApp Multi-Device (Linked via SAT Bridge)',
        verified: true,
        pushSyncEnabled: true,
      };
      onLinkAccount(newLink);
      setIsVerifying(false);
      setPairingCode(null);
    }, 1500);
  };

  const handleCopyCode = () => {
    if (!pairingCode) return;
    navigator.clipboard.writeText(pairingCode.replace('-', ''));
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const handleTriggerSync = () => {
    if (!linkedInfo) return;
    onImportWhatsAppChats(linkedInfo.phoneNumber);
    setSyncNotice(true);
    setTimeout(() => setSyncNotice(false), 3000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-sm animate-fade-in text-slate-100">
      <div className="w-full max-w-md bg-slate-900 border border-emerald-500/30 rounded-2xl p-5 shadow-2xl flex flex-col gap-4 max-h-[92vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400">
              <MessageCircle className="w-5 h-5 fill-emerald-500/20" />
            </div>
            <div>
              <h3 className="font-semibold text-sm text-slate-100 flex items-center gap-1.5">
                Link WhatsApp Account
                <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-emerald-950 text-emerald-400 border border-emerald-800">
                  Bridge
                </span>
              </h3>
              <p className="text-[10px] text-slate-400">Multi-Device End-to-End Encrypted Relay</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* ALREADY LINKED VIEW */}
        {linkedInfo ? (
          <div className="flex flex-col gap-3 py-1">
            <div className="p-4 bg-slate-950 rounded-xl border border-emerald-500/40 flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-emerald-400">
                  <CheckCircle2 className="w-5 h-5" />
                  <span className="text-xs font-semibold">WhatsApp Account Linked</span>
                </div>
                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-950/80 px-2 py-0.5 rounded border border-emerald-800">
                  Online
                </span>
              </div>

              <div className="space-y-1 text-xs text-slate-300">
                <div className="flex justify-between">
                  <span className="text-slate-400">Phone Number:</span>
                  <span className="font-mono font-bold text-slate-100">{linkedInfo.phoneNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Device Protocol:</span>
                  <span className="text-slate-200">Noise / Signal Multi-Device</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Linked On:</span>
                  <span className="text-slate-400">{new Date(linkedInfo.linkedAt).toLocaleDateString()}</span>
                </div>
              </div>

              {syncNotice && (
                <div className="p-2 rounded-lg bg-emerald-950/70 border border-emerald-700 text-[11px] text-emerald-300 flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
                  <span>WhatsApp conversations synchronized into SAT Messenger!</span>
                </div>
              )}
            </div>

            {/* Quick Actions */}
            <div className="flex flex-col gap-2">
              <button
                onClick={handleTriggerSync}
                className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg transition-all"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Sync & Import WhatsApp Chats</span>
              </button>

              <button
                onClick={() => openWhatsAppWebChat(linkedInfo.phoneNumber)}
                className="w-full flex items-center justify-center gap-2 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-medium text-xs transition-colors border border-slate-700"
              >
                <ExternalLink className="w-3.5 h-3.5 text-emerald-400" />
                <span>Open in WhatsApp Web / App (wa.me)</span>
              </button>

              <button
                onClick={onUnlinkAccount}
                className="w-full flex items-center justify-center gap-1.5 py-2 rounded-xl text-rose-400 hover:bg-rose-950/40 text-xs transition-colors"
              >
                <Unlink className="w-3.5 h-3.5" />
                <span>Unlink WhatsApp Account</span>
              </button>
            </div>
          </div>
        ) : (
          /* NOT LINKED: LINKING WORKFLOW */
          <div className="flex flex-col gap-3 py-1">
            {/* Method Switcher */}
            <div className="grid grid-cols-2 gap-2 bg-slate-950 p-1 rounded-xl border border-slate-800">
              <button
                onClick={() => {
                  setMethod('qr');
                  setPairingCode(null);
                }}
                className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  method === 'qr' ? 'bg-slate-800 text-emerald-400 shadow' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <QrCode className="w-3.5 h-3.5" />
                <span>QR Code Pairing</span>
              </button>
              <button
                onClick={() => setMethod('phone')}
                className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  method === 'phone' ? 'bg-slate-800 text-emerald-400 shadow' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Smartphone className="w-3.5 h-3.5" />
                <span>Phone Pairing Code</span>
              </button>
            </div>

            {/* METHOD 1: QR CODE PAIRING */}
            {method === 'qr' && (
              <div className="flex flex-col items-center gap-3 py-1">
                <div className="p-3 bg-white rounded-xl shadow-xl flex flex-col items-center justify-center relative">
                  {/* Dynamic simulated QR Code grid */}
                  <div className="w-44 h-44 bg-slate-950 rounded-lg p-2 grid grid-cols-8 grid-rows-8 gap-0.5">
                    {Array.from({ length: 64 }).map((_, i) => (
                      <div
                        key={i}
                        className={`rounded-[1px] ${
                          i % 2 === 0 || i % 7 === 0 || i === 0 || i === 7 || i === 56 || i === 63
                            ? 'bg-emerald-500'
                            : i % 3 === 0
                            ? 'bg-emerald-300'
                            : 'bg-slate-900'
                        }`}
                      />
                    ))}
                  </div>
                  <span className="text-[9px] font-mono text-slate-700 font-bold mt-1">
                    SCAN WITH WHATSAPP
                  </span>
                </div>

                <div className="w-full space-y-1 text-[11px] text-slate-300 bg-slate-950 p-3 rounded-xl border border-slate-800 leading-relaxed">
                  <p className="font-semibold text-emerald-400 mb-1">To link your WhatsApp:</p>
                  <p>1. Open WhatsApp on your phone</p>
                  <p>2. Tap <strong>Menu (⋮)</strong> or <strong>Settings</strong> &gt; <strong>Linked Devices</strong></p>
                  <p>3. Tap <strong>Link a Device</strong> and point your camera to this screen</p>
                </div>

                <div className="w-full flex flex-col gap-1.5">
                  <button
                    onClick={() =>
                      handleSimulateSuccessfulLink(
                        countryCode === '+260'
                          ? '+260 97 554 1234'
                          : countryCode === '+263'
                          ? '+263 77 123 4567'
                          : `${countryCode} 123 456 789`
                      )
                    }
                    disabled={isVerifying}
                    className="w-full flex items-center justify-center gap-2 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow transition-all disabled:opacity-50"
                  >
                    <CheckCircle2 className={`w-3.5 h-3.5 ${isVerifying ? 'animate-spin' : ''}`} />
                    <span>
                      {isVerifying
                        ? 'Establishing Secure Handshake...'
                        : countryCode === '+260'
                        ? 'Confirm Pairing with Zambia (+260 97 554 1234)'
                        : 'Confirm Pairing from WhatsApp'}
                    </span>
                  </button>
                  <div className="flex items-center justify-center gap-2 text-[10px] text-slate-400">
                    <span>Quick test:</span>
                    <button
                      type="button"
                      onClick={() => handleSimulateSuccessfulLink('+260 97 554 1234')}
                      className="text-emerald-400 hover:underline flex items-center gap-0.5"
                    >
                      🇿🇲 Zambia (+260)
                    </button>
                    <span>•</span>
                    <button
                      type="button"
                      onClick={() => handleSimulateSuccessfulLink('+263 77 123 4567')}
                      className="text-emerald-400 hover:underline flex items-center gap-0.5"
                    >
                      🇿🇼 Zimbabwe (+263)
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* METHOD 2: 8-DIGIT PHONE PAIRING CODE */}
            {method === 'phone' && (
              <div className="flex flex-col gap-3 py-1">
                {!pairingCode ? (
                  <form onSubmit={handleGeneratePairingCode} className="flex flex-col gap-3">
                    <p className="text-[11px] text-slate-300 leading-relaxed">
                      Enter your WhatsApp phone number to generate an 8-character pairing code:
                    </p>

                    <div className="flex flex-col gap-2">
                      <div className="flex items-center gap-2">
                        <select
                          value={countryCode}
                          onChange={(e) => setCountryCode(e.target.value)}
                          className="px-2.5 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:border-emerald-500 font-medium"
                        >
                          {WHATSAPP_SUPPORTED_COUNTRIES.map((c) => (
                            <option key={c.code} value={c.dialCode}>
                              {c.flag} {c.name} ({c.dialCode})
                            </option>
                          ))}
                        </select>

                        <input
                          type="tel"
                          placeholder={
                            WHATSAPP_SUPPORTED_COUNTRIES.find((c) => c.dialCode === countryCode)
                              ?.placeholder || '97 123 4567'
                          }
                          value={phoneInput}
                          onChange={(e) => setPhoneInput(e.target.value)}
                          required
                          className="flex-1 px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:border-emerald-500"
                        />
                      </div>

                      {/* Quick Country Shortcuts */}
                      <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                        <span className="text-[10px] text-slate-500">Quick Select:</span>
                        <button
                          type="button"
                          onClick={() => {
                            setCountryCode('+260');
                            setPhoneInput('97 554 1234');
                          }}
                          className={`text-[10px] px-2 py-0.5 rounded-lg border transition-all ${
                            countryCode === '+260'
                              ? 'bg-emerald-950 text-emerald-300 border-emerald-700'
                              : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-slate-200'
                          }`}
                        >
                          🇿🇲 Zambia (+260)
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setCountryCode('+263');
                            setPhoneInput('77 123 4567');
                          }}
                          className={`text-[10px] px-2 py-0.5 rounded-lg border transition-all ${
                            countryCode === '+263'
                              ? 'bg-emerald-950 text-emerald-300 border-emerald-700'
                              : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-slate-200'
                          }`}
                        >
                          🇿🇼 Zimbabwe (+263)
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setCountryCode('+27');
                            setPhoneInput('82 432 9901');
                          }}
                          className={`text-[10px] px-2 py-0.5 rounded-lg border transition-all ${
                            countryCode === '+27'
                              ? 'bg-emerald-950 text-emerald-300 border-emerald-700'
                              : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-slate-200'
                          }`}
                        >
                          🇿🇦 South Africa (+27)
                        </button>
                      </div>
                    </div>

                    <button
                      type="submit"
                      className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow transition-all"
                    >
                      Generate 8-Character Pairing Code
                    </button>
                  </form>
                ) : (
                  <div className="flex flex-col items-center gap-3 py-1">
                    <div className="w-full bg-slate-950 p-4 rounded-xl border border-emerald-500/50 flex flex-col items-center gap-2">
                      <span className="text-[10px] text-slate-400 uppercase tracking-widest font-mono">
                        Enter Code in WhatsApp
                      </span>

                      <div className="font-mono text-2xl font-black tracking-widest text-emerald-400 select-all py-1">
                        {pairingCode}
                      </div>

                      <div className="flex items-center gap-3 text-xs">
                        <button
                          onClick={handleCopyCode}
                          className="flex items-center gap-1 text-[11px] text-slate-300 hover:text-white"
                        >
                          {copiedCode ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                          <span>{copiedCode ? 'Copied' : 'Copy Code'}</span>
                        </button>
                        <span className="text-slate-600">|</span>
                        <span className="text-[11px] text-slate-400 font-mono">
                          Expires in {codeCountdown}s
                        </span>
                      </div>
                    </div>

                    <div className="w-full text-[11px] text-slate-400 bg-slate-950/70 p-3 rounded-xl border border-slate-800 leading-relaxed">
                      On your phone, open the WhatsApp notification: <em>"Enter code to link new device"</em> and type the 8 characters above.
                    </div>

                    <button
                      onClick={() => handleSimulateSuccessfulLink(`${countryCode} ${phoneInput}`)}
                      disabled={isVerifying}
                      className="w-full flex items-center justify-center gap-2 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow transition-all disabled:opacity-50"
                    >
                      <CheckCircle2 className={`w-3.5 h-3.5 ${isVerifying ? 'animate-spin' : ''}`} />
                      <span>{isVerifying ? 'Completing Handshake...' : 'Confirm Code Entered on Phone'}</span>
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Footer */}
        <div className="flex items-center justify-between pt-2 border-t border-slate-800 text-[10px] text-slate-500">
          <span className="flex items-center gap-1">
            <Shield className="w-3 h-3 text-emerald-500" /> WhatsApp E2EE Signal Protocol
          </span>
          <button
            onClick={onClose}
            className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
