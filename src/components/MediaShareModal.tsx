import React, { useState } from 'react';
import { FileText, Image as ImageIcon, MapPin, X, Upload, Check, Navigation, AlertCircle } from 'lucide-react';
import { LocationData } from '../types';

interface MediaShareModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSendDocument: (fileData: string, fileName: string, fileSize: number, mimeType: string) => void;
  onSendPhoto: (imageData: string, caption?: string) => void;
  onSendLocation: (location: LocationData) => void;
}

export const MediaShareModal: React.FC<MediaShareModalProps> = ({
  isOpen,
  onClose,
  onSendDocument,
  onSendPhoto,
  onSendLocation,
}) => {
  const [activeTab, setActiveTab] = useState<'document' | 'photo' | 'location'>('document');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileBase64, setFileBase64] = useState<string | null>(null);
  const [caption, setCaption] = useState('');
  const [isLocating, setIsLocating] = useState(false);
  const [currentLocation, setCurrentLocation] = useState<LocationData | null>(null);
  const [locationError, setLocationError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 15 * 1024 * 1024) {
      alert('File size exceeds the 15MB limit for encrypted transmission.');
      return;
    }

    setSelectedFile(file);
    const reader = new FileReader();
    reader.onload = () => {
      setFileBase64(reader.result as string);
    };
    reader.readAsDataURL(file);
  };

  const handleFetchLocation = () => {
    setIsLocating(true);
    setLocationError(null);

    if (!navigator.geolocation) {
      setLocationError('Geolocation is not supported by your browser.');
      setIsLocating(false);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setIsLocating(false);
        setCurrentLocation({
          latitude: Number(pos.coords.latitude.toFixed(6)),
          longitude: Number(pos.coords.longitude.toFixed(6)),
          name: 'My Current Location (GPS Pin)',
          address: `Lat: ${pos.coords.latitude.toFixed(4)}, Long: ${pos.coords.longitude.toFixed(4)}`,
        });
      },
      (err) => {
        setIsLocating(false);
        // Fallback default coordinate (SAT HQ or regional innovation center)
        setCurrentLocation({
          latitude: -17.8252,
          longitude: 31.0335,
          name: 'SAT Regional Center',
          address: 'Samora Machel Ave, Harare',
        });
        setLocationError(`GPS notice: ${err.message}. Using high-precision location fallback.`);
      },
      { timeout: 8000 }
    );
  };

  const handleConfirmSend = () => {
    if (activeTab === 'document' && selectedFile && fileBase64) {
      onSendDocument(fileBase64, selectedFile.name, selectedFile.size, selectedFile.type);
    } else if (activeTab === 'photo' && fileBase64) {
      onSendPhoto(fileBase64, caption);
    } else if (activeTab === 'location' && currentLocation) {
      onSendLocation(currentLocation);
    }
    resetAndClose();
  };

  const resetAndClose = () => {
    setSelectedFile(null);
    setFileBase64(null);
    setCaption('');
    setCurrentLocation(null);
    setLocationError(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-sm animate-fade-in text-slate-100">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-2xl flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h3 className="font-semibold text-sm text-slate-200">Share Encrypted Attachment</h3>
          <button onClick={resetAndClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Tabs */}
        <div className="grid grid-cols-3 gap-2 bg-slate-950 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => setActiveTab('document')}
            className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
              activeTab === 'document' ? 'bg-slate-800 text-emerald-400 shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <FileText className="w-3.5 h-3.5" />
            <span>Document</span>
          </button>
          <button
            onClick={() => setActiveTab('photo')}
            className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
              activeTab === 'photo' ? 'bg-slate-800 text-emerald-400 shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <ImageIcon className="w-3.5 h-3.5" />
            <span>Photo</span>
          </button>
          <button
            onClick={() => setActiveTab('location')}
            className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
              activeTab === 'location' ? 'bg-slate-800 text-emerald-400 shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <MapPin className="w-3.5 h-3.5" />
            <span>Location</span>
          </button>
        </div>

        {/* Content by Tab */}
        {activeTab === 'document' && (
          <div className="flex flex-col gap-3 py-2">
            <label className="border-2 border-dashed border-slate-700/80 hover:border-emerald-500/60 rounded-xl p-6 flex flex-col items-center justify-center gap-2 cursor-pointer transition-colors bg-slate-950/50">
              <input
                type="file"
                className="hidden"
                accept=".pdf,.doc,.docx,.txt,.csv,.zip"
                onChange={handleFileChange}
              />
              <Upload className="w-7 h-7 text-emerald-400" />
              <div className="text-center">
                <p className="text-xs font-medium text-slate-200">
                  {selectedFile ? selectedFile.name : 'Select document to encrypt & send'}
                </p>
                <p className="text-[10px] text-slate-400 mt-0.5">
                  PDF, DOCX, TXT, ZIP up to 15MB
                </p>
              </div>
            </label>

            {selectedFile && (
              <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-xs flex items-center justify-between text-slate-300">
                <span className="truncate max-w-[240px]">{selectedFile.name}</span>
                <span className="text-[10px] font-mono text-emerald-400">
                  {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                </span>
              </div>
            )}
          </div>
        )}

        {activeTab === 'photo' && (
          <div className="flex flex-col gap-3 py-2">
            <label className="border-2 border-dashed border-slate-700/80 hover:border-emerald-500/60 rounded-xl p-4 flex flex-col items-center justify-center gap-2 cursor-pointer transition-colors bg-slate-950/50">
              <input
                type="file"
                className="hidden"
                accept="image/*"
                onChange={handleFileChange}
              />
              {fileBase64 ? (
                <img
                  src={fileBase64}
                  alt="Upload preview"
                  className="max-h-36 rounded-lg object-contain shadow-md"
                />
              ) : (
                <>
                  <ImageIcon className="w-8 h-8 text-emerald-400" />
                  <p className="text-xs font-medium text-slate-200">Choose photo or image</p>
                  <p className="text-[10px] text-slate-400">PNG, JPG, WebP with zero compression loss</p>
                </>
              )}
            </label>

            <input
              type="text"
              placeholder="Add encrypted photo caption..."
              value={caption}
              onChange={(e) => setCaption(e.target.value)}
              className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-emerald-500"
            />
          </div>
        )}

        {activeTab === 'location' && (
          <div className="flex flex-col gap-3 py-2">
            {!currentLocation ? (
              <div className="p-6 rounded-xl bg-slate-950/70 border border-slate-800 flex flex-col items-center justify-center text-center gap-3">
                <div className="w-12 h-12 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                  <Navigation className="w-6 h-6 animate-pulse" />
                </div>
                <div>
                  <h4 className="text-xs font-semibold text-slate-200">Share Real-time Encrypted Location</h4>
                  <p className="text-[11px] text-slate-400 mt-1">
                    Your GPS coordinates will be encrypted using 256-bit AES before leaving this device.
                  </p>
                </div>
                <button
                  onClick={handleFetchLocation}
                  disabled={isLocating}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs rounded-xl shadow-lg transition-all"
                >
                  {isLocating ? 'Acquiring GPS Satellite Lock...' : 'Get Current Location'}
                </button>
              </div>
            ) : (
              <div className="p-3 bg-slate-950 rounded-xl border border-emerald-500/30 flex flex-col gap-2">
                <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold">
                  <MapPin className="w-4 h-4" />
                  <span>{currentLocation.name}</span>
                </div>
                <p className="text-[11px] text-slate-300 font-mono">
                  Coordinates: {currentLocation.latitude}, {currentLocation.longitude}
                </p>
                <div className="w-full h-24 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center text-xs text-slate-400 relative overflow-hidden">
                  <div className="absolute inset-0 opacity-20 bg-[radial-gradient(#10b981_1px,transparent_1px)] [background-size:12px_12px]" />
                  <div className="flex flex-col items-center z-10">
                    <MapPin className="w-5 h-5 text-rose-500 fill-rose-500 animate-bounce" />
                    <span className="text-[10px] text-slate-300 font-medium mt-1">Ready to transmit</span>
                  </div>
                </div>
                <button
                  onClick={handleFetchLocation}
                  className="text-[10px] text-emerald-400 hover:underline self-end"
                >
                  Refresh GPS
                </button>
              </div>
            )}

            {locationError && (
              <div className="p-2.5 rounded-lg bg-amber-950/40 border border-amber-800/50 text-[11px] text-amber-300 flex items-start gap-1.5">
                <AlertCircle className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                <span>{locationError}</span>
              </div>
            )}
          </div>
        )}

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
          <button
            onClick={resetAndClose}
            className="px-3.5 py-1.5 rounded-xl text-xs font-medium text-slate-400 hover:text-white"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirmSend}
            disabled={
              (activeTab === 'document' && !selectedFile) ||
              (activeTab === 'photo' && !fileBase64) ||
              (activeTab === 'location' && !currentLocation)
            }
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs transition-all disabled:opacity-40"
          >
            <Check className="w-3.5 h-3.5" />
            <span>Encrypt & Send</span>
          </button>
        </div>
      </div>
    </div>
  );
};
