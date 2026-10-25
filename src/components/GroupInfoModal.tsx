import React, { useState } from 'react';
import {
  Users,
  Shield,
  ShieldCheck,
  ShieldAlert,
  UserPlus,
  UserMinus,
  Edit3,
  Check,
  X,
  Crown,
  Lock,
} from 'lucide-react';
import { Conversation, UserProfile } from '../types';
import { APP_USERS_DIRECTORY } from '../services/mockData';

interface GroupInfoModalProps {
  isOpen: boolean;
  onClose: () => void;
  conversation: Conversation;
  currentUser: UserProfile;
  onUpdateConversation: (updated: Conversation) => void;
}

export const GroupInfoModal: React.FC<GroupInfoModalProps> = ({
  isOpen,
  onClose,
  conversation,
  currentUser,
  onUpdateConversation,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [name, setName] = useState(conversation.name);
  const [description, setDescription] = useState(conversation.description || '');
  const [isAddingMember, setIsAddingMember] = useState(false);
  const [memberSearch, setMemberSearch] = useState('');

  if (!isOpen) return null;

  const adminIds = conversation.adminIds || [conversation.creatorId];
  const isCurrentUserAdmin = adminIds.includes(currentUser.userId) || conversation.creatorId === currentUser.userId;

  // Find all members in the directory or fallback
  const membersList: UserProfile[] = conversation.memberIds.map((mId) => {
    if (mId === currentUser.userId) return currentUser;
    const found = APP_USERS_DIRECTORY.find((u) => u.userId === mId);
    if (found) return found;
    return {
      userId: mId,
      displayName: mId.replace('user-', '').replace('-', ' '),
      email: `${mId}@satmessenger.org`,
      isOnline: false,
    };
  });

  const availableToAdd = APP_USERS_DIRECTORY.filter(
    (u) =>
      !conversation.memberIds.includes(u.userId) &&
      u.userId !== currentUser.userId &&
      (u.displayName.toLowerCase().includes(memberSearch.toLowerCase()) ||
        u.email.toLowerCase().includes(memberSearch.toLowerCase()))
  );

  const handleSaveDetails = () => {
    if (!name.trim()) return;
    const updated: Conversation = {
      ...conversation,
      name: name.trim(),
      description: description.trim(),
      updatedAt: Date.now(),
    };
    onUpdateConversation(updated);
    setIsEditing(false);
  };

  const handleToggleAdmin = (targetUserId: string) => {
    if (!isCurrentUserAdmin) return;
    let nextAdmins: string[];
    if (adminIds.includes(targetUserId)) {
      // Demote admin (cannot demote creator)
      if (targetUserId === conversation.creatorId) {
        alert('Group creator cannot be demoted from admin role.');
        return;
      }
      nextAdmins = adminIds.filter((id) => id !== targetUserId);
    } else {
      // Promote to admin
      nextAdmins = [...adminIds, targetUserId];
    }

    const updated: Conversation = {
      ...conversation,
      adminIds: nextAdmins,
      updatedAt: Date.now(),
    };
    onUpdateConversation(updated);
  };

  const handleRemoveMember = (targetUserId: string) => {
    if (!isCurrentUserAdmin) return;
    if (targetUserId === conversation.creatorId) {
      alert('Cannot remove the group creator.');
      return;
    }

    const nextMembers = conversation.memberIds.filter((id) => id !== targetUserId);
    const nextAdmins = adminIds.filter((id) => id !== targetUserId);

    const updated: Conversation = {
      ...conversation,
      memberIds: nextMembers,
      adminIds: nextAdmins,
      updatedAt: Date.now(),
    };
    onUpdateConversation(updated);
  };

  const handleAddMember = (user: UserProfile) => {
    if (!isCurrentUserAdmin) return;
    const nextMembers = [...conversation.memberIds, user.userId];
    const updated: Conversation = {
      ...conversation,
      memberIds: nextMembers,
      updatedAt: Date.now(),
    };
    onUpdateConversation(updated);
    setIsAddingMember(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-sm animate-fade-in text-slate-100">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-2xl flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400">
              <Users className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-semibold text-sm text-slate-200">Group Info & Admin Controls</h3>
              <p className="text-[10px] text-slate-400">
                {conversation.memberIds.length} Participants • {adminIds.length} Admins
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Group Profile Hero */}
        <div className="flex flex-col items-center gap-2.5 p-4 bg-slate-950/70 rounded-xl border border-slate-800 text-center relative">
          <div className="relative">
            <img
              src={conversation.avatar || 'https://api.dicebear.com/7.x/identicon/svg?seed=group'}
              alt={conversation.name}
              className="w-16 h-16 rounded-full object-cover ring-2 ring-emerald-500/40 bg-slate-800 shadow"
            />
            <div className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-purple-600 border-2 border-slate-900 flex items-center justify-center text-[9px] text-white">
              <Lock className="w-2.5 h-2.5" />
            </div>
          </div>

          {isEditing ? (
            <div className="w-full flex flex-col gap-2 mt-1">
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Group Subject"
                className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-white text-center focus:border-emerald-500"
              />
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Group Description"
                rows={2}
                className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-300 text-center focus:border-emerald-500"
              />
              <div className="flex justify-center gap-2 mt-1">
                <button
                  onClick={() => setIsEditing(false)}
                  className="px-3 py-1 rounded-lg text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSaveDetails}
                  className="flex items-center gap-1 px-3 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-xs text-white"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>Save</span>
                </button>
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center">
              <div className="flex items-center gap-1.5">
                <h4 className="font-bold text-sm text-slate-100">{conversation.name}</h4>
                {isCurrentUserAdmin && (
                  <button
                    onClick={() => setIsEditing(true)}
                    className="p-1 text-slate-400 hover:text-emerald-400"
                    title="Edit Group Info"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
              <p className="text-[11px] text-slate-400 mt-0.5 max-w-xs">
                {conversation.description || 'SAT Messenger end-to-end encrypted group'}
              </p>
            </div>
          )}
        </div>

        {/* Admin Status Notice */}
        <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-[11px] flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span className="text-slate-300">
              {isCurrentUserAdmin ? 'You are a Group Admin' : 'Group Admin privileges required to edit settings'}
            </span>
          </div>
          {isCurrentUserAdmin && (
            <span className="text-[9px] font-mono px-2 py-0.5 rounded bg-emerald-950 text-emerald-400 border border-emerald-800">
              Admin Powers Active
            </span>
          )}
        </div>

        {/* Member Management Section */}
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-300">
            <span>Participants ({membersList.length})</span>
            {isCurrentUserAdmin && !isAddingMember && (
              <button
                onClick={() => setIsAddingMember(true)}
                className="flex items-center gap-1 text-[11px] text-emerald-400 hover:text-emerald-300"
              >
                <UserPlus className="w-3.5 h-3.5" />
                <span>Add Member</span>
              </button>
            )}
          </div>

          {/* Add Member Tray */}
          {isAddingMember && (
            <div className="p-3 bg-slate-950 rounded-xl border border-emerald-500/30 flex flex-col gap-2 animate-fade-in">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-semibold text-emerald-400">Add from App Directory</span>
                <button onClick={() => setIsAddingMember(false)} className="text-slate-400 hover:text-white">
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
              <input
                type="text"
                placeholder="Search registered contacts..."
                value={memberSearch}
                onChange={(e) => setMemberSearch(e.target.value)}
                className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-white"
              />
              <div className="max-h-32 overflow-y-auto divide-y divide-slate-800/80">
                {availableToAdd.length === 0 ? (
                  <p className="text-[10px] text-slate-500 py-2 text-center">No additional contacts available</p>
                ) : (
                  availableToAdd.map((u) => (
                    <div
                      key={u.userId}
                      className="py-1.5 flex items-center justify-between hover:bg-slate-900/60 px-1 rounded cursor-pointer"
                      onClick={() => handleAddMember(u)}
                    >
                      <div className="flex items-center gap-2">
                        <img src={u.photoURL} alt="" className="w-6 h-6 rounded-full object-cover" />
                        <div className="flex flex-col">
                          <span className="text-xs text-slate-200">{u.displayName}</span>
                          <span className="text-[9px] text-slate-400">{u.email}</span>
                        </div>
                      </div>
                      <span className="text-[10px] text-emerald-400 font-semibold px-2 py-0.5 bg-emerald-950 rounded border border-emerald-800">
                        Add
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* Members List */}
          <div className="flex flex-col gap-1.5 divide-y divide-slate-800/60 max-h-56 overflow-y-auto pr-1">
            {membersList.map((member) => {
              const isCreator = member.userId === conversation.creatorId;
              const isAdmin = adminIds.includes(member.userId);
              const isSelf = member.userId === currentUser.userId;

              return (
                <div
                  key={member.userId}
                  className="pt-1.5 pb-1 flex items-center justify-between text-xs group"
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <img
                      src={
                        member.photoURL ||
                        `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(
                          member.displayName
                        )}`
                      }
                      alt={member.displayName}
                      className="w-9 h-9 rounded-full object-cover bg-slate-800 shrink-0"
                    />
                    <div className="flex flex-col min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="font-medium text-slate-200 truncate">
                          {member.displayName}
                        </span>
                        {isSelf && <span className="text-[9px] text-slate-500">(You)</span>}
                      </div>
                      <span className="text-[10px] text-slate-400 truncate">{member.email}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    {/* Admin Badges */}
                    {isCreator ? (
                      <span className="flex items-center gap-1 text-[9px] font-bold px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/30">
                        <Crown className="w-2.5 h-2.5 text-amber-400" /> Creator
                      </span>
                    ) : isAdmin ? (
                      <span className="flex items-center gap-1 text-[9px] font-bold px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-300 border border-emerald-500/30">
                        <Shield className="w-2.5 h-2.5 text-emerald-400" /> Admin
                      </span>
                    ) : null}

                    {/* Admin Actions Dropdown / Buttons */}
                    {isCurrentUserAdmin && !isSelf && (
                      <div className="flex items-center gap-1 ml-1 opacity-90 sm:opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={() => handleToggleAdmin(member.userId)}
                          className={`p-1 rounded text-[10px] transition-colors ${
                            isAdmin
                              ? 'text-amber-400 hover:bg-amber-950/60'
                              : 'text-emerald-400 hover:bg-emerald-950/60'
                          }`}
                          title={isAdmin ? 'Dismiss as Admin' : 'Make Group Admin'}
                        >
                          {isAdmin ? 'Demote' : 'Make Admin'}
                        </button>
                        {!isCreator && (
                          <button
                            onClick={() => handleRemoveMember(member.userId)}
                            className="p-1 rounded text-rose-400 hover:bg-rose-950/60"
                            title="Remove from Group"
                          >
                            <UserMinus className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Close Button */}
        <div className="flex justify-end pt-2 border-t border-slate-800">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
