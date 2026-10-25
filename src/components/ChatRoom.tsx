import React, { useState, useEffect, useRef } from 'react';
import {
  ArrowLeft,
  Shield,
  Phone,
  Video,
  MoreVertical,
  Paperclip,
  Mic,
  Send,
  Smile,
  Check,
  CheckCheck,
  Clock,
  Flame,
  FileText,
  Download,
  MapPin,
  ExternalLink,
  Play,
  Pause,
  Sparkles,
  Lock,
  Eye,
  Info,
  Users,
  ShieldCheck,
  MessageCircle,
} from 'lucide-react';
import { Conversation, Message, ThemeConfig, UserProfile } from '../types';
import { GroupInfoModal } from './GroupInfoModal';
import { MessageInfoModal } from './MessageInfoModal';
import { APP_USERS_DIRECTORY } from '../services/mockData';
import { openWhatsAppWebChat } from '../services/whatsappBridge';

interface ChatRoomProps {
  conversation: Conversation;
  messages: Message[];
  currentUser: UserProfile;
  theme: ThemeConfig;
  onBack: () => void;
  onSendMessage: (text: string) => void;
  onOpenVoiceRecorder: () => void;
  onOpenMediaShare: () => void;
  onOpenDisappearingTimer: () => void;
  onOpenEncryptionDetails: () => void;
  isOtherTyping: boolean;
  typingUserName?: string;
  onTypingHeartbeat: () => void;
  onToggleReaction: (messageId: string, emoji: string) => void;
  onUpdateConversation?: (updated: Conversation) => void;
  onDeleteMessage?: (msgId: string) => void;
}

