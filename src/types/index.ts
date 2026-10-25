export type RoomType = 'direct' | 'group' | 'community' | 'google_chat' | 'whatsapp';

export type MessageType = 'text' | 'audio' | 'image' | 'document' | 'location';

export type MessageStatus = 'sent' | 'delivered' | 'read';

export interface LocationData {
  latitude: number;
  longitude: number;
  name: string;
  address?: string;
}

export interface ReadReceipt {
  userId: string;
  userName?: string;
  userAvatar?: string;
  readAt: number; // epoch ms
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  senderAvatar?: string;
  // E2EE fields
  ciphertext: string;
  iv: string;
  authTag?: string;
  decryptedContent?: string; // Decrypted locally
  type: MessageType;
  
  // Media / File metadata
  mediaData?: string; // base64 or URL
  mediaName?: string;
  mediaSize?: number;
  mimeType?: string;
  audioDuration?: number;
  transcription?: string;
  location?: LocationData;

  // Status & Receipts
  status: MessageStatus;
  readBy: string[]; // user IDs
  readReceipts?: ReadReceipt[]; // specific users with read timestamps
  reactions?: Record<string, string[]>; // emoji -> array of userIds

  // Disappearing messages
  disappearingAfterSeconds?: number;
  expiresAt?: number | null; // epoch ms

  // Timestamps
  createdAt: number; // epoch ms
  replyToId?: string;
  replySnippet?: string;
}

export interface Conversation {
  id: string;
  type: RoomType;
  name: string;
  description?: string;
  avatar?: string;
  creatorId: string;
  memberIds: string[];
  adminIds?: string[];
  disappearingTimerSeconds?: number; // 0 = off, 10, 60, 3600, 86400, etc.
  lastMessageSnippet?: string;
  lastMessageSenderId?: string;
  lastMessageTimestamp?: number;
  encryptionFingerprint?: string;
  createdAt: number;
  updatedAt: number;
  isPinned?: boolean;
  unreadCount?: number;
  googleChatSpaceName?: string;
  whatsappPhoneNumber?: string;
}

export interface WhatsAppLinkInfo {
  phoneNumber: string;
  linkedAt: number;
  deviceName: string;
  verified: boolean;
  pushSyncEnabled: boolean;
  apiKeyOrToken?: string;
}

export interface UserProfile {
  userId: string;
  email: string;
  displayName: string;
  photoURL?: string;
  publicKey?: string;
  bio?: string;
  status?: string;
  isOnline: boolean;
  lastSeen?: number;
  activeTheme?: ThemeId;
  whatsappLinked?: WhatsAppLinkInfo;
}

export interface TypingIndicator {
  userId: string;
  userName: string;
  isTyping: boolean;
  updatedAt: number;
}

export interface CloudBackup {
  id: string;
  userId: string;
  backupData: string; // Encrypted JSON string
  checksum: string;
  totalMessages: number;
  deviceLabel: string;
  createdAt: number;
}

export type ThemeId = 'cyber-midnight' | 'amoled-pure' | 'emerald-stealth' | 'nordic-slate' | 'simba-gold';

export interface ThemeConfig {
  id: ThemeId;
  name: string;
  description: string;
  accentColor: string;
  bgDark: string;
  bgCard: string;
  bgBubbleOwn: string;
  bgBubbleOther: string;
  textColor: string;
  accentText: string;
  border: string;
}
