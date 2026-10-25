import React, { useState } from 'react';
import {
  Palette,
  Shield,
  User,
  LogIn,
  LogOut,
  Check,
  X,
  Award,
  Info,
  Globe,
  Building2,
  Edit2,
  Camera,
  Save,
  CheckCircle2,
  Mail,
  Sparkles,
  MessageCircle,
} from 'lucide-react';
import { ThemeId, UserProfile } from '../types';
import { THEMES } from '../services/themes';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentTheme: ThemeId;
  onSelectTheme: (theme: ThemeId) => void;
  currentUser: UserProfile;
  onUpdateProfile: (updated: UserProfile) => void;
  isGoogleAuthenticated: boolean;
  onGoogleSignIn: () => void;
  onSignOut: () => void;
  onOpenWhatsAppLink?: () => void;
}

const PRESET_AVATARS = [
  'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=128&q=80',
  'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=128&q=80',
  'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=128&q=80',
  'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=128&q=80',
  'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=128&q=80',
  'https://api.dicebear.com/7.x/bottts/svg?seed=SAT_Security',
];

const PRESET_STATUSES = [
  'Available 🟢',
  'Encrypted Only 🔐',
  'Busy 🔴',
  'At Work 💼',
  'On the Move 🚗',
  'In Meeting ⏱️',
];

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  currentTheme,
  onSelectTheme,
  currentUser,
  onUpdateProfile,
  isGoogleAuthenticated,
  onGoogleSignIn,
  onSignOut,
  onOpenWhatsAppLink,
}) => {
  const [activeTab, setActiveTab] = useState<'profile' | 'themes' | 'about'>('profile');

  // Profile Form State
  const [displayName, setDisplayName] = useState(currentUser.displayName || '');
  const [email, setEmail] = useState(currentUser.email || '');
  const [bio, setBio] = useState(currentUser.bio || '');
  const [status, setStatus] = useState(currentUser.status || 'Available 🟢');
  const [photoURL, setPhotoURL] = useState(currentUser.photoURL || PRESET_AVATARS[0]);
  const [savedSuccess, setSavedSuccess] = useState(false);

  if (!isOpen) return null;

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    if (!displayName.trim()) return;

    const updatedUser: UserProfile = {
      ...currentUser,
      displayName: displayName.trim(),
      email: email.trim(),
      bio: bio.trim(),
      status: status.trim(),
      photoURL: photoURL.trim(),
    };

    onUpdateProfile(updatedUser);
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 3000);
  };

  const handleAvatarFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      setPhotoURL(reader.result as string);
    };
    reader.readAsDataURL(file);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-sm animate-fade-in text-slate-100">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-2xl flex flex-col gap-4 max-h-[92vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Shield className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-semibold text-sm text-slate-200">Settings & Profile</h3>
              <p className="text-[10px] text-slate-400">SAT Messenger • v1.0.0</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Navigation Tabs */}
        <div className="grid grid-cols-3 gap-1.5 bg-slate-950 p-1 rounded-xl border border-slate-800 text-xs">
          <button
            onClick={() => setActiveTab('profile')}
            className={`py-1.5 rounded-lg font-medium transition-all ${
              activeTab === 'profile'
                ? 'bg-slate-800 text-emerald-400 shadow'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            My Profile
          </button>
          <button
            onClick={() => setActiveTab('themes')}
            className={`py-1.5 rounded-lg font-medium transition-all ${
              activeTab === 'themes'
                ? 'bg-slate-800 text-emerald-400 shadow'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Themes
          </button>
          <button
            onClick={() => setActiveTab('about')}
            className={`py-1.5 rounded-lg font-medium transition-all ${
              activeTab === 'about'
                ? 'bg-slate-800 text-emerald-400 shadow'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Organization
          </button>
        </div>

        {/* TAB 1: EDIT PROFILE SETTINGS */}
        {activeTab === 'profile' && (
          <form onSubmit={handleSaveProfile} className="flex flex-col gap-4 py-1">
            {savedSuccess && (
              <div className="p-2.5 rounded-xl bg-emerald-950/70 border border-emerald-700/60 text-xs text-emerald-300 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 shrink-0" />
                <span>Profile settings saved and synced to Firestore!</span>
              </div>
            )}

            {/* Profile Avatar Editor */}
            <div className="flex flex-col items-center gap-2 p-3 bg-slate-950/80 rounded-xl border border-slate-800">
              <div className="relative group">
                <img
                  src={photoURL}
                  alt={displayName}
                  className="w-16 h-16 rounded-full object-cover ring-2 ring-emerald-500/50 bg-slate-800 shadow-md"
                />
                <label className="absolute inset-0 rounded-full bg-black/50 opacity-0 group-hover:opacity-100 flex items-center justify-center cursor-pointer transition-opacity text-white">
                  <Camera className="w-5 h-5" />
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleAvatarFile}
                  />
                </label>
              </div>

              {/* Avatar Presets */}
              <div className="flex items-center gap-1.5 mt-1">
                {PRESET_AVATARS.map((url, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setPhotoURL(url)}
                    className={`w-7 h-7 rounded-full overflow-hidden border transition-all ${
                      photoURL === url ? 'ring-2 ring-emerald-400 scale-110' : 'opacity-70 hover:opacity-100'
                    }`}
                  >
                    <img src={url} alt="" className="w-full h-full object-cover" />
                  </button>
                ))}
              </div>
            </div>

            {/* Display Name Input */}
            <div className="flex flex-col gap-1">
              <label className="text-[11px] font-medium text-slate-300">Display Name</label>
              <input
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                required
                placeholder="Your full name or handle"
                className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-emerald-500"
              />
            </div>

            {/* Email Address Input */}
            <div className="flex flex-col gap-1">
              <label className="text-[11px] font-medium text-slate-300">Email Address</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="name@gmail.com"
                className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-emerald-500"
              />
            </div>

            {/* Bio / About Input */}
            <div className="flex flex-col gap-1">
              <label className="text-[11px] font-medium text-slate-300">Bio / About</label>
              <textarea
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                rows={2}
                placeholder="Short bio or cryptographic role..."
                className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-emerald-500 resize-none"
              />
            </div>

            {/* Presence Status Chips */}
            <div className="flex flex-col gap-1.5">
              <label className="text-[11px] font-medium text-slate-300">Status Message</label>
              <input
                type="text"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                placeholder="What's your current status?"
                className="px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-emerald-500 mb-1"
              />
              <div className="flex flex-wrap gap-1.5">
                {PRESET_STATUSES.map((st) => (
                  <button
                    key={st}
                    type="button"
                    onClick={() => setStatus(st)}
                    className={`px-2 py-0.5 rounded-lg text-[10px] font-medium transition-all ${
                      status === st
                        ? 'bg-emerald-950 text-emerald-300 border border-emerald-700/60'
                        : 'bg-slate-950 text-slate-400 border border-slate-800 hover:text-slate-200'
                    }`}
                  >
                    {st}
                  </button>
                ))}
              </div>
            </div>

            {/* Google Account Registration status */}
            <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 flex flex-col gap-2.5 text-xs">
              <div className="flex items-center justify-between">
                <div className="flex flex-col">
                  <span className="font-semibold text-slate-200">Google Registration & Linkage</span>
                  <span className="text-[10px] text-slate-400">
                    {isGoogleAuthenticated ? 'Linked to SAT Limited Organization' : 'Independent Profile Session'}
                  </span>
                </div>
                {isGoogleAuthenticated ? (
                  <button
                    type="button"
                    onClick={onSignOut}
                    className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-rose-950/60 hover:bg-rose-900 text-rose-300 text-xs border border-rose-800"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Sign Out</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={onGoogleSignIn}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs shadow"
                  >
                    <LogIn className="w-3.5 h-3.5" />
                    <span>Register with Google</span>
                  </button>
                )}
              </div>

              {/* Linkage Statement */}
              <div className="p-2 rounded-lg bg-emerald-950/40 border border-emerald-800/60 text-[10px] text-slate-300 flex items-start gap-1.5 leading-relaxed">
                <Shield className="w-3 h-3 text-emerald-400 shrink-0 mt-0.5" />
                <span>
                  <strong>Registration Notice:</strong> During Google registration, <strong className="text-emerald-400">SAT Limited</strong> (The SAT Limited Organization, Founded by Mr Simbarashe Tembo) is the one you are linking to.
                </span>
              </div>
            </div>

            {/* WhatsApp Bridge Account status */}
            <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 flex items-center justify-between text-xs">
              <div className="flex flex-col">
                <div className="flex items-center gap-1.5">
                  <span className="font-semibold text-slate-200">WhatsApp Multi-Device</span>
                  {currentUser.whatsappLinked && (
                    <span className="text-[9px] px-1.5 py-0.2 rounded bg-emerald-950 text-emerald-300 border border-emerald-800 font-mono font-bold">
                      LINKED
                    </span>
                  )}
                </div>
                <span className="text-[10px] text-slate-400">
                  {currentUser.whatsappLinked
                    ? currentUser.whatsappLinked.phoneNumber
                    : 'Pair via QR code or 8-char code'}
                </span>
              </div>
              {onOpenWhatsAppLink && (
                <button
                  type="button"
                  onClick={() => {
                    onClose();
                    onOpenWhatsAppLink();
                  }}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/40 text-emerald-300 text-xs border border-emerald-500/40 transition-colors"
                >
                  <MessageCircle className="w-3.5 h-3.5 text-emerald-400" />
                  <span>{currentUser.whatsappLinked ? 'Manage' : 'Link WhatsApp'}</span>
                </button>
              )}
            </div>

            {/* Save Profile Button */}
            <button
              type="submit"
              className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg transition-all"
            >
              <Save className="w-4 h-4" />
              <span>Save & Sync Profile Settings</span>
            </button>
          </form>
        )}

        {/* TAB 2: DARK MODE THEMES */}
        {activeTab === 'themes' && (
          <div className="flex flex-col gap-2 py-1">
            <div className="flex items-center gap-2 text-xs font-semibold text-slate-300 mb-1">
              <Palette className="w-3.5 h-3.5 text-emerald-400" />
              <span>Customizable Dark Mode Themes</span>
            </div>

            <div className="grid grid-cols-1 gap-2">
              {(Object.keys(THEMES) as ThemeId[]).map((key) => {
                const theme = THEMES[key];
                const isSelected = currentTheme === key;
                return (
                  <button
                    key={key}
                    type="button"
                    onClick={() => onSelectTheme(key)}
                    className={`w-full p-2.5 rounded-xl border text-left flex items-center justify-between transition-all ${
                      isSelected
                        ? 'bg-slate-800/90 border-emerald-500/70 text-slate-100 shadow-md ring-1 ring-emerald-500/30'
                        : 'bg-slate-950/70 border-slate-800 text-slate-300 hover:bg-slate-900'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className="w-5 h-5 rounded-full border border-slate-600 shadow-inner flex items-center justify-center"
                        style={{ backgroundColor: theme.bgDark }}
                      >
                        <div className="w-2 h-2 rounded-full" style={{ backgroundColor: theme.accentColor }} />
                      </div>
                      <div className="flex flex-col">
                        <span className="text-xs font-medium text-slate-200">{theme.name}</span>
                        <span className="text-[10px] text-slate-400 line-clamp-1">{theme.description}</span>
                      </div>
                    </div>
                    {isSelected && <Check className="w-4 h-4 text-emerald-400" />}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* TAB 3: ABOUT ORGANIZATION */}
        {activeTab === 'about' && (
          <div className="flex flex-col gap-3 py-1">
            <div className="p-3.5 bg-gradient-to-br from-slate-950 to-slate-900 rounded-xl border border-emerald-500/30 flex flex-col gap-2.5 text-xs">
              <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                <div className="flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-amber-400" />
                  <span className="font-semibold text-slate-200">The SAT Limited Organization</span>
                </div>
                <span className="text-[9px] px-1.5 py-0.5 rounded bg-amber-500/10 border border-amber-500/30 text-amber-300 font-mono">
                  Official
                </span>
              </div>

              <div className="space-y-1.5 text-[11px] text-slate-300">
                <p>
                  <strong className="text-slate-400">Founder & CEO:</strong>{' '}
                  <span className="text-amber-300 font-semibold">Mr Simbarashe Tembo</span>
                </p>
                <p>
                  <strong className="text-slate-400">Founder Contact Email:</strong>{' '}
                  <span className="text-emerald-400 font-mono">simbarasheaugustustembo@gmail.com</span>
                </p>
                <p>
                  <strong className="text-slate-400">Application:</strong> SAT Messenger
                </p>
                <p>
                  <strong className="text-slate-400">Android Package Name:</strong>{' '}
                  <code className="text-emerald-400 font-mono bg-slate-950 px-1 py-0.5 rounded text-[10px]">
                    com.sat_commuity.satmessenger
                  </code>
                </p>
                <p>
                  <strong className="text-slate-400">Security Architecture:</strong> Client-side AES-GCM 256-bit with PBKDF2/SHA-256 derivation & Firestore cloud persistence.
                </p>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 text-[11px] text-slate-400 leading-relaxed">
              SAT Messenger is designed for absolute privacy, military-grade end-to-end encryption, and seamless cross-platform syncing across Android, Web, and Desktop environments.
            </div>
          </div>
        )}

        {/* Close Button */}
        <div className="flex justify-end pt-1 border-t border-slate-800">
          <button
            type="button"
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
