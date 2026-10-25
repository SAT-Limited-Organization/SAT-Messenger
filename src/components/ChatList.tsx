import React, { useState } from 'react';
import {
  Search,
  Plus,
  Shield,
  Cloud,
  Settings,
  Flame,
  Pin,
  Check,
  CheckCheck,
  Globe,
  Users,
  MessageSquare,
  MessageCircle,
  Lock,
  Sparkles,
} from 'lucide-react';
import { Conversation, RoomType, UserProfile } from '../types';

interface ChatListProps {
  conversations: Conversation[];
  selectedConversationId: string | null;
  onSelectConversation: (id: string) => void;
  onOpenNewChat: () => void;
  onOpenCloudBackup: () => void;
  onOpenSettings: () => void;
  onOpenWhatsAppLink?: () => void;
  activeFilter: RoomType | 'all';
  onChangeFilter: (filter: RoomType | 'all') => void;
  currentUser: UserProfile;
  typingUsers: Record<string, string>; // conversationId -> userName typing
}

export const ChatList: React.FC<ChatListProps> = ({
  conversations,
  selectedConversationId,
  onSelectConversation,
  onOpenNewChat,
  onOpenCloudBackup,
  onOpenSettings,
  onOpenWhatsAppLink,
  activeFilter,
  onChangeFilter,
  currentUser,
  typingUsers,
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  const filteredConversations = conversations.filter((c) => {
    const matchesFilter =
      activeFilter === 'all'
        ? true
        : activeFilter === 'direct'
        ? c.type === 'direct'
        : activeFilter === 'group'
        ? c.type === 'group'
        : activeFilter === 'community'
        ? c.type === 'community'
        : activeFilter === 'google_chat'
        ? c.type === 'google_chat'
        : c.type === 'whatsapp';

    const matchesSearch =
      c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (c.lastMessageSnippet && c.lastMessageSnippet.toLowerCase().includes(searchQuery.toLowerCase()));

    return matchesFilter && matchesSearch;
  });

  const formatChatTime = (timestamp?: number) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const now = new Date();
    if (date.toDateString() === now.toDateString()) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  };

  return (
    <div className="w-full h-full flex flex-col bg-slate-950/95 border-r border-slate-800/80 select-none">
      {/* Top App Header */}
      <div className="p-3.5 pb-2 border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-md">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="relative">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-400 p-0.5 shadow-md shadow-emerald-950/50">
                <div className="w-full h-full rounded-[10px] bg-slate-950 flex items-center justify-center font-black text-sm tracking-tighter text-emerald-400">
                  SAT
                </div>
              </div>
              <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full bg-emerald-500 border-2 border-slate-950" />
            </div>
            <div className="flex flex-col">
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-sm text-slate-100 tracking-tight">SAT Messenger</span>
                <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  E2EE
                </span>
              </div>
              <span className="text-[10px] text-slate-400 truncate max-w-[190px]">
                The SAT Limited Org • Mr Simbarashe Tembo
              </span>
            </div>
          </div>

          <div className="flex items-center gap-1">
            <button
              onClick={onOpenWhatsAppLink}
              className={`p-2 rounded-xl transition-colors relative ${
                currentUser.whatsappLinked
                  ? 'text-emerald-400 bg-emerald-500/10 border border-emerald-500/30'
                  : 'text-slate-400 hover:text-emerald-400 hover:bg-slate-800/60'
              }`}
              title={
                currentUser.whatsappLinked
                  ? `Linked WhatsApp: ${currentUser.whatsappLinked.phoneNumber}`
                  : 'Link WhatsApp Account'
              }
            >
              <MessageCircle className="w-4 h-4" />
              {currentUser.whatsappLinked && (
                <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-emerald-400 ring-2 ring-slate-950" />
              )}
            </button>
            <button
              onClick={onOpenCloudBackup}
              className="p-2 rounded-xl text-slate-400 hover:text-cyan-400 hover:bg-slate-800/60 transition-colors"
              title="Cloud Backup & Device Sync"
            >
              <Cloud className="w-4 h-4" />
            </button>
            <button
              onClick={onOpenSettings}
              className="p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 transition-colors"
              title="Settings & Themes"
            >
              <Settings className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Search Bar */}
        <div className="mt-3 relative">
          <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Search encrypted chats or messages..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-8 pr-3 py-1.5 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-emerald-500/80 transition-all"
          />
        </div>

        {/* Category Tabs */}
        <div className="flex items-center gap-1.5 mt-2.5 overflow-x-auto no-scrollbar pb-1 text-xs">
          <button
            onClick={() => onChangeFilter('all')}
            className={`px-3 py-1 rounded-lg font-medium transition-all text-xs whitespace-nowrap ${
              activeFilter === 'all'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 bg-slate-900/60'
            }`}
          >
            All
          </button>
          <button
            onClick={() => onChangeFilter('direct')}
            className={`px-3 py-1 rounded-lg font-medium transition-all text-xs whitespace-nowrap ${
              activeFilter === 'direct'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 bg-slate-900/60'
            }`}
          >
            Chats
          </button>
          <button
            onClick={() => onChangeFilter('group')}
            className={`px-3 py-1 rounded-lg font-medium transition-all text-xs whitespace-nowrap ${
              activeFilter === 'group'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 bg-slate-900/60'
            }`}
          >
            Groups
          </button>
          <button
            onClick={() => onChangeFilter('community')}
            className={`px-3 py-1 rounded-lg font-medium transition-all text-xs whitespace-nowrap ${
              activeFilter === 'community'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 bg-slate-900/60'
            }`}
          >
            Communities
          </button>
          <button
            onClick={() => onChangeFilter('whatsapp')}
            className={`px-3 py-1 rounded-lg font-medium transition-all text-xs whitespace-nowrap flex items-center gap-1.5 ${
              activeFilter === 'whatsapp'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-emerald-400 bg-slate-900/60'
            }`}
          >
            <MessageCircle className="w-3 h-3 text-emerald-400" />
            <span>WhatsApp</span>
            {currentUser.whatsappLinked && (
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            )}
          </button>
          <button
            onClick={() => onChangeFilter('google_chat')}
            className={`px-3 py-1 rounded-lg font-medium transition-all text-xs whitespace-nowrap flex items-center gap-1 ${
              activeFilter === 'google_chat'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 bg-slate-900/60'
            }`}
          >
            <MessageSquare className="w-3 h-3 text-cyan-400" />
            <span>Google Chat</span>
          </button>
        </div>
      </div>

      {/* Conversation List */}
      <div className="flex-1 overflow-y-auto divide-y divide-slate-900/80">
        {filteredConversations.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500 flex flex-col items-center justify-center gap-2">
            <Lock className="w-8 h-8 text-slate-700" />
            <span>No conversations found in this category.</span>
            <button
              onClick={onOpenNewChat}
              className="mt-2 px-3 py-1.5 rounded-lg bg-emerald-600/30 text-emerald-300 border border-emerald-500/40 text-xs hover:bg-emerald-600/50 transition-all"
            >
              Start New Chat
            </button>
          </div>
        ) : (
          filteredConversations.map((c) => {
            const isSelected = selectedConversationId === c.id;
            const isTyping = Boolean(typingUsers[c.id]);

            return (
              <div
                key={c.id}
                onClick={() => onSelectConversation(c.id)}
                className={`flex items-center gap-3 p-3 cursor-pointer transition-all ${
                  isSelected
                    ? 'bg-slate-900/90 border-l-4 border-l-emerald-500'
                    : 'hover:bg-slate-900/40'
                }`}
              >
                {/* Avatar */}
                <div className="relative shrink-0">
                  <img
                    src={c.avatar || 'https://api.dicebear.com/7.x/bottts/svg?seed=SAT'}
                    alt={c.name}
                    className="w-11 h-11 rounded-full object-cover bg-slate-800 ring-1 ring-slate-700"
                  />
                  {c.type === 'community' && (
                    <div className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-cyan-600 flex items-center justify-center text-[8px] text-white">
                      <Globe className="w-2.5 h-2.5" />
                    </div>
                  )}
                  {c.type === 'group' && (
                    <div className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-purple-600 flex items-center justify-center text-[8px] text-white">
                      <Users className="w-2.5 h-2.5" />
                    </div>
                  )}
                  {c.type === 'google_chat' && (
                    <div className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-blue-600 flex items-center justify-center text-[8px] text-white">
                      <MessageSquare className="w-2.5 h-2.5" />
                    </div>
                  )}
                  {c.type === 'whatsapp' && (
                    <div className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-emerald-600 flex items-center justify-center text-[8px] text-white ring-1 ring-slate-950">
                      <MessageCircle className="w-2.5 h-2.5" />
                    </div>
                  )}
                  {c.id === 'conv-sat-ai' && (
                    <div className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-gradient-to-tr from-cyan-600 to-emerald-500 flex items-center justify-center text-[8px] text-white ring-1 ring-slate-950 shadow-sm">
                      <Sparkles className="w-2.5 h-2.5" />
                    </div>
                  )}
                </div>

                {/* Details */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-0.5">
                    <span className="font-semibold text-xs text-slate-200 truncate flex items-center gap-1.5">
                      {c.name}
                      {c.id === 'conv-sat-ai' && (
                        <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-cyan-950/90 text-cyan-300 border border-cyan-800/60 font-bold flex items-center gap-0.5">
                          <Sparkles className="w-2 h-2 text-cyan-400" />
                          <span>AI</span>
                        </span>
                      )}
                      {c.isPinned && <Pin className="w-2.5 h-2.5 text-emerald-400 rotate-45 shrink-0" />}
                      {c.disappearingTimerSeconds ? (
                        <span title="Disappearing messages on">
                          <Flame className="w-2.5 h-2.5 text-amber-400 shrink-0" />
                        </span>
                      ) : null}
                    </span>
                    <span className="text-[10px] text-slate-500 shrink-0">
                      {formatChatTime(c.lastMessageTimestamp)}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-xs">
                    {isTyping ? (
                      <span className="text-[11px] text-emerald-400 font-medium animate-pulse flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                        {typingUsers[c.id]} is typing...
                      </span>
                    ) : (
                      <p className="text-[11px] text-slate-400 truncate max-w-[210px]">
                        {c.lastMessageSnippet || 'Encrypted conversation started'}
                      </p>
                    )}

                    {c.unreadCount && c.unreadCount > 0 ? (
                      <span className="ml-2 px-1.5 py-0.2 rounded-full bg-emerald-500 text-slate-950 font-bold text-[10px] shrink-0">
                        {c.unreadCount}
                      </span>
                    ) : null}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Floating Action Button (New Chat) */}
      <div className="p-3 border-t border-slate-900 bg-slate-950/80 flex items-center justify-between">
        <div className="flex items-center gap-1.5 text-[10px] text-slate-500">
          <Shield className="w-3 h-3 text-emerald-500" />
          <span>AES-256 E2EE Enabled</span>
        </div>

        <button
          onClick={onOpenNewChat}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg shadow-emerald-950/50 transition-all hover:scale-105 active:scale-95"
        >
          <Plus className="w-4 h-4" />
          <span>New Chat</span>
        </button>
      </div>
    </div>
  );
};