export const ChatRoom: React.FC<ChatRoomProps> = ({
  conversation,
  messages,
  currentUser,
  theme,
  onBack,
  onSendMessage,
  onOpenVoiceRecorder,
  onOpenMediaShare,
  onOpenDisappearingTimer,
  onOpenEncryptionDetails,
  isOtherTyping,
  typingUserName,
  onTypingHeartbeat,
  onToggleReaction,
  onUpdateConversation,
}) => {
  const [inputText, setInputText] = useState('');
  const [playingAudioId, setPlayingAudioId] = useState<string | null>(null);
  const [audioProgress, setAudioProgress] = useState<Record<string, number>>({});
  const [inspectedMessage, setInspectedMessage] = useState<Message | null>(null);
  const [activeCall, setActiveCall] = useState<'voice' | 'video' | null>(null);
  const [activeReactionMessageId, setActiveReactionMessageId] = useState<string | null>(null);
  const [showMoreEmojisFor, setShowMoreEmojisFor] = useState<string | null>(null);
  const [isGroupInfoOpen, setIsGroupInfoOpen] = useState(false);
  const [selectedReceiptMessage, setSelectedReceiptMessage] = useState<Message | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const audioElementsRef = useRef<Record<string, HTMLAudioElement>>({});
  const longPressTimerRef = useRef<any>(null);

  const isGroupOrCommunity =
    conversation.type === 'group' || conversation.type === 'community';

  // Helper to resolve reader user profiles
  const getReadersList = (msg: Message): UserProfile[] => {
    const readerIds = (msg.readBy || []).filter((id) => id !== msg.senderId);
    return readerIds.map((id) => {
      if (id === currentUser.userId) return currentUser;
      const found = APP_USERS_DIRECTORY.find((u) => u.userId === id);
      if (found) return found;
      return {
        userId: id,
        displayName: id.replace('user-', '').replace('-', ' '),
        email: `${id}@satmessenger.org`,
        isOnline: false,
      } as UserProfile;
    });
  };

  const getReadersSummaryText = (msg: Message): string => {
    const readers = getReadersList(msg);
    if (readers.length === 0) return '';
    if (readers.length === 1) {
      return readers[0].userId === currentUser.userId
        ? 'You'
        : readers[0].displayName.split(' ')[0] || readers[0].displayName;
    }
    if (readers.length === 2) {
      const name1 =
        readers[0].userId === currentUser.userId
          ? 'You'
          : readers[0].displayName.split(' ')[0];
      const name2 =
        readers[1].userId === currentUser.userId
          ? 'You'
          : readers[1].displayName.split(' ')[0];
      return `${name1} & ${name2}`;
    }
    const name1 =
      readers[0].userId === currentUser.userId
        ? 'You'
        : readers[0].displayName.split(' ')[0];
    return `${name1} & ${readers.length - 1} others`;
  };

  // Auto scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isOtherTyping]);

  // Click outside listener to dismiss reaction popup
  useEffect(() => {
    const handleClickOutside = () => {
      setActiveReactionMessageId(null);
      setShowMoreEmojisFor(null);
    };
    window.addEventListener('click', handleClickOutside);
    return () => window.removeEventListener('click', handleClickOutside);
  }, []);

  const handlePressStart = (msgId: string) => {
    longPressTimerRef.current = setTimeout(() => {
      setActiveReactionMessageId(msgId);
    }, 450);
  };

  const handlePressEnd = () => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
    }
  };

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;
    onSendMessage(inputText.trim());
    setInputText('');
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputText(e.target.value);
    onTypingHeartbeat();
  };

  const togglePlayAudio = (msg: Message) => {
    if (!msg.mediaData) {
      // Synthesize tone or preview
      return;
    }

    if (playingAudioId === msg.id) {
      audioElementsRef.current[msg.id]?.pause();
      setPlayingAudioId(null);
    } else {
      if (playingAudioId && audioElementsRef.current[playingAudioId]) {
        audioElementsRef.current[playingAudioId].pause();
      }

      if (!audioElementsRef.current[msg.id]) {
        const audio = new Audio(msg.mediaData);
        audio.onended = () => {
          setPlayingAudioId(null);
          setAudioProgress((prev) => ({ ...prev, [msg.id]: 0 }));
        };
        audio.ontimeupdate = () => {
          const pct = (audio.currentTime / (audio.duration || 1)) * 100;
          setAudioProgress((prev) => ({ ...prev, [msg.id]: pct }));
        };
        audioElementsRef.current[msg.id] = audio;
      }

      audioElementsRef.current[msg.id].play();
      setPlayingAudioId(msg.id);
    }
  };

  const formatMessageTime = (epochMs: number) => {
    return new Date(epochMs).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div
      className="w-full h-full flex flex-col relative select-none"
      style={{ backgroundColor: theme.bgDark }}
    >
      {/* Chat Room Top Bar */}
      <header
        className="px-3 py-2.5 flex items-center justify-between border-b z-20 shadow-md backdrop-blur-md"
        style={{
          backgroundColor: theme.bgCard,
          borderColor: theme.border,
        }}
      >
        <div className="flex items-center gap-2 min-w-0">
          <button
            onClick={onBack}
            className="p-1.5 -ml-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>

          <div
            className="relative shrink-0 cursor-pointer"
            onClick={() => {
              if (conversation.type === 'group') setIsGroupInfoOpen(true);
              else onOpenEncryptionDetails();
            }}
          >
            <img
              src={conversation.avatar || 'https://api.dicebear.com/7.x/bottts/svg?seed=SAT'}
              alt={conversation.name}
              className="w-10 h-10 rounded-full object-cover ring-1 ring-slate-700 bg-slate-800"
            />
            <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full bg-emerald-500 border-2 border-slate-900" />
          </div>

          <div
            className="flex flex-col min-w-0 cursor-pointer"
            onClick={() => {
              if (conversation.type === 'group') setIsGroupInfoOpen(true);
              else onOpenEncryptionDetails();
            }}
          >
            <span className="font-semibold text-xs text-slate-100 truncate flex items-center gap-1.5">
              {conversation.name}
              {conversation.id === 'conv-sat-ai' && (
                <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-cyan-950/80 text-cyan-300 border border-cyan-800/60 flex items-center gap-1">
                  <Sparkles className="w-2.5 h-2.5 text-cyan-400" />
                  <span>SAT AI Assistant</span>
                </span>
              )}
              {conversation.type === 'group' && (
                <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-purple-950/80 text-purple-300 border border-purple-800/60">
                  {conversation.memberIds.length} members
                </span>
              )}
              {conversation.type === 'whatsapp' && (
                <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-emerald-950/80 text-emerald-300 border border-emerald-800/60 flex items-center gap-1">
                  <MessageCircle className="w-2.5 h-2.5 text-emerald-400" />
                  <span>WhatsApp Bridge</span>
                </span>
              )}
            </span>
            <div className="flex items-center gap-1.5 text-[10px] text-slate-400">
              {isOtherTyping ? (
                <span className="text-emerald-400 font-medium animate-pulse">
                  {typingUserName || 'typing...'}
                </span>
              ) : conversation.id === 'conv-sat-ai' ? (
                <span className="flex items-center gap-1 text-cyan-400/90 font-medium">
                  <Sparkles className="w-2.5 h-2.5 text-cyan-400" /> Official AI of The SAT Limited Organization
                </span>
              ) : conversation.type === 'group' ? (
                <span className="flex items-center gap-1 text-purple-300 hover:text-purple-200">
                  <Users className="w-2.5 h-2.5" /> Tap for group info & admins
                </span>
              ) : conversation.type === 'whatsapp' ? (
                <span className="flex items-center gap-1 text-emerald-400/90">
                  <MessageCircle className="w-2.5 h-2.5" /> Synchronized with WhatsApp multi-device
                </span>
              ) : (
                <span className="flex items-center gap-1 text-emerald-400/90">
                  <Shield className="w-2.5 h-2.5" /> End-to-End Encrypted
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Action icons */}
        <div className="flex items-center gap-1 text-slate-300">
          {/* WhatsApp Native Relay Trigger */}
          {conversation.type === 'whatsapp' && conversation.whatsappPhoneNumber && (
            <button
              onClick={() => openWhatsAppWebChat(conversation.whatsappPhoneNumber!)}
              className="p-2 rounded-xl hover:bg-slate-800/80 text-emerald-400 hover:text-emerald-300 transition-colors"
              title={`Open in native WhatsApp (${conversation.whatsappPhoneNumber})`}
            >
              <ExternalLink className="w-4 h-4" />
            </button>
          )}

          {/* Group Info & Admins Trigger */}
          {conversation.type === 'group' && (
            <button
              onClick={() => setIsGroupInfoOpen(true)}
              className="p-2 rounded-xl hover:bg-slate-800/80 text-purple-400 hover:text-purple-300 transition-colors"
              title="Group Info & Admins"
            >
              <Users className="w-4 h-4" />
            </button>
          )}

          {/* Disappearing Message Badge button */}
          <button
            onClick={onOpenDisappearingTimer}
            className={`p-2 rounded-xl transition-all flex items-center gap-1 ${
              conversation.disappearingTimerSeconds
                ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                : 'hover:bg-slate-800/80 text-slate-400'
            }`}
            title="Configure Disappearing Timer"
          >
            <Flame className="w-4 h-4" />
            {conversation.disappearingTimerSeconds ? (
              <span className="text-[10px] font-mono font-bold">
                {conversation.disappearingTimerSeconds >= 3600
                  ? `${conversation.disappearingTimerSeconds / 3600}h`
                  : `${conversation.disappearingTimerSeconds}s`}
              </span>
            ) : null}
          </button>

          {/* Secure Calls */}
          <button
            onClick={() => setActiveCall('video')}
            className="p-2 rounded-xl hover:bg-slate-800/80 text-slate-400 hover:text-cyan-400 transition-colors"
            title="E2EE Video Call"
          >
            <Video className="w-4 h-4" />
          </button>
          <button
            onClick={() => setActiveCall('voice')}
            className="p-2 rounded-xl hover:bg-slate-800/80 text-slate-400 hover:text-emerald-400 transition-colors"
            title="E2EE Voice Call"
          >
            <Phone className="w-4 h-4" />
          </button>
          <button
            onClick={onOpenEncryptionDetails}
            className="p-2 rounded-xl hover:bg-slate-800/80 text-slate-400 hover:text-slate-200 transition-colors"
            title="Verify Encryption"
          >
            <MoreVertical className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-3 sm:p-4 space-y-3 relative">
        {/* E2EE WhatsApp-Style Safety Notice */}
        <div
          onClick={onOpenEncryptionDetails}
          className="mx-auto max-w-sm p-2.5 rounded-xl border cursor-pointer hover:scale-[1.01] transition-transform text-center flex flex-col items-center gap-1 bg-slate-950/70 border-emerald-500/30 text-[11px] text-slate-300 shadow-sm"
        >
          <div className="flex items-center gap-1.5 text-emerald-400 font-semibold text-xs">
            <Lock className="w-3.5 h-3.5" />
            <span>End-to-End Encrypted Communication</span>
          </div>
          <p className="text-[10px] text-slate-400 leading-normal">
            Messages and calls are end-to-end encrypted with 256-bit AES-GCM. No one outside of this chat, not even the SAT Limited Organization, can read or listen to them. Tap to verify.
          </p>
        </div>

        {/* Message Items */}
        {messages.map((msg) => {
          const isOwn = msg.senderId === currentUser.userId;

          return (
            <div
              key={msg.id}
              className={`flex flex-col ${isOwn ? 'items-end' : 'items-start'} group`}
            >
              {/* Sender name for groups/communities */}
              {!isOwn && (conversation.type === 'group' || conversation.type === 'community') && (
                <span className="text-[10px] font-semibold text-emerald-400 ml-1 mb-0.5">
                  {msg.senderName}
                </span>
              )}

              <div className="relative">
                {/* Floating Reaction Pill (WhatsApp style) */}
                {activeReactionMessageId === msg.id && (
                  <div
                    onClick={(e) => e.stopPropagation()}
                    className={`absolute -top-11 ${
                      isOwn ? 'right-0' : 'left-0'
                    } z-40 flex items-center gap-1 p-1 bg-slate-900/95 backdrop-blur-md rounded-full border border-slate-700 shadow-2xl animate-fade-in`}
                  >
                    {['👍', '❤️', '😂', '😮', '😢', '🙏', '🔐'].map((emoji) => {
                      const isReacted = msg.reactions?.[emoji]?.includes(currentUser.userId);
                      return (
                        <button
                          key={emoji}
                          onClick={(e) => {
                            e.stopPropagation();
                            onToggleReaction(msg.id, emoji);
                            setActiveReactionMessageId(null);
                            setShowMoreEmojisFor(null);
                          }}
                          className={`w-7 h-7 sm:w-8 sm:h-8 rounded-full flex items-center justify-center text-sm sm:text-base hover:scale-125 transition-transform active:scale-95 ${
                            isReacted ? 'bg-emerald-500/30 ring-1 ring-emerald-400' : 'hover:bg-slate-800'
                          }`}
                          title={`React with ${emoji}`}
                        >
                          {emoji}
                        </button>
                      );
                    })}

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setShowMoreEmojisFor(showMoreEmojisFor === msg.id ? null : msg.id);
                      }}
                      className="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                      title="More emojis"
                    >
                      +
                    </button>
                  </div>
                )}

                {/* More Emojis Extended Menu */}
                {activeReactionMessageId === msg.id && showMoreEmojisFor === msg.id && (
                  <div
                    onClick={(e) => e.stopPropagation()}
                    className={`absolute -top-22 ${
                      isOwn ? 'right-0' : 'left-0'
                    } z-50 flex items-center gap-1 p-1.5 bg-slate-950/95 backdrop-blur-md rounded-2xl border border-slate-700 shadow-2xl`}
                  >
                    {['🔥', '🎉', '🚀', '⚡', '🤝', '💯', '💪', '✨'].map((emoji) => {
                      const isReacted = msg.reactions?.[emoji]?.includes(currentUser.userId);
                      return (
                        <button
                          key={emoji}
                          onClick={(e) => {
                            e.stopPropagation();
                            onToggleReaction(msg.id, emoji);
                            setActiveReactionMessageId(null);
                            setShowMoreEmojisFor(null);
                          }}
                          className={`w-7 h-7 rounded-full flex items-center justify-center text-sm hover:scale-125 transition-transform ${
                            isReacted ? 'bg-emerald-500/30 ring-1 ring-emerald-400' : 'hover:bg-slate-800'
                          }`}
                        >
                          {emoji}
                        </button>
                      );
                    })}
                  </div>
                )}

                {/* Message Bubble Container with Long-press & Right-click Listeners */}
                <div className="flex items-center gap-1.5">
                  {/* Left hover reaction trigger for own messages */}
                  {isOwn && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setActiveReactionMessageId(activeReactionMessageId === msg.id ? null : msg.id);
                      }}
                      className="opacity-0 group-hover:opacity-100 transition-opacity p-1.5 rounded-full bg-slate-900/80 hover:bg-slate-800 text-slate-400 hover:text-emerald-400 shadow-sm"
                      title="Add reaction (or long-press)"
                    >
                      <Smile className="w-3.5 h-3.5" />
                    </button>
                  )}

                  <div
                    onTouchStart={() => handlePressStart(msg.id)}
                    onTouchEnd={handlePressEnd}
                    onTouchMove={handlePressEnd}
                    onMouseDown={() => handlePressStart(msg.id)}
                    onMouseUp={handlePressEnd}
                    onMouseLeave={handlePressEnd}
                    onContextMenu={(e) => {
                      e.preventDefault();
                      setActiveReactionMessageId(msg.id);
                    }}
                    className={`relative max-w-[85%] sm:max-w-[75%] rounded-2xl px-3 py-2 shadow-sm text-xs transition-all leading-relaxed select-text cursor-pointer ${
                      isOwn ? 'rounded-tr-xs text-white' : 'rounded-tl-xs text-slate-200'
                    }`}
                    style={{
                      backgroundColor: isOwn ? theme.bgBubbleOwn : theme.bgBubbleOther,
                    }}
                  >
                    {/* Disappearing Message Flame Tag */}
                    {msg.disappearingAfterSeconds ? (
                      <div className="flex items-center gap-1 text-[9px] font-mono text-amber-300 mb-1 pb-1 border-b border-white/10">
                        <Flame className="w-3 h-3 text-amber-400 animate-pulse" />
                        <span>Disappearing message active</span>
                      </div>
                    ) : null}

                    {/* 1. TEXT MESSAGE */}
                    {msg.type === 'text' && (
                      <p className="whitespace-pre-wrap break-words">{msg.decryptedContent}</p>
                    )}

                    {/* 2. VOICE NOTE / AUDIO MESSAGE */}
                    {msg.type === 'audio' && (
                      <div className="flex flex-col gap-1.5 min-w-[200px]">
                        <div className="flex items-center gap-2.5">
                          <button
                            onClick={() => togglePlayAudio(msg)}
                            className="w-9 h-9 rounded-full bg-emerald-500 hover:bg-emerald-400 text-slate-950 flex items-center justify-center shrink-0 transition-colors shadow"
                          >
                            {playingAudioId === msg.id ? (
                              <Pause className="w-4 h-4 fill-current" />
                            ) : (
                              <Play className="w-4 h-4 fill-current ml-0.5" />
                            )}
                          </button>

                          {/* Waveform Bar Simulation */}
                          <div className="flex-1 flex items-center gap-0.5 h-6">
                            {Array.from({ length: 18 }).map((_, idx) => (
                              <div
                                key={idx}
                                className={`flex-1 rounded-full transition-all ${
                                  (audioProgress[msg.id] || 0) > (idx / 18) * 100
                                    ? 'bg-emerald-400'
                                    : 'bg-slate-600/70'
                                }`}
                                style={{
                                  height: `${25 + ((idx * 7) % 65)}%`,
                                }}
                              />
                            ))}
                          </div>

                          <span className="text-[10px] font-mono text-slate-400 shrink-0">
                            0:{msg.audioDuration ? String(msg.audioDuration).padStart(2, '0') : '14'}
                          </span>
                        </div>

                        {/* Speech-to-Text Transcription Box */}
                        {msg.transcription && (
                          <div className="mt-1 p-2 rounded-lg bg-black/30 border border-white/10 text-[10px] text-slate-300 flex flex-col gap-0.5">
                            <div className="flex items-center gap-1 text-[9px] font-semibold text-emerald-400">
                              <Sparkles className="w-2.5 h-2.5" />
                              <span>Voice Transcription</span>
                            </div>
                            <p className="italic text-slate-300 leading-tight">
                              "{msg.transcription}"
                            </p>
                          </div>
                        )}
                      </div>
                    )}

                    {/* 3. DOCUMENT ATTACHMENT */}
                    {msg.type === 'document' && (
                      <div className="flex items-center gap-3 p-2 rounded-xl bg-black/25 border border-white/10 min-w-[190px]">
                        <div className="w-10 h-10 rounded-lg bg-rose-500/20 border border-rose-500/40 flex items-center justify-center text-rose-400 shrink-0">
                          <FileText className="w-5 h-5" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="font-semibold text-xs truncate text-slate-100">
                            {msg.mediaName || 'Encrypted_Document.pdf'}
                          </p>
                          <span className="text-[10px] text-slate-400 font-mono">
                            {msg.mediaSize ? `${(msg.mediaSize / 1024 / 1024).toFixed(1)} MB` : '1.2 MB'} • PDF
                          </span>
                        </div>
                        <a
                          href={msg.mediaData || '#'}
                          download={msg.mediaName || 'document.pdf'}
                          className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white transition-colors"
                          title="Download Document"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </a>
                      </div>
                    )}

                    {/* 4. PHOTO / MEDIA */}
                    {msg.type === 'image' && (
                      <div className="flex flex-col gap-1.5">
                        <img
                          src={msg.mediaData}
                          alt="Encrypted attachment"
                          className="max-h-56 rounded-xl object-cover shadow border border-white/10"
                        />
                        {msg.decryptedContent && (
                          <p className="whitespace-pre-wrap">{msg.decryptedContent}</p>
                        )}
                      </div>
                    )}

                    {/* 5. LOCATION PIN */}
                    {msg.type === 'location' && msg.location && (
                      <div className="flex flex-col gap-2 min-w-[210px] p-1">
                        <div className="flex items-center gap-1.5 text-rose-400 font-semibold text-xs">
                          <MapPin className="w-4 h-4 fill-rose-500 text-slate-900" />
                          <span>{msg.location.name}</span>
                        </div>
                        {msg.location.address && (
                          <p className="text-[10px] text-slate-300 -mt-1">{msg.location.address}</p>
                        )}
                        <div className="h-24 rounded-lg bg-slate-900/90 border border-white/15 relative overflow-hidden flex items-center justify-center">
                          <div className="absolute inset-0 opacity-25 bg-[radial-gradient(#10b981_1px,transparent_1px)] [background-size:10px_10px]" />
                          <div className="z-10 flex flex-col items-center">
                            <MapPin className="w-6 h-6 text-rose-500 fill-rose-500 animate-bounce" />
                            <span className="text-[9px] font-mono text-emerald-400 mt-1">
                              {msg.location.latitude}, {msg.location.longitude}
                            </span>
                          </div>
                        </div>
                        <a
                          href={`https://maps.google.com/?q=${msg.location.latitude},${msg.location.longitude}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center justify-center gap-1 text-[10px] text-cyan-300 font-medium py-1 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
                        >
                          <ExternalLink className="w-3 h-3" />
                          <span>Open in Maps</span>
                        </a>
                      </div>
                    )}

                    {/* Message Meta: Timestamp & Read Receipts */}
                    <div className="flex items-center justify-end gap-1 mt-1 text-[9px] text-slate-300/80 font-mono select-none">
                      {/* Inspect Ciphertext button */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setInspectedMessage(msg);
                        }}
                        className="opacity-0 group-hover:opacity-100 transition-opacity mr-1 text-slate-400 hover:text-emerald-300"
                        title="Audit Ciphertext & Keys"
                      >
                        <Eye className="w-3 h-3" />
                      </button>

                      {/* Info / Read Receipts trigger button */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedReceiptMessage(msg);
                        }}
                        className="opacity-0 group-hover:opacity-100 transition-opacity mr-0.5 text-slate-400 hover:text-cyan-400"
                        title="Message read receipts & delivery info"
                      >
                        <Info className="w-3 h-3" />
                      </button>

                      <span>{formatMessageTime(msg.createdAt)}</span>

                      {/* Double Checkmark indicators */}
                      {isOwn ? (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedReceiptMessage(msg);
                          }}
                          className="ml-0.5 inline-flex items-center hover:scale-115 transition-transform cursor-pointer"
                          title={
                            msg.status === 'read'
                              ? isGroupOrCommunity
                                ? `Read by ${msg.readBy.filter((id) => id !== msg.senderId).length} member${msg.readBy.filter((id) => id !== msg.senderId).length !== 1 ? 's' : ''} (Click to see who)`
                                : 'Read (Double-check)'
                              : msg.status === 'delivered'
                              ? 'Delivered (Double-check)'
                              : 'Sent (Single-check)'
                          }
                        >
                          {msg.status === 'sent' && (
                            <Check className="w-3.5 h-3.5 text-slate-400" />
                          )}
                          {msg.status === 'delivered' && (
                            <CheckCheck className="w-3.5 h-3.5 text-slate-400" />
                          )}
                          {msg.status === 'read' && (
                            <CheckCheck className="w-3.5 h-3.5 text-cyan-400 drop-shadow-[0_0_6px_rgba(34,211,238,0.6)]" />
                          )}
                        </button>
                      ) : (
                        /* In group chats, also show double-check for incoming messages if read */
                        isGroupOrCommunity && msg.readBy && msg.readBy.length > 0 && (
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedReceiptMessage(msg);
                            }}
                            className="ml-0.5 inline-flex items-center text-cyan-400/80 hover:text-cyan-300 hover:scale-115 transition-transform cursor-pointer"
                            title="Message read receipts (Click to see who read)"
                          >
                            <CheckCheck className="w-3.5 h-3.5 text-cyan-400" />
                          </button>
                        )
                      )}
                    </div>
                  </div>

                  {/* Right hover reaction trigger for incoming messages */}
                  {!isOwn && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setActiveReactionMessageId(activeReactionMessageId === msg.id ? null : msg.id);
                      }}
                      className="opacity-0 group-hover:opacity-100 transition-opacity p-1.5 rounded-full bg-slate-900/80 hover:bg-slate-800 text-slate-400 hover:text-emerald-400 shadow-sm"
                      title="Add reaction (or long-press)"
                    >
                      <Smile className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>

                {/* Reaction Badges Pill (grouped by emoji) */}
                {msg.reactions && Object.keys(msg.reactions).length > 0 && (
                  <div
                    className={`flex flex-wrap items-center gap-1 mt-1 px-1 ${
                      isOwn ? 'justify-end' : 'justify-start'
                    }`}
                  >
                    {Object.entries(msg.reactions).map(([emoji, userIds]) => {
                      if (!userIds || userIds.length === 0) return null;
                      const hasReacted = userIds.includes(currentUser.userId);
                      return (
                        <button
                          key={emoji}
                          onClick={(e) => {
                            e.stopPropagation();
                            onToggleReaction(msg.id, emoji);
                          }}
                          className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium border shadow-sm transition-all active:scale-90 ${
                            hasReacted
                              ? 'bg-emerald-950/90 border-emerald-500/80 text-emerald-300 ring-1 ring-emerald-500/40'
                              : 'bg-slate-900/90 border-slate-700/80 text-slate-300 hover:bg-slate-800/90'
                          }`}
                          title={`${userIds.length} reaction${
                            userIds.length > 1 ? 's' : ''
                          } • Tap to ${hasReacted ? 'remove' : 'add'}`}
                        >
                          <span>{emoji}</span>
                          {userIds.length > 1 && (
                            <span className="font-mono text-[9px] font-bold text-slate-400">
                              {userIds.length}
                            </span>
                          )}
                        </button>
                      );
                    })}
                  </div>
                )}

                {/* Specific Users Read Badge in Group Chats */}
                {isGroupOrCommunity &&
                  msg.readBy &&
                  msg.readBy.filter((id) => id !== msg.senderId).length > 0 && (
                    <div
                      className={`flex items-center mt-1 px-1 ${
                        isOwn ? 'justify-end' : 'justify-start'
                      }`}
                    >
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedReceiptMessage(msg);
                        }}
                        className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-slate-900/90 hover:bg-slate-800 border border-slate-700/60 hover:border-cyan-500/50 text-[10px] text-slate-300 hover:text-cyan-300 transition-all shadow-sm group/receipt cursor-pointer"
                        title="Click to view full read receipts and timestamps"
                      >
                        <CheckCheck className="w-3 h-3 text-cyan-400 shrink-0 drop-shadow-[0_0_4px_rgba(34,211,238,0.5)]" />

                        {/* Reader Avatars Cluster */}
                        <div className="flex items-center -space-x-1.5 overflow-hidden py-0.2">
                          {getReadersList(msg)
                            .slice(0, 3)
                            .map((reader) => (
                              <img
                                key={reader.userId}
                                src={
                                  reader.photoURL ||
                                  `https://api.dicebear.com/7.x/bottts/svg?seed=${reader.userId}`
                                }
                                alt={reader.displayName}
                                className="w-3.5 h-3.5 rounded-full object-cover ring-1 ring-slate-900 bg-slate-800"
                              />
                            ))}
                        </div>

                        <span className="font-medium text-slate-300 group-hover/receipt:text-cyan-200">
                          Read by{' '}
                          <strong className="font-semibold text-cyan-300">
                            {getReadersSummaryText(msg)}
                          </strong>
                        </span>
                      </button>
                    </div>
                  )}
              </div>
            </div>
          );
        })}

        {/* Real-time typing bubble */}
        {isOtherTyping && (
          <div className="flex items-center gap-1.5 p-2 px-3 rounded-2xl rounded-tl-xs bg-slate-900 border border-slate-800 text-xs text-slate-400 w-fit">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-bounce" />
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-bounce [animation-delay:0.2s]" />
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-bounce [animation-delay:0.4s]" />
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Chat Input Bar */}
      <form
        onSubmit={handleSend}
        className="p-2 sm:p-3 border-t z-20 backdrop-blur-md flex items-center gap-2"
        style={{
          backgroundColor: theme.bgCard,
          borderColor: theme.border,
        }}
      >
        <button
          type="button"
          onClick={onOpenMediaShare}
          className="p-2 rounded-xl text-slate-400 hover:text-emerald-400 hover:bg-slate-800/60 transition-colors"
          title="Share document, photo or location"
        >
          <Paperclip className="w-4 h-4" />
        </button>

        <div className="flex-1 relative">
          <input
            type="text"
            placeholder="Type encrypted message..."
            value={inputText}
            onChange={handleInputChange}
            className="w-full px-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-2xl text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-emerald-500/80 transition-all pr-8"
          />
          <button
            type="button"
            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
            onClick={() => setInputText((prev) => prev + ' 🔐')}
          >
            <Smile className="w-4 h-4" />
          </button>
        </div>

        {inputText.trim() ? (
          <button
            type="submit"
            className="p-2.5 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-950/50 transition-all hover:scale-105 active:scale-95"
            title="Send"
          >
            <Send className="w-4 h-4" />
          </button>
        ) : (
          <button
            type="button"
            onClick={onOpenVoiceRecorder}
            className="p-2.5 rounded-2xl bg-emerald-600/20 hover:bg-emerald-600 text-emerald-400 hover:text-white border border-emerald-500/40 shadow-sm transition-all"
            title="Record Voice Note (Speech-to-Text Transcription)"
          >
            <Mic className="w-4 h-4" />
          </button>
        )}
      </form>

      {/* Ciphertext Audit Modal Sheet */}
      {inspectedMessage && (
        <div className="absolute inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 max-w-sm w-full flex flex-col gap-3 text-xs text-slate-200">
            <div className="flex items-center justify-between border-b border-slate-800 pb-2">
              <div className="flex items-center gap-1.5 text-emerald-400 font-semibold">
                <Shield className="w-4 h-4" />
                <span>E2EE Cryptographic Audit</span>
              </div>
              <button
                onClick={() => setInspectedMessage(null)}
                className="text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>

            <div className="space-y-2 font-mono text-[10px]">
              <div>
                <span className="text-slate-400">Decrypted Content:</span>
                <p className="text-white bg-slate-950 p-2 rounded border border-slate-800 mt-1">
                  {inspectedMessage.decryptedContent || '(Media attachment)'}
                </p>
              </div>

              <div>
                <span className="text-slate-400">Raw Wire Ciphertext (AES-GCM):</span>
                <p className="text-emerald-400 bg-black p-2 rounded border border-slate-800 mt-1 break-all select-all">
                  {inspectedMessage.ciphertext}
                </p>
              </div>

              <div>
                <span className="text-slate-400">Initialization Vector (IV):</span>
                <p className="text-cyan-400 bg-black p-1.5 rounded border border-slate-800 mt-0.5 break-all">
                  {inspectedMessage.iv}
                </p>
              </div>
            </div>

            <p className="text-[10px] text-slate-400 leading-normal">
              This verifies that plaintext never leaves the browser. The database stores strictly unreadable ciphertext.
            </p>
          </div>
        </div>
      )}

      {/* Simulated Secure Call Overlay */}
      {activeCall && (
        <div className="absolute inset-0 z-50 bg-slate-950/95 backdrop-blur-md flex flex-col items-center justify-between p-8 text-center text-slate-100">
          <div className="flex flex-col items-center gap-2 mt-8">
            <div className="w-20 h-20 rounded-full p-1 bg-gradient-to-tr from-emerald-500 to-teal-400 animate-pulse">
              <img
                src={conversation.avatar}
                alt={conversation.name}
                className="w-full h-full rounded-full object-cover"
              />
            </div>
            <h3 className="font-bold text-base text-white mt-2">{conversation.name}</h3>
            <span className="text-xs text-emerald-400 flex items-center gap-1 font-mono">
              <Shield className="w-3 h-3" /> End-to-End Encrypted {activeCall === 'video' ? 'Video' : 'Voice'} Call
            </span>
            <span className="text-[11px] text-slate-400 font-mono">Ringing...</span>
          </div>

          <div className="flex items-center gap-6">
            <button
              onClick={() => setActiveCall(null)}
              className="w-14 h-14 rounded-full bg-rose-600 hover:bg-rose-500 text-white flex items-center justify-center shadow-lg shadow-rose-900/50 transition-transform active:scale-90"
              title="End Call"
            >
              <Phone className="w-6 h-6 rotate-[135deg]" />
            </button>
          </div>
        </div>
      )}

      {/* Group Info & Admin Controls Modal */}
      {conversation.type === 'group' && onUpdateConversation && (
        <GroupInfoModal
          isOpen={isGroupInfoOpen}
          onClose={() => setIsGroupInfoOpen(false)}
          conversation={conversation}
          currentUser={currentUser}
          onUpdateConversation={onUpdateConversation}
        />
      )}

      {/* Message Info & Read Receipts Modal */}
      {selectedReceiptMessage && (
        <MessageInfoModal
          isOpen={Boolean(selectedReceiptMessage)}
          onClose={() => setSelectedReceiptMessage(null)}
          message={selectedReceiptMessage}
          conversation={conversation}
          currentUser={currentUser}
        />
      )}
    </div>
  );
};
