import React from 'react';
import {
  Check,
  CheckCheck,
  Clock,
  Shield,
  X,
  Users,
  Eye,
  FileText,
  MapPin,
  Mic,
  Calendar,
  Lock,
} from 'lucide-react';
import { Conversation, Message, UserProfile } from '../types';
import { APP_USERS_DIRECTORY } from '../services/mockData';

interface MessageInfoModalProps {
  isOpen: boolean;
  onClose: () => void;
  message: Message | null;
  conversation: Conversation;
  currentUser: UserProfile;
}

export const MessageInfoModal: React.FC<MessageInfoModalProps> = ({
  isOpen,
  onClose,
  message,
  conversation,
  currentUser,
}) => {
  if (!isOpen || !message) return null;

  // Build directory lookup map
  const allKnownUsers: Record<string, UserProfile> = {
    [currentUser.userId]: currentUser,
    ...Object.fromEntries(APP_USERS_DIRECTORY.map((u) => [u.userId, u])),
  };

  const isGroupOrCommunity =
    conversation.type === 'group' || conversation.type === 'community';

  // Target member list for this conversation
  const totalGroupMembers = conversation.memberIds
    .filter((id) => id !== message.senderId && id !== 'community-all')
    .map((id) => {
      if (allKnownUsers[id]) return allKnownUsers[id];
      return {
        userId: id,
        email: `${id}@satmessenger.org`,
        displayName: id.replace('user-', '').replace('-', ' '),
        isOnline: false,
      } as UserProfile;
    });

  // Extract readers
  const readBySet = new Set(message.readBy || []);
  const readReceiptMap = new Map<string, number>();

  if (message.readReceipts) {
    message.readReceipts.forEach((r) => {
      readReceiptMap.set(r.userId, r.readAt);
      readBySet.add(r.userId);
    });
  }

  // Filter out the sender from the readers list
  const readerIds = Array.from(readBySet).filter((id) => id !== message.senderId);

  const readersList = readerIds.map((id) => {
    const profile = allKnownUsers[id] || {
      userId: id,
      displayName: id.replace('user-', '').replace('-', ' '),
      email: `${id}@satmessenger.org`,
      isOnline: false,
    };
    const readAt = readReceiptMap.get(id) || message.createdAt + 1000 * 45;
    return {
      profile,
      readAt,
    };
  });

  // Delivered but not yet read members (for group chats)
  const deliveredList = totalGroupMembers.filter(
    (member) => !readBySet.has(member.userId)
  );

  const formatTimestamp = (ts: number) => {
    const date = new Date(ts);
    const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const isToday = new Date().toDateString() === date.toDateString();
    return isToday ? `Today at ${timeStr}` : `${date.toLocaleDateString([], { month: 'short', day: 'numeric' })} at ${timeStr}`;
  };

  const readCount = readersList.length;
  const totalExpected = Math.max(totalGroupMembers.length, readCount);
  const readPercentage =
    totalExpected > 0 ? Math.round((readCount / totalExpected) * 100) : 100;

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 animate-fade-in"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="bg-slate-950 border border-slate-800 rounded-3xl w-full max-w-md shadow-2xl flex flex-col max-h-[85vh] overflow-hidden animate-scale-up"
      >
        {/* Header */}
        <div className="p-4 border-b border-slate-800/80 bg-slate-900/60 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
              <CheckCheck className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-slate-100 flex items-center gap-1.5">
                <span>Message Read Receipts</span>
                <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-cyan-500/10 text-cyan-400 border border-cyan-500/30">
                  {message.status === 'read' ? 'READ' : 'DELIVERED'}
                </span>
              </h3>
              <p className="text-[11px] text-slate-400">
                {conversation.name}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Scroll Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {/* Message Preview Box */}
          <div className="p-3 rounded-2xl bg-slate-900/80 border border-slate-800/90 text-xs">
            <div className="flex items-center justify-between mb-1.5 text-[10px] text-slate-400 font-mono">
              <span className="text-emerald-400 font-medium">
                Sent by {message.senderName}
              </span>
              <span>{formatTimestamp(message.createdAt)}</span>
            </div>

            {message.type === 'text' && (
              <p className="text-slate-200 line-clamp-3 leading-relaxed">
                {message.decryptedContent}
              </p>
            )}

            {message.type === 'audio' && (
              <div className="flex items-center gap-2 text-slate-300">
                <Mic className="w-4 h-4 text-emerald-400" />
                <span>Voice Note ({message.audioDuration || 14}s)</span>
                {message.transcription && (
                  <span className="text-[10px] text-slate-400 truncate italic">
                    "{message.transcription}"
                  </span>
                )}
              </div>
            )}

            {message.type === 'document' && (
              <div className="flex items-center gap-2 text-slate-300">
                <FileText className="w-4 h-4 text-rose-400" />
                <span className="truncate">{message.mediaName || 'Document attachment'}</span>
              </div>
            )}

            {message.type === 'location' && (
              <div className="flex items-center gap-2 text-slate-300">
                <MapPin className="w-4 h-4 text-rose-500" />
                <span>{message.location?.name || 'Shared Location'}</span>
              </div>
            )}

            {message.type === 'image' && (
              <div className="flex items-center gap-2">
                <img
                  src={message.mediaData}
                  alt="Attachment preview"
                  className="w-12 h-12 rounded-lg object-cover border border-slate-700"
                />
                <span className="text-slate-300">Photo Attachment</span>
              </div>
            )}
          </div>

          {/* Group Delivery / Read Status Meter */}
          {isGroupOrCommunity && totalExpected > 0 && (
            <div className="p-3 rounded-2xl bg-slate-900/40 border border-slate-800/70">
              <div className="flex items-center justify-between text-xs mb-1.5">
                <div className="flex items-center gap-1.5 text-slate-300 font-medium">
                  <CheckCheck className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Read by {readCount} of {totalExpected} members</span>
                </div>
                <span className="text-[11px] font-mono text-cyan-400 font-bold">
                  {readPercentage}%
                </span>
              </div>
              <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-emerald-500 to-cyan-400 transition-all duration-500 rounded-full"
                  style={{ width: `${Math.max(readPercentage, 8)}%` }}
                />
              </div>
            </div>
          )}

          {/* 1. READ BY SECTION (Explicit list of users who have read the message) */}
          <div className="space-y-2">
            <div className="flex items-center justify-between px-1">
              <span className="text-xs font-semibold text-slate-200 flex items-center gap-1.5">
                <CheckCheck className="w-3.5 h-3.5 text-cyan-400" />
                <span>Read by ({readersList.length})</span>
              </span>
              <span className="text-[10px] text-slate-400">
                Double Cyan Checkmark Confirmed
              </span>
            </div>

            {readersList.length === 0 ? (
              <div className="p-4 rounded-xl bg-slate-900/30 border border-slate-800/60 text-center text-xs text-slate-500">
                <span>No members have read this message yet.</span>
              </div>
            ) : (
              <div className="divide-y divide-slate-900 rounded-2xl bg-slate-900/50 border border-slate-800/80 overflow-hidden">
                {readersList.map(({ profile, readAt }) => {
                  const isCurrent = profile.userId === currentUser.userId;
                  return (
                    <div
                      key={profile.userId}
                      className="p-3 flex items-center justify-between gap-3 hover:bg-slate-800/30 transition-colors"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="relative shrink-0">
                          <img
                            src={
                              profile.photoURL ||
                              `https://api.dicebear.com/7.x/bottts/svg?seed=${profile.userId}`
                            }
                            alt={profile.displayName}
                            className="w-9 h-9 rounded-full object-cover ring-1 ring-slate-700 bg-slate-800"
                          />
                          {profile.isOnline && (
                            <span className="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-slate-950" />
                          )}
                        </div>
                        <div className="min-w-0">
                          <p className="font-semibold text-xs text-slate-200 truncate flex items-center gap-1.5">
                            <span>{profile.displayName}</span>
                            {isCurrent && (
                              <span className="text-[9px] px-1.5 py-0.2 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                You
                              </span>
                            )}
                          </p>
                          <p className="text-[10px] text-slate-400 truncate">
                            {profile.email || profile.bio || 'Member'}
                          </p>
                        </div>
                      </div>

                      <div className="flex flex-col items-end shrink-0 gap-0.5">
                        <div className="flex items-center gap-1 text-cyan-400 text-xs font-mono">
                          <CheckCheck className="w-3.5 h-3.5" />
                          <span className="text-[10px] font-semibold">Read</span>
                        </div>
                        <span className="text-[10px] text-slate-400 font-mono">
                          {formatTimestamp(readAt)}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* 2. DELIVERED TO SECTION (Members who haven't read yet) */}
          {isGroupOrCommunity && deliveredList.length > 0 && (
            <div className="space-y-2 pt-1">
              <div className="flex items-center justify-between px-1">
                <span className="text-xs font-semibold text-slate-400 flex items-center gap-1.5">
                  <CheckCheck className="w-3.5 h-3.5 text-slate-400" />
                  <span>Delivered to ({deliveredList.length})</span>
                </span>
                <span className="text-[10px] text-slate-500">
                  Awaiting Member Open
                </span>
              </div>

              <div className="divide-y divide-slate-900 rounded-2xl bg-slate-900/30 border border-slate-800/60 overflow-hidden">
                {deliveredList.map((member) => (
                  <div
                    key={member.userId}
                    className="p-2.5 px-3 flex items-center justify-between gap-3 hover:bg-slate-800/20 transition-colors"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <img
                        src={
                          member.photoURL ||
                          `https://api.dicebear.com/7.x/bottts/svg?seed=${member.userId}`
                        }
                        alt={member.displayName}
                        className="w-8 h-8 rounded-full object-cover ring-1 ring-slate-800 bg-slate-800 shrink-0 opacity-80"
                      />
                      <div className="min-w-0">
                        <p className="font-medium text-xs text-slate-300 truncate">
                          {member.displayName}
                        </p>
                        <p className="text-[10px] text-slate-500 truncate">
                          {member.email}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-1 text-slate-500 text-[10px] font-mono">
                      <CheckCheck className="w-3 h-3 text-slate-500" />
                      <span>Delivered</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Security & Cryptography Assurance Footer */}
          <div className="p-3 rounded-2xl bg-emerald-950/20 border border-emerald-500/20 flex items-start gap-2.5 text-[10px] text-slate-300">
            <Lock className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              Read receipts in SAT Messenger are cryptographically signed. Receipts are generated only after client-side decryption on verified recipient devices.
            </p>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="p-3 border-t border-slate-800/80 bg-slate-900/40 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
