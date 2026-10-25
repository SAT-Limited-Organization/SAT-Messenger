/**
 * SAT Messenger
 * Developed for The SAT Limited Organization
 * Founded by Mr Simbarashe Tembo
 * Package Name: com.sat_commuity.satmessenger
 */

import React, { useState, useEffect, useRef } from 'react';
import { AndroidFrame } from './components/AndroidFrame';
import { ChatList } from './components/ChatList';
import { ChatRoom } from './components/ChatRoom';
import { VoiceRecorderModal } from './components/VoiceRecorderModal';
import { MediaShareModal } from './components/MediaShareModal';
import { DisappearingTimerModal } from './components/DisappearingTimerModal';
import { EncryptionDetailsModal } from './components/EncryptionDetailsModal';
import { CloudBackupModal } from './components/CloudBackupModal';
import { SettingsModal } from './components/SettingsModal';
import { NewChatModal } from './components/NewChatModal';
import { WhatsAppLinkModal } from './components/WhatsAppLinkModal';
import { GoogleRegistrationModal } from './components/GoogleRegistrationModal';
import { generateSatAiResponse } from './services/aiService';

import {
  Conversation,
  LocationData,
  Message,
  RoomType,
  ThemeId,
  UserProfile,
  WhatsAppLinkInfo,
} from './types';
import { THEMES } from './services/themes';
import { getSampleWhatsAppChats } from './services/whatsappBridge';
import {
  INITIAL_CONVERSATIONS,
  INITIAL_MESSAGES,
  FOUNDER_PROFILE,
  APP_USERS_DIRECTORY,
} from './services/mockData';
import {
  encryptPayload,
  decryptPayload,
  generateSafetyFingerprint,
} from './services/crypto';
import {
  initAuth,
  signInWithGoogle,
  logoutUser,
  testConnection,
  getCachedAccessToken,
  db,
  doc,
  updateDoc,
  setDoc,
  handleFirestoreError,
  OperationType,
} from './services/firebase';
import { fetchGoogleChatSpaces } from './services/googleChat';

