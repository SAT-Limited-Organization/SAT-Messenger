import React, { useState } from 'react';
import { Smartphone, Monitor, Shield, Wifi, Battery, Signal } from 'lucide-react';

interface AndroidFrameProps {
  children: React.ReactNode;
}

export const AndroidFrame: React.FC<AndroidFrameProps> = ({ children }) => {
  const [deviceMode, setDeviceMode] = useState<'android' | 'desktop'>('android');
  const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="min-h-screen bg-[#070a10] flex flex-col items-center justify-center p-0 sm:p-4 text-slate-100 font-['Plus_Jakarta_Sans',sans-serif]">
      {/* Device Mode Switcher Top Bar (Floating Header) */}
      <header className="w-full max-w-6xl mx-auto mb-2 px-4 py-2 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-400 bg-slate-900/60 backdrop-blur-md rounded-xl border border-slate-800/80 shadow-lg">
        <div className="flex items-center gap-2.5">
          <div className="w-6 h-6 rounded-md bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 font-bold text-xs">
            SAT
          </div>
          <div className="flex flex-col">
            <span className="font-semibold text-slate-200 text-xs">
              SAT Messenger <span className="text-emerald-400 text-[10px] px-1.5 py-0.5 rounded bg-emerald-950/60 border border-emerald-800/50">E2EE Android v1.0</span>
            </span>
            <span className="text-[10px] text-slate-400 truncate hidden sm:inline">
              SAT Limited Organization • Founded by Mr Simbarashe Tembo • com.sat_commuity.satmessenger
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-[11px] text-slate-400 hidden md:inline">Platform View:</span>
          <div className="inline-flex rounded-lg bg-slate-950/80 p-0.5 border border-slate-800">
            <button
              onClick={() => setDeviceMode('android')}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-medium transition-all ${
                deviceMode === 'android'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Android Mobile</span>
            </button>
            <button
              onClick={() => setDeviceMode('desktop')}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-medium transition-all ${
                deviceMode === 'desktop'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Monitor className="w-3.5 h-3.5" />
              <span>Desktop / Web Sync</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="w-full flex-1 flex items-center justify-center">
        {deviceMode === 'android' ? (
          /* Android Phone Shell */
          <div className="relative w-full max-w-[430px] h-[92vh] max-h-[880px] bg-black rounded-[44px] shadow-[0_25px_60px_-15px_rgba(0,0,0,0.9),0_0_0_12px_#18202f,0_0_0_14px_#0e1420] overflow-hidden flex flex-col border border-slate-700/30">
            {/* Android Dynamic Island / Camera Hole & Speaker */}
            <div className="absolute top-2.5 left-1/2 -translate-x-1/2 z-50 flex items-center gap-2">
              <div className="w-3.5 h-3.5 rounded-full bg-slate-950 border-2 border-slate-800 flex items-center justify-center">
                <div className="w-1.5 h-1.5 rounded-full bg-slate-900 ring-1 ring-emerald-500/30" />
              </div>
            </div>

            {/* Android Status Bar */}
            <div className="h-7 pt-1 px-6 bg-slate-950/90 text-slate-300 text-[11px] font-semibold flex items-center justify-between select-none z-40 border-b border-slate-900/60">
              <span className="tracking-tight">{currentTime}</span>
              <div className="flex items-center gap-2">
                <span className="text-[10px] tracking-wider text-emerald-400 flex items-center gap-1 font-mono">
                  <Shield className="w-2.5 h-2.5" /> E2EE
                </span>
                <Signal className="w-3 h-3 text-slate-300" />
                <Wifi className="w-3 h-3 text-slate-300" />
                <div className="flex items-center gap-0.5">
                  <span className="text-[9px]">98%</span>
                  <Battery className="w-3.5 h-3.5 text-slate-300 fill-slate-300" />
                </div>
              </div>
            </div>

            {/* App Screen Content */}
            <div className="flex-1 overflow-hidden relative flex flex-col bg-[#0b0f17]">
              {children}
            </div>

            {/* Android Bottom Navigation Gesture Bar */}
            <div className="h-4 bg-slate-950 flex items-center justify-center select-none z-40">
              <div className="w-28 h-1 rounded-full bg-slate-600/60 hover:bg-slate-400 transition-colors" />
            </div>
          </div>
        ) : (
          /* Desktop / Tablet Wide Screen View */
          <div className="w-full max-w-6xl h-[88vh] bg-slate-950 rounded-2xl shadow-2xl border border-slate-800/80 overflow-hidden flex flex-col">
            <div className="h-9 px-4 bg-slate-900/90 border-b border-slate-800/70 flex items-center justify-between text-xs text-slate-400">
              <div className="flex items-center gap-2">
                <div className="flex gap-1.5">
                  <div className="w-3 h-3 rounded-full bg-rose-500/80" />
                  <div className="w-3 h-3 rounded-full bg-amber-500/80" />
                  <div className="w-3 h-3 rounded-full bg-emerald-500/80" />
                </div>
                <span className="ml-2 font-mono text-[11px] text-slate-300">
                  SAT Messenger Desktop • Synchronized via End-to-End Encrypted Cloud
                </span>
              </div>
              <div className="flex items-center gap-3 text-[11px]">
                <span className="text-emerald-400 flex items-center gap-1">
                  <Shield className="w-3 h-3" /> Live E2EE Active
                </span>
                <span className="text-slate-500">|</span>
                <span className="text-slate-400">com.sat_commuity.satmessenger</span>
              </div>
            </div>
            <div className="flex-1 overflow-hidden relative flex flex-col bg-[#0b0f17]">
              {children}
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
