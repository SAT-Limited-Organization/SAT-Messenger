/**
 * Google Chat Workspace API Service
 * Manages Google Chat spaces and messages synchronization with SAT Messenger
 */

export interface GoogleChatSpace {
  name: string; // "spaces/AAA..."
  type: 'ROOM' | 'DM';
  displayName?: string;
  spaceType?: string;
  externalUserAllowed?: boolean;
}

export interface GoogleChatMessage {
  name: string; // "spaces/AAA/messages/BBB"
  text: string;
  createTime: string;
  sender?: {
    name?: string;
    displayName?: string;
    avatarUrl?: string;
    type?: string;
  };
}

export interface GoogleChatSyncStatus {
  isConfigured: boolean;
  error?: string;
  errorDetails?: string;
  consoleUrl?: string;
}

const PROJECT_ID = 'gen-lang-client-0233162056';

export async function fetchGoogleChatSpaces(accessToken: string): Promise<{
  spaces: GoogleChatSpace[];
  status: GoogleChatSyncStatus;
}> {
  try {
    const res = await fetch('https://chat.googleapis.com/v1/spaces', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (res.status === 404) {
      return {
        spaces: [],
        status: {
          isConfigured: false,
          error: 'Google Chat app not configured yet',
          errorDetails:
            'To use Google Chat in your organization, the Google Chat API app configuration must be enabled in Google Cloud Console.',
          consoleUrl: `https://console.cloud.google.com/apis/api/chat.googleapis.com/hangouts-chat?project=${PROJECT_ID}`,
        },
      };
    }

    if (!res.ok) {
      const errJson = await res.json().catch(() => ({}));
      const message = errJson.error?.message || `HTTP ${res.status}: ${res.statusText}`;
      return {
        spaces: [],
        status: {
          isConfigured: false,
          error: message,
          consoleUrl: `https://console.cloud.google.com/apis/api/chat.googleapis.com/hangouts-chat?project=${PROJECT_ID}`,
        },
      };
    }

    const data = await res.json();
    return {
      spaces: data.spaces || [],
      status: { isConfigured: true },
    };
  } catch (error) {
    return {
      spaces: [],
      status: {
        isConfigured: false,
        error: error instanceof Error ? error.message : 'Network error connecting to Google Chat API',
      },
    };
  }
}

export async function fetchGoogleChatMessages(
  accessToken: string,
  spaceName: string
): Promise<{ messages: GoogleChatMessage[]; error?: string }> {
  try {
    const res = await fetch(`https://chat.googleapis.com/v1/${spaceName}/messages?pageSize=25`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!res.ok) {
      const errJson = await res.json().catch(() => ({}));
      return {
        messages: [],
        error: errJson.error?.message || `Failed to fetch messages (${res.status})`,
      };
    }

    const data = await res.json();
    return {
      messages: data.messages || [],
    };
  } catch (error) {
    return {
      messages: [],
      error: error instanceof Error ? error.message : 'Failed to fetch messages',
    };
  }
}

export async function sendGoogleChatMessage(
  accessToken: string,
  spaceName: string,
  text: string
): Promise<{ success: boolean; message?: GoogleChatMessage; error?: string }> {
  try {
    const res = await fetch(`https://chat.googleapis.com/v1/${spaceName}/messages`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ text }),
    });

    if (!res.ok) {
      const errJson = await res.json().catch(() => ({}));
      return {
        success: false,
        error: errJson.error?.message || `Failed to post message (${res.status})`,
      };
    }

    const data = await res.json();
    return { success: true, message: data };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : 'Error sending message',
    };
  }
}
