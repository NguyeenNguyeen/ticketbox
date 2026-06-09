"use client";

import { X, Users } from "lucide-react";
import { useEffect, useState } from "react";
import type { Artist } from "@/types/concert";

interface ArtistBioModalProps {
  artist: Artist | null;
  isOpen: boolean;
  onClose: () => void;
}

export function ArtistBioModal({ artist, isOpen, onClose }: ArtistBioModalProps) {
  const [show, setShow] = useState(false);
  const [render, setRender] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setRender(true);
      // Small delay to allow CSS transition to trigger
      setTimeout(() => setShow(true), 10);
    } else {
      setShow(false);
      // Wait for exit animation before unmounting
      setTimeout(() => setRender(false), 300);
    }
  }, [isOpen]);

  if (!render || !artist) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div 
        className={`absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity duration-300 ${show ? 'opacity-100' : 'opacity-0'}`}
        onClick={onClose}
      />
      
      {/* Modal */}
      <div 
        className={`relative w-full max-w-2xl max-h-[85vh] bg-white rounded-3xl shadow-2xl overflow-hidden flex flex-col transition-all duration-300 transform ${show ? 'opacity-100 scale-100 translate-y-0' : 'opacity-0 scale-95 translate-y-8'}`}
      >
        <button 
          onClick={onClose}
          className="absolute top-4 right-4 z-10 w-10 h-10 bg-black/10 hover:bg-black/20 rounded-full flex items-center justify-center transition-colors"
        >
          <X className="w-5 h-5 text-gray-800" />
        </button>

        <div className="overflow-y-auto custom-scrollbar flex-1">
          <div className="p-8 md:p-10">
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6 mb-8">
              <div className="w-32 h-32 rounded-full overflow-hidden border-4 border-primary/20 shrink-0 flex items-center justify-center bg-primary/10">
                {artist.avatarUrl ? (
                  <img src={artist.avatarUrl} alt={artist.name} className="w-full h-full object-cover" />
                ) : (
                  <Users className="w-16 h-16 text-primary/60" />
                )}
              </div>
              <div className="text-center sm:text-left mt-2">
                <h2 className="text-3xl font-bold mb-2 text-gray-900">{artist.name}</h2>
                <div className="inline-block px-3 py-1 bg-primary/10 text-primary font-medium text-sm rounded-full">
                  Nghệ sĩ khách mời
                </div>
              </div>
            </div>

            <div>
              <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
                <span className="w-1.5 h-6 bg-primary rounded-full"></span>
                Tiểu sử & Sự nghiệp (AI Generated)
              </h3>
              <div className="prose prose-sm sm:prose-base text-muted-foreground whitespace-pre-wrap leading-relaxed">
                {artist.bio || "Nghệ sĩ này chưa được cập nhật tiểu sử."}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
