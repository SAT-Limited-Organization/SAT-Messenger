import React, { useState, useEffect, useRef } from 'react';
import { Mic, Square, Trash2, Send, Sparkles, AlertCircle } from 'lucide-react';
import { VoiceRecorder, generateWaveformBars } from '../services/audio';

interface VoiceRecorderModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSendVoiceNote: (audioBase64: string, duration: number, transcription: string) => void;
}

export const VoiceRecorderModal: React.FC<VoiceRecorderModalProps> = ({
  isOpen,
  onClose,
  onSendVoiceNote,
}) => {
  const [isRecording, setIsRecording] = useState(false);
  const [secondsElapsed, setSecondsElapsed] = useState(0);
  const [transcription, setTranscription] = useState('');
  const [waveformLevel, setWaveformLevel] = useState(20);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const recorderRef = useRef<VoiceRecorder | null>(null);
  const timerRef = useRef<any>(null);
  const [bars, setBars] = useState<number[]>(generateWaveformBars(24));

  useEffect(() => {
    if (isOpen) {
      startRecording();
    } else {
      stopAndCleanup();
    }
    return () => {
      stopAndCleanup();
    };
  }, [isOpen]);

  // Waveform level animation
  useEffect(() => {
    if (isRecording) {
      const interval = setInterval(() => {
        setBars(generateWaveformBars(24));
      }, 150);
      return () => clearInterval(interval);
    }
  }, [isRecording]);

  const startRecording = async () => {
    setErrorMessage(null);
    setTranscription('');
    setSecondsElapsed(0);

    const recorder = new VoiceRecorder();
    recorderRef.current = recorder;

    try {
      await recorder.startRecording(
        (liveText) => {
          setTranscription(liveText);
        },
        (level) => {
          setWaveformLevel(level);
        }
      );

      setIsRecording(true);
      timerRef.current = setInterval(() => {
        setSecondsElapsed((prev) => prev + 1);
      }, 1000);
    } catch (err: any) {
      setErrorMessage(err.message || 'Microphone access denied. Check your browser permissions.');
      setIsRecording(false);
    }
  };

  const handleStopAndSend = async () => {
    if (!recorderRef.current || !isRecording) return;
    try {
      clearInterval(timerRef.current);
      const result = await recorderRef.current.stopRecording();
      setIsRecording(false);
      onSendVoiceNote(result.audioBase64, result.durationSeconds, result.transcription);
      onClose();
    } catch (err: any) {
      setErrorMessage('Failed to finalize voice recording.');
    }
  };

  const handleCancel = () => {
    stopAndCleanup();
    onClose();
  };

  const stopAndCleanup = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    if (recorderRef.current) {
      recorderRef.current.cancelRecording();
      recorderRef.current = null;
    }
    setIsRecording(false);
  };

  if (!isOpen) return null;

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-2xl flex flex-col gap-4 text-slate-100">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full bg-rose-500 animate-ping" />
            <h3 className="font-semibold text-sm text-slate-200">Recording Encrypted Voice Note</h3>
          </div>
          <span className="font-mono text-emerald-400 font-bold text-sm bg-slate-950 px-2.5 py-0.5 rounded-full border border-slate-800">
            {formatTime(secondsElapsed)}
          </span>
        </div>

        {errorMessage ? (
          <div className="p-3 rounded-xl bg-rose-950/50 border border-rose-800/60 text-xs text-rose-300 flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        ) : (
          /* Live Waveform Bars */
          <div className="h-16 bg-slate-950 rounded-xl px-4 py-2 border border-slate-800/80 flex items-center justify-center gap-1">
            {bars.map((height, i) => (
              <div
                key={i}
                className="w-1.5 rounded-full bg-gradient-to-t from-emerald-600 to-teal-300 transition-all duration-150"
                style={{
                  height: `${Math.max(12, (height * (waveformLevel + 30)) / 100)}%`,
                }}
              />
            ))}
          </div>
        )}

        {/* Live Audio Transcription Box */}
        <div className="p-3 bg-slate-950/90 rounded-xl border border-slate-800/80 text-xs flex flex-col gap-1.5 min-h-[72px]">
          <div className="flex items-center gap-1.5 text-[11px] font-semibold text-emerald-400">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Live Speech-to-Text Transcription</span>
          </div>
          <p className="text-slate-300 italic text-[11px] leading-relaxed">
            {transcription || (isRecording ? 'Listening for speech...' : 'Press start to speak')}
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex items-center justify-between pt-1">
          <button
            onClick={handleCancel}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-rose-400 hover:bg-slate-800/80 transition-colors"
          >
            <Trash2 className="w-4 h-4" />
            <span>Discard</span>
          </button>

          <div className="flex items-center gap-2">
            <button
              onClick={handleStopAndSend}
              disabled={!isRecording && !errorMessage}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg shadow-emerald-900/40 transition-all disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Send Voice Note</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
