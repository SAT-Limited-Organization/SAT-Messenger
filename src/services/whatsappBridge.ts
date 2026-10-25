/**
 * SAT Messenger - WhatsApp Account Link & Cross-Sync Bridge
 * Supports QR pairing, 8-character pairing code handshake, and wa.me deep-link relay
 */

import { Conversation, Message, WhatsAppLinkInfo } from '../types';

export interface WhatsAppCountry {
  code: string;
  name: string;
  dialCode: string;
  flag: string;
  placeholder: string;
}

export const WHATSAPP_SUPPORTED_COUNTRIES: WhatsAppCountry[] = [
  { code: 'ZM', name: 'Zambia', dialCode: '+260', flag: '🇿🇲', placeholder: '97 123 4567' },
  { code: 'ZW', name: 'Zimbabwe', dialCode: '+263', flag: '🇿🇼', placeholder: '77 123 4567' },
  { code: 'ZA', name: 'South Africa', dialCode: '+27', flag: '🇿🇦', placeholder: '82 123 4567' },
  { code: 'KE', name: 'Kenya', dialCode: '+254', flag: '🇰🇪', placeholder: '712 345678' },
  { code: 'NG', name: 'Nigeria', dialCode: '+234', flag: '🇳🇬', placeholder: '802 123 4567' },
  { code: 'UK', name: 'United Kingdom', dialCode: '+44', flag: '🇬🇧', placeholder: '7911 123456' },
  { code: 'US', name: 'United States', dialCode: '+1', flag: '🇺🇸', placeholder: '202 555 0123' },
];

export interface WhatsAppContact {
  id: string;
  name: string;
  phone: string;
  avatar?: string;
  about?: string;
  lastSeen?: string;
}

// Generate dynamic 8-character pairing code (WhatsApp phone linking protocol)
export function generatePairingCode(phoneNumber: string): string {
  const cleanPhone = phoneNumber.replace(/\D/g, '');
  let hash = 0;
  for (let i = 0; i < cleanPhone.length; i++) {
    hash = (hash << 5) - hash + cleanPhone.charCodeAt(i);
    hash |= 0;
  }
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code = '';
  const seed = Math.abs(hash) + Date.now();
  for (let i = 0; i < 8; i++) {
    code += chars[(seed + i * 7) % chars.length];
  }
  return `${code.slice(0, 4)}-${code.slice(4)}`;
}

// Generate QR pairing session string
export function generateWhatsAppQrSession(): {
  sessionToken: string;
  expiresInSeconds: number;
} {
  const randomBytes = Array.from(window.crypto.getRandomValues(new Uint8Array(20)))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
  return {
    sessionToken: `2@${randomBytes},SAT-MESSENGER-BRIDGE,${Date.now()}`,
    expiresInSeconds: 60,
  };
}

// Open chat directly in WhatsApp Web / App via wa.me
export function openWhatsAppWebChat(phone: string, text?: string) {
  const clean = phone.replace(/\D/g, '');
  const url = `https://wa.me/${clean}${text ? `?text=${encodeURIComponent(text)}` : ''}`;
  window.open(url, '_blank');
}

// Mock sample chats imported from WhatsApp once linked
export function getSampleWhatsAppChats(userPhone: string): {
  conversations: Conversation[];
  messages: Record<string, Message[]>;
} {
  const conv1Id = 'conv-wa-family';
  const conv2Id = 'conv-wa-business';

  const conversations: Conversation[] = [
    {
      id: conv1Id,
      type: 'whatsapp',
      name: 'Family & Close Circle (WhatsApp)',
      description: 'Synchronized from linked WhatsApp account • Encrypted Bridge',
      avatar: 'https://images.unsplash.com/photo-1511895426328-dc8714191300?auto=format&fit=crop&w=256&q=80',
      creatorId: 'wa-creator',
      memberIds: ['current-user', 'wa-contact-1', 'wa-contact-2'],
      whatsappPhoneNumber: userPhone,
      disappearingTimerSeconds: 0,
      lastMessageSnippet: 'Mom: Let us know when you land safely! ❤️',
      lastMessageTimestamp: Date.now() - 1000 * 60 * 18,
      createdAt: Date.now() - 1000 * 60 * 60 * 24 * 5,
      updatedAt: Date.now() - 1000 * 60 * 18,
      unreadCount: 1,
    },
    {
      id: conv2Id,
      type: 'whatsapp',
      name: 'SAT Regional Logistics (WhatsApp)',
      description: 'Bridged WhatsApp Business Channel • Verified',
      avatar: 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=256&q=80',
      creatorId: 'wa-business-admin',
      memberIds: ['current-user', 'wa-vendor-1'],
      whatsappPhoneNumber: userPhone,
      disappearingTimerSeconds: 0,
      lastMessageSnippet: 'Courier dispatch confirmation: Consignment #SAT-9920 has been cleared.',
      lastMessageTimestamp: Date.now() - 1000 * 60 * 85,
      createdAt: Date.now() - 1000 * 60 * 60 * 24 * 2,
      updatedAt: Date.now() - 1000 * 60 * 85,
      unreadCount: 0,
    },
  ];

  const messages: Record<string, Message[]> = {
    [conv1Id]: [
      {
        id: 'msg-wa-1',
        conversationId: conv1Id,
        senderId: 'wa-contact-1',
        senderName: 'Mom (WhatsApp)',
        ciphertext: 'wa_sync_payload_e2ee...',
        iv: 'wa_iv_9901',
        decryptedContent: 'Let us know when you land safely! Have a great trip and check in on SAT Messenger! ❤️',
        type: 'text',
        status: 'read',
        readBy: ['current-user'],
        createdAt: Date.now() - 1000 * 60 * 18,
      },
    ],
    [conv2Id]: [
      {
        id: 'msg-wa-2',
        conversationId: conv2Id,
        senderId: 'wa-vendor-1',
        senderName: 'Logistics Dispatch (WhatsApp)',
        ciphertext: 'wa_sync_payload_e2ee_2...',
        iv: 'wa_iv_9902',
        decryptedContent: 'Courier dispatch confirmation: Consignment #SAT-9920 has been cleared for Harare central depot.',
        type: 'text',
        status: 'read',
        readBy: ['current-user'],
        createdAt: Date.now() - 1000 * 60 * 85,
      },
    ],
  };

  return { conversations, messages };
}
