import React, { useState } from 'react';
import { Users, UserPlus, Globe, MessageSquare, MessageCircle, X, Plus, Shield, Search, Check } from 'lucide-react';
import { Conversation, RoomType, UserProfile } from '../types';
import { APP_USERS_DIRECTORY } from '../services/mockData';

interface NewChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateConversation: (newConv: Partial<Conversation>) => void;
  onSyncGoogleChat: () => void;
  onOpenWhatsAppLink?: () => void;
}

export const NewChatModal: React.FC<NewChatModalProps> = ({
  isOpen,
  onClose,
  onCreateConversation,
  onSyncGoogleChat,
  onOpenWhatsAppLink,
}) => {
  const [tab, setTab] = useState<'direct' | 'group' | 'community'>('direct');
  const [directName, setDirectName] = useState('');
  const [searchUserQuery, setSearchUserQuery] = useState('');
  const [groupName, setGroupName] = useState('');
  const [groupDesc, setGroupDesc] = useState('');
  const [selectedGroupMembers, setSelectedGroupMembers] = useState<string[]>([]);
  const [showManualInput, setShowManualInput] = useState(false);

  if (!isOpen) return null;

  const filteredUsers = APP_USERS_DIRECTORY.filter(
    (u) =>
      u.displayName.toLowerCase().includes(searchUserQuery.toLowerCase()) ||
      u.email.toLowerCase().includes(searchUserQuery.toLowerCase()) ||
      (u.bio && u.bio.toLowerCase().includes(searchUserQuery.toLowerCase()))
  );

  const handleStartDirectWithUser = (user: UserProfile) => {
    onCreateConversation({
      type: 'direct',
      name: user.displayName,
      description: user.email,
      avatar: user.photoURL,
      memberIds: ['current-user', user.userId],
      disappearingTimerSeconds: 0,
      lastMessageSnippet: 'Encrypted channel established.',
      unreadCount: 0,
    });
    onClose();
  };

  const handleCreateDirectManual = (e: React.FormEvent) => {
    e.preventDefault();
    if (!directName.trim()) return;

    onCreateConversation({
      type: 'direct',
      name: directName.trim(),
      description: `${directName.toLowerCase().replace(/\s+/g, '')}@satmessenger.org`,
      avatar: `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(directName)}`,
      disappearingTimerSeconds: 0,
      lastMessageSnippet: 'Encrypted channel established.',
      unreadCount: 0,
    });
    setDirectName('');
    onClose();
  };

  const toggleGroupMemberSelection = (userId: string) => {
    if (selectedGroupMembers.includes(userId)) {
      setSelectedGroupMembers(selectedGroupMembers.filter((id) => id !== userId));
    } else {
      setSelectedGroupMembers([...selectedGroupMembers, userId]);
    }
  };

  const handleCreateGroup = (e: React.FormEvent) => {
    e.preventDefault();
    if (!groupName.trim()) return;

    onCreateConversation({
      type: 'group',
      name: groupName.trim(),
      description: groupDesc.trim() || 'SAT Messenger encrypted group',
      avatar: `https://api.dicebear.com/7.x/identicon/svg?seed=${encodeURIComponent(groupName)}`,
      memberIds: ['current-user', ...selectedGroupMembers],
      adminIds: ['current-user'],
      disappearingTimerSeconds: 0,
      lastMessageSnippet: 'Group created with E2EE protection.',
      unreadCount: 0,
    });
    setGroupName('');
    setGroupDesc('');
    setSelectedGroupMembers([]);
    onClose();
  };

  const handleJoinCommunity = (name: string, desc: string, icon: string) => {
    onCreateConversation({
      type: 'community',
      name,
      description: desc,
      avatar: icon,
      disappearingTimerSeconds: 0,
      lastMessageSnippet: 'Joined global community channel.',
      unreadCount: 0,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-sm animate-fade-in text-slate-100">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-2xl flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Plus className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-semibold text-sm text-slate-200">New Secure Conversation</h3>
              <p className="text-[10px] text-slate-400">SAT Messenger Worldwide</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Tabs */}
        <div className="grid grid-cols-3 gap-2 bg-slate-950 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => setTab('direct')}
            className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
              tab === 'direct' ? 'bg-slate-800 text-emerald-400 shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <UserPlus className="w-3.5 h-3.5" />
            <span>Direct</span>
          </button>
          <button
            onClick={() => setTab('group')}
            className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
              tab === 'group' ? 'bg-slate-800 text-emerald-400 shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            <span>Group</span>
          </button>
          <button
            onClick={() => setTab('community')}
            className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
              tab === 'community' ? 'bg-slate-800 text-emerald-400 shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Globe className="w-3.5 h-3.5" />
            <span>Community</span>
          </button>
        </div>

        {/* Form Content */}
        {tab === 'direct' && (
          <div className="flex flex-col gap-3 py-1">
            {/* Search Directory of registered people */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="Search registered people on SAT Messenger..."
                value={searchUserQuery}
                onChange={(e) => setSearchUserQuery(e.target.value)}
                className="w-full pl-8 pr-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-emerald-500"
              />
            </div>

            {/* Featured SAT Limited AI Card */}
            <div
              onClick={() => {
                const aiUser = APP_USERS_DIRECTORY.find((u) => u.userId === 'sat-limited-ai-agent');
                if (aiUser) handleStartDirectWithUser(aiUser);
              }}
              className="p-2.5 rounded-xl bg-gradient-to-r from-cyan-950/70 to-emerald-950/60 border border-cyan-500/40 hover:border-cyan-400 cursor-pointer transition-all flex items-center justify-between shadow-md group"
            >
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-cyan-600 to-emerald-500 flex items-center justify-center text-white shadow ring-1 ring-cyan-400/50">
                  <Shield className="w-4 h-4" />
                </div>
                <div className="flex flex-col">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-bold text-white group-hover:text-cyan-300 transition-colors">
                      SAT Limited AI
                    </span>
                    <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-cyan-950 text-cyan-300 border border-cyan-800">
                      Official AI
                    </span>
                  </div>
                  <span className="text-[10px] text-slate-300 line-clamp-1">
                    Ask questions on SAT Limited, AES-256 E2EE, WhatsApp bridge & tasks
                  </span>
                </div>
              </div>
              <span className="text-[10px] font-medium text-cyan-400 px-2 py-0.5 rounded bg-cyan-950 border border-cyan-800">
                Chat
              </span>
            </div>

            <div className="flex items-center justify-between text-[11px] text-slate-400">
              <span>People on SAT Messenger ({filteredUsers.length})</span>
              <button
                onClick={() => setShowManualInput(!showManualInput)}
                className="text-emerald-400 hover:underline"
              >
                {showManualInput ? 'Hide Custom Input' : '+ Custom Handle / Email'}
              </button>
            </div>

            {/* Custom Input (if toggled) */}
            {showManualInput && (
              <form onSubmit={handleCreateDirectManual} className="p-3 bg-slate-950/80 rounded-xl border border-slate-800 flex flex-col gap-2">
                <input
                  type="text"
                  placeholder="Enter contact name or email handle..."
                  value={directName}
                  onChange={(e) => setDirectName(e.target.value)}
                  required
                  className="px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-200"
                />
                <button
                  type="submit"
                  className="py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs transition-colors"
                >
                  Start Custom Chat
                </button>
              </form>
            )}

            {/* Directory Cards */}
            <div className="flex flex-col gap-1.5 max-h-60 overflow-y-auto divide-y divide-slate-800/60 pr-1">
              {filteredUsers.length === 0 ? (
                <div className="p-4 text-center text-xs text-slate-500">No matching contacts found.</div>
              ) : (
                filteredUsers.map((user) => (
                  <div
                    key={user.userId}
                    onClick={() => handleStartDirectWithUser(user)}
                    className="pt-2 pb-1.5 flex items-center justify-between hover:bg-slate-800/60 p-2 rounded-xl cursor-pointer transition-colors group"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="relative shrink-0">
                        <img
                          src={user.photoURL}
                          alt={user.displayName}
                          className="w-10 h-10 rounded-full object-cover ring-1 ring-slate-700 bg-slate-800"
                        />
                        {user.isOnline && (
                          <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full bg-emerald-500 border-2 border-slate-900" />
                        )}
                      </div>
                      <div className="flex flex-col min-w-0">
                        <div className="flex items-center gap-1.5">
                          <span className="font-semibold text-xs text-slate-200 truncate group-hover:text-emerald-400 transition-colors">
                            {user.displayName}
                          </span>
                          {user.userId === 'founder-simbarashe-tembo' && (
                            <span className="text-[9px] font-bold px-1.5 py-0.2 rounded bg-amber-500/10 text-amber-300 border border-amber-500/30">
                              Founder
                            </span>
                          )}
                        </div>
                        <span className="text-[10px] text-slate-400 truncate">{user.email}</span>
                        {user.status && (
                          <span className="text-[10px] text-slate-500 truncate line-clamp-1 mt-0.5">
                            {user.status}
                          </span>
                        )}
                      </div>
                    </div>

                    <span className="text-[10px] font-medium text-emerald-400 px-2.5 py-1 rounded-lg bg-emerald-950/60 border border-emerald-800/60 shrink-0 group-hover:bg-emerald-600 group-hover:text-white transition-all">
                      Chat
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {tab === 'group' && (
          <form onSubmit={handleCreateGroup} className="flex flex-col gap-3 py-1">
            <div className="flex flex-col gap-1">
              <label className="text-[11px] font-medium text-slate-300">Group Subject</label>
              <input
                type="text"
                placeholder="e.g. SAT Harare Developers, Global Security Team..."
                value={groupName}
                onChange={(e) => setGroupName(e.target.value)}
                required
                className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-emerald-500"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-[11px] font-medium text-slate-300">Description (Optional)</label>
              <input
                type="text"
                placeholder="Topic or guidelines for members..."
                value={groupDesc}
                onChange={(e) => setGroupDesc(e.target.value)}
                className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-emerald-500"
              />
            </div>

            {/* Select Members to Add */}
            <div className="flex flex-col gap-1.5">
              <span className="text-[11px] font-medium text-slate-300">
                Select Members ({selectedGroupMembers.length} selected)
              </span>
              <div className="max-h-36 overflow-y-auto divide-y divide-slate-800/60 bg-slate-950 rounded-xl border border-slate-800 p-1">
                {APP_USERS_DIRECTORY.map((user) => {
                  const isSelected = selectedGroupMembers.includes(user.userId);
                  return (
                    <div
                      key={user.userId}
                      onClick={() => toggleGroupMemberSelection(user.userId)}
                      className="p-1.5 flex items-center justify-between hover:bg-slate-900 rounded-lg cursor-pointer"
                    >
                      <div className="flex items-center gap-2">
                        <img src={user.photoURL} alt="" className="w-6 h-6 rounded-full object-cover" />
                        <div className="flex flex-col">
                          <span className="text-xs text-slate-200">{user.displayName}</span>
                          <span className="text-[9px] text-slate-400">{user.email}</span>
                        </div>
                      </div>
                      <div
                        className={`w-4 h-4 rounded border flex items-center justify-center transition-colors ${
                          isSelected
                            ? 'bg-emerald-600 border-emerald-500 text-white'
                            : 'border-slate-700 bg-slate-900'
                        }`}
                      >
                        {isSelected && <Check className="w-3 h-3" />}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <button
              type="submit"
              className="mt-1 w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg transition-all"
            >
              Create Encrypted Group (with Admin Controls)
            </button>
          </form>
        )}

        {tab === 'community' && (
          <div className="flex flex-col gap-2 py-1">
            <span className="text-[11px] text-slate-400">Join worldwide open community hubs:</span>
            <div className="flex flex-col gap-2">
              <div
                onClick={() =>
                  handleJoinCommunity(
                    'SAT Global Community Worldwide 🌍',
                    'Worldwide SAT Messenger community forum',
                    'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=256&q=80'
                  )
                }
                className="p-3 bg-slate-950 hover:bg-slate-800/80 rounded-xl border border-slate-800 cursor-pointer transition-all flex items-center justify-between"
              >
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-full bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 font-bold text-xs">
                    SAT
                  </div>
                  <div className="flex flex-col">
                    <span className="text-xs font-semibold text-slate-200">SAT Global Community Worldwide</span>
                    <span className="text-[10px] text-slate-400">Official general discussion</span>
                  </div>
                </div>
                <span className="text-[10px] text-emerald-400 px-2 py-0.5 rounded bg-emerald-950/60 border border-emerald-800/50">
                  Join
                </span>
              </div>

              <div
                onClick={() =>
                  handleJoinCommunity(
                    'Cybersecurity & E2EE Research Hub 🔐',
                    'Zero-trust encryption protocols & privacy research',
                    'https://images.unsplash.com/photo-1563986768609-322da13575f3?auto=format&fit=crop&w=256&q=80'
                  )
                }
                className="p-3 bg-slate-950 hover:bg-slate-800/80 rounded-xl border border-slate-800 cursor-pointer transition-all flex items-center justify-between"
              >
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-full bg-cyan-500/20 border border-cyan-500/40 flex items-center justify-center text-cyan-400 font-bold text-xs">
                    E2E
                  </div>
                  <div className="flex flex-col">
                    <span className="text-xs font-semibold text-slate-200">Cybersecurity & E2EE Research</span>
                    <span className="text-[10px] text-slate-400">Cryptographic privacy council</span>
                  </div>
                </div>
                <span className="text-[10px] text-cyan-400 px-2 py-0.5 rounded bg-cyan-950/60 border border-cyan-800/50">
                  Join
                </span>
              </div>
            </div>
          </div>
        )}

        {/* External Bridge Integrations */}
        <div className="pt-2 border-t border-slate-800 flex flex-col gap-2">
          {onOpenWhatsAppLink && (
            <button
              onClick={() => {
                onClose();
                onOpenWhatsAppLink();
              }}
              className="w-full flex items-center justify-center gap-2 p-2 rounded-xl bg-slate-950 hover:bg-slate-800/80 border border-emerald-500/40 text-xs text-emerald-400 font-medium transition-colors"
            >
              <MessageCircle className="w-3.5 h-3.5" />
              <span>Link WhatsApp Account (Zambia 🇿🇲, Zimbabwe 🇿🇼 & Worldwide)</span>
            </button>
          )}

          <button
            onClick={() => {
              onClose();
              onSyncGoogleChat();
            }}
            className="w-full flex items-center justify-center gap-2 p-2 rounded-xl bg-slate-950 hover:bg-slate-800/80 border border-cyan-500/30 text-xs text-cyan-400 font-medium transition-colors"
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>Connect & Sync Google Chat (Links to SAT Limited)</span>
          </button>
        </div>
      </div>
    </div>
  );
};