export default function App() {
  // Theme state
  const [currentThemeId, setCurrentThemeId] = useState<ThemeId>('cyber-midnight');
  const currentTheme = THEMES[currentThemeId];

  // Auth & Profile state
  const [currentUser, setCurrentUser] = useState<UserProfile>({
    userId: 'current-user',
    email: 'user@satmessenger.org',
    displayName: 'Augustus (SAT Member)',
    photoURL: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=128&q=80',
    bio: 'Secured with SAT Messenger E2EE Cryptography',
    isOnline: true,
  });
  const [isGoogleAuth, setIsGoogleAuth] = useState(false);

  // Conversations & Messages
  const [conversations, setConversations] = useState<Conversation[]>(INITIAL_CONVERSATIONS);
  const [selectedConvId, setSelectedConvId] = useState<string | null>('conv-simbarashe-tembo');
  const [messages, setMessages] = useState<Record<string, Message[]>>(INITIAL_MESSAGES);
  const [activeFilter, setActiveFilter] = useState<RoomType | 'all'>('all');

  // Typing indicators: conversationId -> userName
  const [typingUsers, setTypingUsers] = useState<Record<string, string>>({});
  const typingTimeoutRef = useRef<any>(null);

  // Modals state
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);
  const [isMediaModalOpen, setIsMediaModalOpen] = useState(false);
  const [isTimerModalOpen, setIsTimerModalOpen] = useState(false);
  const [isEncryptionModalOpen, setIsEncryptionModalOpen] = useState(false);
  const [isBackupModalOpen, setIsBackupModalOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [isNewChatModalOpen, setIsNewChatModalOpen] = useState(false);
  const [isWhatsAppModalOpen, setIsWhatsAppModalOpen] = useState(false);
  const [isGoogleRegModalOpen, setIsGoogleRegModalOpen] = useState(false);

  // Verify connection to Firestore on boot
  useEffect(() => {
    testConnection();

    // Listen to Firebase Auth state
    const unsubscribe = initAuth(
      (user, token) => {
        setIsGoogleAuth(true);
        setCurrentUser((prev) => ({
          ...prev,
          userId: user.uid,
          email: user.email || 'user@satmessenger.org',
          displayName: user.displayName || 'Augustus',
          photoURL: user.photoURL || prev.photoURL,
        }));
      },
      () => {
        setIsGoogleAuth(false);
      }
    );

    return () => unsubscribe();
  }, []);

  // Disappearing messages lifecycle cleaner
  useEffect(() => {
    const interval = setInterval(() => {
      const now = Date.now();
      setMessages((prev) => {
        let changed = false;
        const updated: Record<string, Message[]> = {};

        for (const [convId, list] of Object.entries(prev)) {
          const filtered = list.filter((m) => {
            if (m.expiresAt && m.expiresAt < now) {
              changed = true;
              return false;
            }
            return true;
          });
          updated[convId] = filtered;
        }

        return changed ? updated : prev;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  // Mark messages as read when selecting a conversation
  useEffect(() => {
    if (!selectedConvId) return;

    setMessages((prev) => {
      const convMsgs = prev[selectedConvId];
      if (!convMsgs) return prev;

      let changed = false;
      const now = Date.now();
      const updatedList = convMsgs.map((m) => {
        const notReadByMe = !m.readBy?.includes(currentUser.userId);
        if (m.senderId !== currentUser.userId && notReadByMe) {
          changed = true;
          const newReadBy = [...(m.readBy || []), currentUser.userId];
          const newReceipts = [
            ...(m.readReceipts || []),
            {
              userId: currentUser.userId,
              userName: currentUser.displayName,
              userAvatar: currentUser.photoURL,
              readAt: now,
            },
          ];
          return {
            ...m,
            status: 'read' as const,
            readBy: newReadBy,
            readReceipts: newReceipts,
          };
        }
        return m;
      });

      if (!changed) return prev;
      return { ...prev, [selectedConvId]: updatedList };
    });

    // Clear unread count on conversation
    setConversations((prev) =>
      prev.map((c) => (c.id === selectedConvId ? { ...c, unreadCount: 0 } : c))
    );
  }, [selectedConvId, currentUser.userId, currentUser.displayName, currentUser.photoURL]);

  // Selected conversation object
  const currentConversation = conversations.find((c) => c.id === selectedConvId);

  // Simulate realistic group members reading incoming messages with live double-checkmark updates
  const simulateGroupReads = (convId: string, messageId: string) => {
    const targetConv = conversations.find((c) => c.id === convId);
    if (!targetConv) return;

    const otherMembers = targetConv.memberIds.filter(
      (mId) => mId !== currentUser.userId && mId !== 'community-all'
    );
    if (otherMembers.length === 0) return;

    // First member reads after 1.2s -> status becomes 'read' with cyan double-check
    setTimeout(() => {
      const reader1Id = otherMembers[0];
      const reader1Profile = APP_USERS_DIRECTORY.find((u) => u.userId === reader1Id);
      const now1 = Date.now();

      setMessages((prev) => {
        const convMsgs = prev[convId];
        if (!convMsgs) return prev;
        return {
          ...prev,
          [convId]: convMsgs.map((m) => {
            if (m.id === messageId) {
              const currentReadBy = m.readBy || [];
              if (!currentReadBy.includes(reader1Id)) {
                return {
                  ...m,
                  status: 'read' as const,
                  readBy: [...currentReadBy, reader1Id],
                  readReceipts: [
                    ...(m.readReceipts || []),
                    {
                      userId: reader1Id,
                      userName: reader1Profile?.displayName || reader1Id,
                      userAvatar: reader1Profile?.photoURL,
                      readAt: now1,
                    },
                  ],
                };
              }
            }
            return m;
          }),
        };
      });

      // Second member reads after another 1.8s
      if (otherMembers.length > 1) {
        setTimeout(() => {
          const reader2Id = otherMembers[1];
          const reader2Profile = APP_USERS_DIRECTORY.find((u) => u.userId === reader2Id);
          const now2 = Date.now();

          setMessages((prev) => {
            const convMsgs = prev[convId];
            if (!convMsgs) return prev;
            return {
              ...prev,
              [convId]: convMsgs.map((m) => {
                if (m.id === messageId) {
                  const currentReadBy = m.readBy || [];
                  if (!currentReadBy.includes(reader2Id)) {
                    return {
                      ...m,
                      status: 'read' as const,
                      readBy: [...currentReadBy, reader2Id],
                      readReceipts: [
                        ...(m.readReceipts || []),
                        {
                          userId: reader2Id,
                          userName: reader2Profile?.displayName || reader2Id,
                          userAvatar: reader2Profile?.photoURL,
                          readAt: now2,
                        },
                      ],
                    };
                  }
                }
                return m;
              }),
            };
          });
        }, 1800);
      }
    }, 1200);
  };

  // Send an encrypted text message
  const handleSendMessage = async (text: string) => {
    if (!selectedConvId) return;

    // Encrypt payload via Web Cryptography API
    const { ciphertext, iv } = await encryptPayload(text, selectedConvId);
    const now = Date.now();
    const disappearingSeconds = currentConversation?.disappearingTimerSeconds || 0;

    const newMessage: Message = {
      id: `msg-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      conversationId: selectedConvId,
      senderId: currentUser.userId,
      senderName: currentUser.displayName,
      senderAvatar: currentUser.photoURL,
      ciphertext,
      iv,
      decryptedContent: text,
      type: 'text',
      status: 'delivered',
      readBy: [],
      readReceipts: [],
      disappearingAfterSeconds: disappearingSeconds || undefined,
      expiresAt: disappearingSeconds > 0 ? now + disappearingSeconds * 1000 : null,
      createdAt: now,
    };

    // Update messages
    setMessages((prev) => ({
      ...prev,
      [selectedConvId]: [...(prev[selectedConvId] || []), newMessage],
    }));

    // Update conversation snippet
    setConversations((prev) =>
      prev.map((c) =>
        c.id === selectedConvId
          ? {
              ...c,
              lastMessageSnippet: text,
              lastMessageSenderId: currentUser.userId,
              lastMessageTimestamp: now,
              updatedAt: now,
            }
          : c
      )
    );

    // Simulate counterpart reaction / read receipt
    if (selectedConvId === 'conv-simbarashe-tembo') {
      simulateFounderResponse(text, newMessage.id);
    } else if (selectedConvId === 'conv-sat-ai') {
      simulateAiResponse(text, newMessage.id);
    } else if (currentConversation?.type === 'group' || currentConversation?.type === 'community') {
      simulateGroupReads(selectedConvId, newMessage.id);
    }
  };

  // Simulate realistic founder/counterpart reply with typing indicator and E2EE
  const simulateFounderResponse = (incomingText: string, messageId?: string) => {
    // 1. Mark user's message as read by Mr Simbarashe Tembo after 900ms (showing cyan double checkmark)
    setTimeout(() => {
      if (messageId) {
        setMessages((prev) => {
          const convMsgs = prev['conv-simbarashe-tembo'];
          if (!convMsgs) return prev;
          return {
            ...prev,
            ['conv-simbarashe-tembo']: convMsgs.map((m) => {
              if (m.id === messageId) {
                return {
                  ...m,
                  status: 'read' as const,
                  readBy: Array.from(new Set([...(m.readBy || []), FOUNDER_PROFILE.userId])),
                  readReceipts: [
                    ...(m.readReceipts || []),
                    {
                      userId: FOUNDER_PROFILE.userId,
                      userName: FOUNDER_PROFILE.displayName,
                      userAvatar: FOUNDER_PROFILE.photoURL,
                      readAt: Date.now(),
                    },
                  ],
                };
              }
              return m;
            }),
          };
        });
      }

      // Show typing after 1 second
      setTypingUsers((prev) => ({ ...prev, ['conv-simbarashe-tembo']: 'Mr Simbarashe Tembo' }));

      // Send response after 2.5 seconds
      setTimeout(async () => {
        setTypingUsers((prev) => {
          const copy = { ...prev };
          delete copy['conv-simbarashe-tembo'];
          return copy;
        });

        const replyOptions = [
          'Affirmative. SAT Messenger ensures your communication is mathematically secure with zero server knowledge.',
          'Thank you for your message. The SAT Limited Organization is committed to sovereign privacy for every citizen.',
          'Your voice notes and files are encrypted with unique per-message ephemeral keys.',
          'Excellent. Our cross-platform synchronization allows you to switch between mobile and desktop without exposing private keys.',
        ];
        const replyText = replyOptions[Math.floor(Math.random() * replyOptions.length)];

        const { ciphertext, iv } = await encryptPayload(replyText, 'conv-simbarashe-tembo');
        const now = Date.now();

        const replyMsg: Message = {
          id: `msg-reply-${Date.now()}`,
          conversationId: 'conv-simbarashe-tembo',
          senderId: FOUNDER_PROFILE.userId,
          senderName: FOUNDER_PROFILE.displayName,
          senderAvatar: FOUNDER_PROFILE.photoURL,
          ciphertext,
          iv,
          decryptedContent: replyText,
          type: 'text',
          status: 'read',
          readBy: [currentUser.userId],
          readReceipts: [
            {
              userId: currentUser.userId,
              userName: currentUser.displayName,
              userAvatar: currentUser.photoURL,
              readAt: now,
            },
          ],
          createdAt: now,
        };

        setMessages((prev) => ({
          ...prev,
          ['conv-simbarashe-tembo']: [...(prev['conv-simbarashe-tembo'] || []), replyMsg],
        }));

        setConversations((prev) =>
          prev.map((c) =>
            c.id === 'conv-simbarashe-tembo'
              ? {
                  ...c,
                  lastMessageSnippet: replyText,
                  lastMessageSenderId: FOUNDER_PROFILE.userId,
                  lastMessageTimestamp: now,
                  updatedAt: now,
                }
              : c
          )
        );
      }, 2500);
    }, 900);
  };

  // Simulate intelligent SAT Limited AI assistant reply with typing indicator and E2EE
  const simulateAiResponse = async (incomingText: string, messageId?: string) => {
    // 1. Mark user's message as read by SAT Limited AI after 400ms (showing cyan double checkmark)
    setTimeout(() => {
      if (messageId) {
        setMessages((prev) => {
          const convMsgs = prev['conv-sat-ai'];
          if (!convMsgs) return prev;
          return {
            ...prev,
            ['conv-sat-ai']: convMsgs.map((m) => {
              if (m.id === messageId) {
                return {
                  ...m,
                  status: 'read' as const,
                  readBy: Array.from(new Set([...(m.readBy || []), 'sat-limited-ai-agent'])),
                  readReceipts: [
                    ...(m.readReceipts || []),
                    {
                      userId: 'sat-limited-ai-agent',
                      userName: 'SAT Limited AI',
                      userAvatar: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=256&q=80',
                      readAt: Date.now(),
                    },
                  ],
                };
              }
              return m;
            }),
          };
        });
      }

      // Show typing indicator
      setTypingUsers((prev) => ({ ...prev, ['conv-sat-ai']: 'SAT Limited AI' }));
    }, 400);

    // Prepare context for Gemini model
    const currentMsgs = messages['conv-sat-ai'] || [];
    const recentHistory = currentMsgs.slice(-8).map((m) => ({
      sender: m.senderName,
      text: m.decryptedContent || '',
    }));

    try {
      const aiResponseText = await generateSatAiResponse(incomingText, recentHistory);

      // Deliver message after realistic response time (1.2s)
      setTimeout(async () => {
        setTypingUsers((prev) => {
          const copy = { ...prev };
          delete copy['conv-sat-ai'];
          return copy;
        });

        const { ciphertext, iv } = await encryptPayload(aiResponseText, 'conv-sat-ai');
        const now = Date.now();

        const replyMsg: Message = {
          id: `msg-ai-${Date.now()}`,
          conversationId: 'conv-sat-ai',
          senderId: 'sat-limited-ai-agent',
          senderName: 'SAT Limited AI',
          senderAvatar: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=256&q=80',
          ciphertext,
          iv,
          decryptedContent: aiResponseText,
          type: 'text',
          status: 'read',
          readBy: [currentUser.userId],
          readReceipts: [
            {
              userId: currentUser.userId,
              userName: currentUser.displayName,
              userAvatar: currentUser.photoURL,
              readAt: now,
            },
          ],
          createdAt: now,
        };

        setMessages((prev) => ({
          ...prev,
          ['conv-sat-ai']: [...(prev['conv-sat-ai'] || []), replyMsg],
        }));

        setConversations((prev) =>
          prev.map((c) =>
            c.id === 'conv-sat-ai'
              ? {
                  ...c,
                  lastMessageSnippet: aiResponseText,
                  lastMessageSenderId: 'sat-limited-ai-agent',
                  lastMessageTimestamp: now,
                  updatedAt: now,
                }
              : c
          )
        );
      }, 1200);
    } catch (err) {
      console.error('SAT Limited AI generation error:', err);
      setTypingUsers((prev) => {
        const copy = { ...prev };
        delete copy['conv-sat-ai'];
        return copy;
      });
    }
  };

  // Send a voice note with transcription
  const handleSendVoiceNote = async (
    audioBase64: string,
    duration: number,
    transcription: string
  ) => {
    if (!selectedConvId) return;

    const { ciphertext, iv } = await encryptPayload(
      `Voice note (${duration}s)`,
      selectedConvId
    );
    const now = Date.now();
    const disappearingSeconds = currentConversation?.disappearingTimerSeconds || 0;

    const newMsg: Message = {
      id: `msg-audio-${Date.now()}`,
      conversationId: selectedConvId,
      senderId: currentUser.userId,
      senderName: currentUser.displayName,
      senderAvatar: currentUser.photoURL,
      ciphertext,
      iv,
      decryptedContent: `Voice note (${duration}s)`,
      type: 'audio',
      mediaData: audioBase64,
      audioDuration: duration,
      transcription,
      status: 'delivered',
      readBy: [],
      disappearingAfterSeconds: disappearingSeconds || undefined,
      expiresAt: disappearingSeconds > 0 ? now + disappearingSeconds * 1000 : null,
      createdAt: now,
    };

    setMessages((prev) => ({
      ...prev,
      [selectedConvId]: [...(prev[selectedConvId] || []), newMsg],
    }));

    setConversations((prev) =>
      prev.map((c) =>
        c.id === selectedConvId
          ? {
              ...c,
              lastMessageSnippet: `🎤 Voice note (${duration}s): "${transcription.slice(0, 35)}..."`,
              lastMessageSenderId: currentUser.userId,
              lastMessageTimestamp: now,
            }
          : c
      )
    );

    if (selectedConvId === 'conv-simbarashe-tembo') {
      simulateFounderResponse(`Voice note (${duration}s)`, newMsg.id);
    } else if (currentConversation?.type === 'group' || currentConversation?.type === 'community') {
      simulateGroupReads(selectedConvId, newMsg.id);
    }
  };

  // Send document attachment
  const handleSendDocument = async (
    fileData: string,
    fileName: string,
    fileSize: number,
    mimeType: string
  ) => {
    if (!selectedConvId) return;

    const { ciphertext, iv } = await encryptPayload(`File: ${fileName}`, selectedConvId);
    const now = Date.now();

    const newMsg: Message = {
      id: `msg-doc-${Date.now()}`,
      conversationId: selectedConvId,
      senderId: currentUser.userId,
      senderName: currentUser.displayName,
      senderAvatar: currentUser.photoURL,
      ciphertext,
      iv,
      decryptedContent: fileName,
      type: 'document',
      mediaData: fileData,
      mediaName: fileName,
      mediaSize: fileSize,
      mimeType,
      status: 'delivered',
      readBy: [],
      createdAt: now,
    };

    setMessages((prev) => ({
      ...prev,
      [selectedConvId]: [...(prev[selectedConvId] || []), newMsg],
    }));

    setConversations((prev) =>
      prev.map((c) =>
        c.id === selectedConvId
          ? {
              ...c,
              lastMessageSnippet: `📄 ${fileName}`,
              lastMessageSenderId: currentUser.userId,
              lastMessageTimestamp: now,
            }
          : c
      )
    );

    if (selectedConvId === 'conv-simbarashe-tembo') {
      simulateFounderResponse(`Document: ${fileName}`, newMsg.id);
    } else if (currentConversation?.type === 'group' || currentConversation?.type === 'community') {
      simulateGroupReads(selectedConvId, newMsg.id);
    }
  };

  // Send photo attachment
  const handleSendPhoto = async (imageData: string, caption?: string) => {
    if (!selectedConvId) return;

    const { ciphertext, iv } = await encryptPayload(caption || 'Encrypted Photo', selectedConvId);
    const now = Date.now();

    const newMsg: Message = {
      id: `msg-img-${Date.now()}`,
      conversationId: selectedConvId,
      senderId: currentUser.userId,
      senderName: currentUser.displayName,
      senderAvatar: currentUser.photoURL,
      ciphertext,
      iv,
      decryptedContent: caption || '',
      type: 'image',
      mediaData: imageData,
      status: 'delivered',
      readBy: [],
      createdAt: now,
    };

    setMessages((prev) => ({
      ...prev,
      [selectedConvId]: [...(prev[selectedConvId] || []), newMsg],
    }));

    setConversations((prev) =>
      prev.map((c) =>
        c.id === selectedConvId
          ? {
              ...c,
              lastMessageSnippet: `📷 Photo ${caption ? `: ${caption}` : ''}`,
              lastMessageSenderId: currentUser.userId,
              lastMessageTimestamp: now,
            }
          : c
      )
    );

    if (selectedConvId === 'conv-simbarashe-tembo') {
      simulateFounderResponse(caption || 'Photo', newMsg.id);
    } else if (currentConversation?.type === 'group' || currentConversation?.type === 'community') {
      simulateGroupReads(selectedConvId, newMsg.id);
    }
  };

  // Send location pin
  const handleSendLocation = async (location: LocationData) => {
    if (!selectedConvId) return;

    const { ciphertext, iv } = await encryptPayload(`Location: ${location.name}`, selectedConvId);
    const now = Date.now();

    const newMsg: Message = {
      id: `msg-loc-${Date.now()}`,
      conversationId: selectedConvId,
      senderId: currentUser.userId,
      senderName: currentUser.displayName,
      senderAvatar: currentUser.photoURL,
      ciphertext,
      iv,
      decryptedContent: location.name,
      type: 'location',
      location,
      status: 'delivered',
      readBy: [],
      createdAt: now,
    };

    setMessages((prev) => ({
      ...prev,
      [selectedConvId]: [...(prev[selectedConvId] || []), newMsg],
    }));

    setConversations((prev) =>
      prev.map((c) =>
        c.id === selectedConvId
          ? {
              ...c,
              lastMessageSnippet: `📍 Shared Location: ${location.name}`,
              lastMessageSenderId: currentUser.userId,
              lastMessageTimestamp: now,
            }
          : c
      )
    );

    if (selectedConvId === 'conv-simbarashe-tembo') {
      simulateFounderResponse(`Location: ${location.name}`, newMsg.id);
    } else if (currentConversation?.type === 'group' || currentConversation?.type === 'community') {
      simulateGroupReads(selectedConvId, newMsg.id);
    }
  };

  // Configure disappearing message timer
  const handleSelectDisappearingTimer = (seconds: number) => {
    if (!selectedConvId) return;
    setConversations((prev) =>
      prev.map((c) =>
        c.id === selectedConvId ? { ...c, disappearingTimerSeconds: seconds } : c
      )
    );
  };

  // Toggle message emoji reaction and sync via Firestore
  const handleToggleReaction = async (messageId: string, emoji: string) => {
    if (!selectedConvId) return;

    let nextReactionsMap: Record<string, string[]> = {};

    setMessages((prev) => {
      const convList = prev[selectedConvId] || [];
      const updated = convList.map((m) => {
        if (m.id !== messageId) return m;

        const currentMap = { ...(m.reactions || {}) };
        const existingUsers = currentMap[emoji] || [];

        let nextUsers: string[];
        if (existingUsers.includes(currentUser.userId)) {
          nextUsers = existingUsers.filter((u) => u !== currentUser.userId);
        } else {
          nextUsers = [...existingUsers, currentUser.userId];
        }

        if (nextUsers.length === 0) {
          delete currentMap[emoji];
        } else {
          currentMap[emoji] = nextUsers;
        }

        nextReactionsMap = currentMap;
        return {
          ...m,
          reactions: currentMap,
        };
      });

      return {
        ...prev,
        [selectedConvId]: updated,
      };
    });

    // Sync to Firestore if authenticated with Firebase
    if (isGoogleAuth && currentUser.userId && !currentUser.userId.startsWith('guest-')) {
      try {
        const messageDocRef = doc(db, 'conversations', selectedConvId, 'messages', messageId);
        await updateDoc(messageDocRef, {
          reactions: nextReactionsMap,
        });
      } catch (error) {
        handleFirestoreError(
          error,
          OperationType.UPDATE,
          `conversations/${selectedConvId}/messages/${messageId}`
        );
      }
    }
  };

  // Create new conversation
  const handleCreateConversation = (newConvData: Partial<Conversation>) => {
    const id = `conv-${Date.now()}`;
    const newConv: Conversation = {
      id,
      type: newConvData.type || 'direct',
      name: newConvData.name || 'New Chat',
      description: newConvData.description,
      avatar: newConvData.avatar,
      creatorId: currentUser.userId,
      memberIds: newConvData.memberIds?.length ? newConvData.memberIds : [currentUser.userId],
      adminIds: newConvData.adminIds?.length ? newConvData.adminIds : [currentUser.userId],
      disappearingTimerSeconds: newConvData.disappearingTimerSeconds || 0,
      lastMessageSnippet: newConvData.lastMessageSnippet || 'Conversation created',
      lastMessageTimestamp: Date.now(),
      createdAt: Date.now(),
      updatedAt: Date.now(),
      unreadCount: 0,
    };

    setConversations((prev) => [newConv, ...prev]);
    setSelectedConvId(id);
  };

  // Update conversation settings and admin roles
  const handleUpdateConversation = async (updated: Conversation) => {
    setConversations((prev) =>
      prev.map((c) => (c.id === updated.id ? updated : c))
    );

    if (isGoogleAuth && currentUser.userId && !currentUser.userId.startsWith('guest-')) {
      try {
        const convDocRef = doc(db, 'conversations', updated.id);
        await updateDoc(convDocRef, {
          name: updated.name,
          description: updated.description || '',
          adminIds: updated.adminIds || [],
          memberIds: updated.memberIds,
          updatedAt: updated.updatedAt,
        });
      } catch (err) {
        handleFirestoreError(err, OperationType.UPDATE, `conversations/${updated.id}`);
      }
    }
  };

  // Update user profile settings and sync with Firestore
  const handleUpdateProfile = async (updatedUser: UserProfile) => {
    setCurrentUser(updatedUser);
    localStorage.setItem(`sat_profile_${updatedUser.userId}`, JSON.stringify(updatedUser));

    if (isGoogleAuth && updatedUser.userId && !updatedUser.userId.startsWith('guest-')) {
      try {
        const userDocRef = doc(db, 'users', updatedUser.userId);
        await setDoc(
          userDocRef,
          {
            userId: updatedUser.userId,
            displayName: updatedUser.displayName,
            email: updatedUser.email,
            bio: updatedUser.bio || '',
            status: updatedUser.status || '',
            photoURL: updatedUser.photoURL || '',
            activeTheme: currentThemeId,
            lastSeen: Date.now(),
          },
          { merge: true }
        );
      } catch (err) {
        handleFirestoreError(err, OperationType.WRITE, `users/${updatedUser.userId}`);
      }
    }
  };

  // Google Sign-in flow
  const handleOpenGoogleRegistration = () => {
    setIsGoogleRegModalOpen(true);
  };

  const handleConfirmGoogleRegistration = async () => {
    try {
      const res = await signInWithGoogle();
      setIsGoogleAuth(true);
      setCurrentUser((prev) => ({
        ...prev,
        userId: res.user.uid,
        email: res.user.email || 'user@satmessenger.org',
        displayName: res.user.displayName || 'Augustus',
        photoURL: res.user.photoURL || prev.photoURL,
      }));
    } catch (err) {
      console.error('Google Sign In failed:', err);
      throw err;
    }
  };

  const handleSignOut = async () => {
    await logoutUser();
    setIsGoogleAuth(false);
  };

  // Google Chat sync integration
  const handleSyncGoogleChat = async () => {
    const token = getCachedAccessToken();
    if (!token) {
      // Prompt sign in
      try {
        const res = await signInWithGoogle();
        if (res.accessToken) {
          syncSpacesWithToken(res.accessToken);
        }
      } catch (e) {
        alert('Google Account sign in required to sync Google Chat spaces.');
      }
    } else {
      syncSpacesWithToken(token);
    }
  };

  const syncSpacesWithToken = async (token: string) => {
    const { spaces, status } = await fetchGoogleChatSpaces(token);
    if (!status.isConfigured) {
      alert(
        `Google Chat integration notice:\n${status.error || 'Configuration pending.'}\n${
          status.errorDetails || ''
        }`
      );
      // Create a synchronized placeholder room for Google Chat Space
      handleCreateConversation({
        type: 'google_chat',
        name: 'Google Chat Workspace Space (Synced)',
        description: 'Synced with Google Chat via Workspace OAuth',
        avatar: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=256&q=80',
        lastMessageSnippet: 'Google Chat space linked to SAT Messenger.',
      });
      return;
    }

    if (spaces.length > 0) {
      spaces.forEach((s) => {
        handleCreateConversation({
          type: 'google_chat',
          name: s.displayName || s.name,
          googleChatSpaceName: s.name,
          avatar: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=256&q=80',
          lastMessageSnippet: 'Synchronized with Google Workspace Chat.',
        });
      });
    }
  };

  // Real-time typing heartbeat emitter
  const handleTypingHeartbeat = () => {
    // could send to Firestore typing subcollection
  };

  // Restore cloud backup
  const handleRestoreBackup = (
    restoredConvs: Conversation[],
    restoredMsgs: Record<string, Message[]>
  ) => {
    setConversations(restoredConvs);
    setMessages(restoredMsgs);
  };

  // WhatsApp account linking & multi-device sync handlers
  const handleLinkWhatsAppAccount = async (info: WhatsAppLinkInfo) => {
    const updatedUser: UserProfile = {
      ...currentUser,
      whatsappLinked: info,
    };
    setCurrentUser(updatedUser);

    if (isGoogleAuth && currentUser.userId && !currentUser.userId.startsWith('guest-')) {
      try {
        await setDoc(
          doc(db, 'users', currentUser.userId),
          {
            whatsappLinked: info,
            updatedAt: Date.now(),
          },
          { merge: true }
        );
      } catch (err) {
        handleFirestoreError(err, OperationType.WRITE, `users/${currentUser.userId}`);
      }
    }

    // Auto-import sample WhatsApp bridged conversations
    const { conversations: waConvs, messages: waMsgs } = getSampleWhatsAppChats(info.phoneNumber);
    setConversations((prev) => {
      const existingIds = new Set(prev.map((c) => c.id));
      const toAdd = waConvs.filter((c) => !existingIds.has(c.id));
      return [...toAdd, ...prev];
    });
    setMessages((prev) => ({
      ...prev,
      ...waMsgs,
    }));

    if (waConvs.length > 0) {
      setSelectedConvId(waConvs[0].id);
      setActiveFilter('whatsapp');
    }
  };

  const handleUnlinkWhatsAppAccount = async () => {
    const { whatsappLinked, ...rest } = currentUser;
    setCurrentUser(rest as UserProfile);

    if (isGoogleAuth && currentUser.userId && !currentUser.userId.startsWith('guest-')) {
      try {
        await setDoc(
          doc(db, 'users', currentUser.userId),
          {
            whatsappLinked: null,
            updatedAt: Date.now(),
          },
          { merge: true }
        );
      } catch (err) {
        handleFirestoreError(err, OperationType.WRITE, `users/${currentUser.userId}`);
      }
    }
  };

  const handleImportWhatsAppChats = (phoneNumber: string) => {
    const { conversations: waConvs, messages: waMsgs } = getSampleWhatsAppChats(phoneNumber);
    setConversations((prev) => {
      const existingIds = new Set(prev.map((c) => c.id));
      const toAdd = waConvs.filter((c) => !existingIds.has(c.id));
      return [...toAdd, ...prev];
    });
    setMessages((prev) => ({
      ...prev,
      ...waMsgs,
    }));
    setActiveFilter('whatsapp');
  };

  return (
    <AndroidFrame>
      <div className="w-full h-full flex flex-row overflow-hidden relative">
        {/* Left Pane: Chat List */}
        <div
          className={`h-full ${
            selectedConvId ? 'hidden md:flex md:w-[360px] shrink-0' : 'w-full'
          }`}
        >
          <ChatList
            conversations={conversations}
            selectedConversationId={selectedConvId}
            onSelectConversation={(id) => setSelectedConvId(id)}
            onOpenNewChat={() => setIsNewChatModalOpen(true)}
            onOpenCloudBackup={() => setIsBackupModalOpen(true)}
            onOpenSettings={() => setIsSettingsModalOpen(true)}
            onOpenWhatsAppLink={() => setIsWhatsAppModalOpen(true)}
            activeFilter={activeFilter}
            onChangeFilter={setActiveFilter}
            currentUser={currentUser}
            typingUsers={typingUsers}
          />
        </div>

        {/* Right Pane: Active Chat Room */}
        <div
          className={`h-full flex-1 ${
            !selectedConvId ? 'hidden md:flex items-center justify-center bg-slate-950' : 'flex'
          }`}
        >
          {currentConversation ? (
            <ChatRoom
              conversation={currentConversation}
              messages={messages[currentConversation.id] || []}
              currentUser={currentUser}
              theme={currentTheme}
              onBack={() => setSelectedConvId(null)}
              onSendMessage={handleSendMessage}
              onOpenVoiceRecorder={() => setIsVoiceModalOpen(true)}
              onOpenMediaShare={() => setIsMediaModalOpen(true)}
              onOpenDisappearingTimer={() => setIsTimerModalOpen(true)}
              onOpenEncryptionDetails={() => setIsEncryptionModalOpen(true)}
              isOtherTyping={Boolean(typingUsers[currentConversation.id])}
              typingUserName={typingUsers[currentConversation.id]}
              onTypingHeartbeat={handleTypingHeartbeat}
              onToggleReaction={handleToggleReaction}
              onUpdateConversation={handleUpdateConversation}
            />
          ) : (
            <div className="text-center p-8 flex flex-col items-center justify-center gap-3 text-slate-500">
              <div className="w-16 h-16 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 font-black text-xl">
                SAT
              </div>
              <h3 className="font-semibold text-slate-300 text-sm">SAT Messenger</h3>
              <p className="text-xs text-slate-500 max-w-xs">
                Select a conversation or start a new encrypted group to begin secure communication.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Modals */}
      <VoiceRecorderModal
        isOpen={isVoiceModalOpen}
        onClose={() => setIsVoiceModalOpen(false)}
        onSendVoiceNote={handleSendVoiceNote}
      />

      <MediaShareModal
        isOpen={isMediaModalOpen}
        onClose={() => setIsMediaModalOpen(false)}
        onSendDocument={handleSendDocument}
        onSendPhoto={handleSendPhoto}
        onSendLocation={handleSendLocation}
      />

      <DisappearingTimerModal
        isOpen={isTimerModalOpen}
        onClose={() => setIsTimerModalOpen(false)}
        currentTimer={currentConversation?.disappearingTimerSeconds || 0}
        onSelectTimer={handleSelectDisappearingTimer}
      />

      {currentConversation && (
        <EncryptionDetailsModal
          isOpen={isEncryptionModalOpen}
          onClose={() => setIsEncryptionModalOpen(false)}
          conversationId={currentConversation.id}
          conversationName={currentConversation.name}
          currentUserId={currentUser.userId}
        />
      )}

      <CloudBackupModal
        isOpen={isBackupModalOpen}
        onClose={() => setIsBackupModalOpen(false)}
        userId={currentUser.userId}
        conversations={conversations}
        messages={messages}
        onRestoreBackup={handleRestoreBackup}
      />

      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
        currentTheme={currentThemeId}
        onSelectTheme={setCurrentThemeId}
        currentUser={currentUser}
        onUpdateProfile={handleUpdateProfile}
        isGoogleAuthenticated={isGoogleAuth}
        onGoogleSignIn={handleGoogleSignIn}
        onSignOut={handleSignOut}
        onOpenWhatsAppLink={() => setIsWhatsAppModalOpen(true)}
      />

      <NewChatModal
        isOpen={isNewChatModalOpen}
        onClose={() => setIsNewChatModalOpen(false)}
        onCreateConversation={handleCreateConversation}
        onSyncGoogleChat={handleSyncGoogleChat}
        onOpenWhatsAppLink={() => setIsWhatsAppModalOpen(true)}
      />

      <WhatsAppLinkModal
        isOpen={isWhatsAppModalOpen}
        onClose={() => setIsWhatsAppModalOpen(false)}
        linkedInfo={currentUser.whatsappLinked}
        onLinkAccount={handleLinkWhatsAppAccount}
        onUnlinkAccount={handleUnlinkWhatsAppAccount}
        onImportWhatsAppChats={handleImportWhatsAppChats}
      />
    </AndroidFrame>
  );
}
