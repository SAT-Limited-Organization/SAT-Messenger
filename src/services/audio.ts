/**
 * SAT Messenger Audio & Voice Note Service
 * Handles microphone recording, waveform generation, and speech-to-text audio transcription
 */

// SpeechRecognition type declarations
interface IWindow extends Window {
  SpeechRecognition?: any;
  webkitSpeechRecognition?: any;
}

export class VoiceRecorder {
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];
  private recognition: any = null;
  private transcribedText = '';
  private stream: MediaStream | null = null;
  private startTime = 0;

  async startRecording(
    onTranscriptionUpdate?: (text: string) => void,
    onWaveformUpdate?: (level: number) => void
  ): Promise<void> {
    this.audioChunks = [];
    this.transcribedText = '';
    this.startTime = Date.now();

    try {
      this.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (err) {
      console.warn('Microphone permission not granted or device unavailable', err);
      throw new Error('Microphone access is required to record voice notes.');
    }

    // Initialize MediaRecorder
    let mimeType = 'audio/webm';
    if (!MediaRecorder.isTypeSupported('audio/webm')) {
      mimeType = MediaRecorder.isTypeSupported('audio/mp4') ? 'audio/mp4' : '';
    }

    this.mediaRecorder = mimeType ? new MediaRecorder(this.stream, { mimeType }) : new MediaRecorder(this.stream);

    this.mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        this.audioChunks.push(event.data);
      }
    };

    // Setup live Web Speech API recognition for real-time transcription if supported
    const win = window as unknown as IWindow;
    const SpeechRec = win.SpeechRecognition || win.webkitSpeechRecognition;

    if (SpeechRec) {
      try {
        this.recognition = new SpeechRec();
        this.recognition.continuous = true;
        this.recognition.interimResults = true;
        this.recognition.lang = 'en-US';

        this.recognition.onresult = (event: any) => {
          let currentTranscript = '';
          for (let i = 0; i < event.results.length; i++) {
            currentTranscript += event.results[i][0].transcript + ' ';
          }
          this.transcribedText = currentTranscript.trim();
          if (onTranscriptionUpdate) {
            onTranscriptionUpdate(this.transcribedText);
          }
        };

        this.recognition.onerror = (e: any) => {
          console.warn('Speech recognition warning:', e.error);
        };

        this.recognition.start();
      } catch (e) {
        console.warn('Speech recognition initialization error:', e);
      }
    }

    // Audio context for waveform analysis
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const source = audioCtx.createMediaStreamSource(this.stream);
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 64;
      source.connect(analyser);

      const dataArray = new Uint8Array(analyser.frequencyBinCount);
      const checkLevel = () => {
        if (!this.mediaRecorder || this.mediaRecorder.state !== 'recording') return;
        analyser.getByteFrequencyData(dataArray);
        let sum = 0;
        for (let i = 0; i < dataArray.length; i++) {
          sum += dataArray[i];
        }
        const average = sum / dataArray.length;
        const normalized = Math.min(100, Math.round((average / 128) * 100));
        if (onWaveformUpdate) {
          onWaveformUpdate(normalized);
        }
        requestAnimationFrame(checkLevel);
      };
      requestAnimationFrame(checkLevel);
    } catch (e) {
      // AudioContext fallback
    }

    this.mediaRecorder.start(250);
  }

  async stopRecording(): Promise<{
    audioBlob: Blob;
    audioBase64: string;
    durationSeconds: number;
    transcription: string;
  }> {
    return new Promise((resolve, reject) => {
      if (!this.mediaRecorder) {
        reject(new Error('No active recording'));
        return;
      }

      const duration = Math.max(1, Math.round((Date.now() - this.startTime) / 1000));

      if (this.recognition) {
        try {
          this.recognition.stop();
        } catch (e) {
          // ignore
        }
      }

      this.mediaRecorder.onstop = async () => {
        const audioBlob = new Blob(this.audioChunks, { type: this.mediaRecorder?.mimeType || 'audio/webm' });
        
        // Stop audio tracks
        if (this.stream) {
          this.stream.getTracks().forEach((track) => track.stop());
        }

        // Convert to Base64
        const reader = new FileReader();
        reader.onloadend = () => {
          const audioBase64 = reader.result as string;
          let finalTranscription = this.transcribedText;
          if (!finalTranscription) {
            finalTranscription = `Voice memo (${duration}s) - Secure E2EE Audio`;
          }
          resolve({
            audioBlob,
            audioBase64,
            durationSeconds: duration,
            transcription: finalTranscription,
          });
        };
        reader.onerror = reject;
        reader.readAsDataURL(audioBlob);
      };

      this.mediaRecorder.stop();
    });
  }

  cancelRecording() {
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch (e) {}
    }
    if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
      this.mediaRecorder.stop();
    }
    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop());
    }
  }
}

// Generate an SVG or wave bar levels sequence for sound representation
export function generateWaveformBars(count = 28): number[] {
  const bars: number[] = [];
  for (let i = 0; i < count; i++) {
    const wave = Math.sin((i / count) * Math.PI) * 50 + 20;
    const randomJitter = Math.floor(Math.random() * 25);
    bars.push(Math.min(95, Math.max(15, Math.round(wave + randomJitter))));
  }
  return bars;
}
