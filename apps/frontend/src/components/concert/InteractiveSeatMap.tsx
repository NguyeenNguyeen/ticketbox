"use client";

import { useEffect, useRef, useState } from "react";
import { API_BASE_URL } from "@/lib/constants";

interface InteractiveSeatMapProps {
  concertId: string;
}

export function InteractiveSeatMap({ concertId }: InteractiveSeatMapProps) {
  const [svgContent, setSvgContent] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetch(`${API_BASE_URL}/concerts/${concertId}/seat-map`)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to load SVG");
        return res.text();
      })
      .then((text) => setSvgContent(text))
      .catch((err) => console.error("Error loading SVG seat map", err));
  }, [concertId]);

  useEffect(() => {
    if (!svgContent || !containerRef.current) return;

    const svg = containerRef.current.querySelector("svg");
    if (!svg) return;

    svg.style.width = "100%";
    svg.style.height = "auto";
    svg.style.maxHeight = "600px";

    const handleClick = (e: MouseEvent) => {
      const target = e.target as Element;
      const zoneElement = target.closest('g[id], path[id], rect[id], polygon[id]');
      
      if (!zoneElement || !svg.contains(zoneElement)) return;
      
      const zoneId = zoneElement.id.trim();
      const categoryBlock = document.getElementById(`category-${zoneId}`);
      
      if (categoryBlock) {
        // Scroll to it
        categoryBlock.scrollIntoView({ behavior: "smooth", block: "center" });
        
        // Add a strong visible highlight effect
        categoryBlock.style.transition = "all 0.3s ease-in-out";
        const originalBg = categoryBlock.style.backgroundColor;
        const originalBoxShadow = categoryBlock.style.boxShadow;
        const originalTransform = categoryBlock.style.transform;
        
        categoryBlock.style.backgroundColor = "rgba(59, 130, 246, 0.15)";
        categoryBlock.style.boxShadow = "0 0 0 4px rgba(59, 130, 246, 0.5)";
        categoryBlock.style.transform = "scale(1.02)";
        
        setTimeout(() => {
          categoryBlock.style.backgroundColor = originalBg || "";
          categoryBlock.style.boxShadow = originalBoxShadow || "";
          categoryBlock.style.transform = originalTransform || "";
        }, 1500);
      } else {
        console.warn(`[InteractiveSeatMap] Category block not found for zone ID: ${zoneId}`);
      }
    };

    svg.addEventListener("click", handleClick);
    
    // Add some global CSS for interactive paths within this SVG
    const styleId = "interactive-svg-styles";
    if (!document.getElementById(styleId)) {
      const style = document.createElement("style");
      style.id = styleId;
      style.innerHTML = `
        .interactive-seat-map svg g[id], 
        .interactive-seat-map svg path[id], 
        .interactive-seat-map svg rect[id], 
        .interactive-seat-map svg polygon[id] {
          cursor: pointer;
          transition: all 0.2s ease-in-out;
        }
        .interactive-seat-map svg g[id]:hover, 
        .interactive-seat-map svg path[id]:hover, 
        .interactive-seat-map svg rect[id]:hover, 
        .interactive-seat-map svg polygon[id]:hover {
          filter: drop-shadow(0 0 5px rgba(0,0,0,0.5));
          opacity: 0.8;
          stroke: #000;
          stroke-width: 2px;
        }
      `;
      document.head.appendChild(style);
    }

    return () => {
      svg.removeEventListener("click", handleClick);
    };
  }, [svgContent]);

  if (!svgContent) {
    return (
      <div className="w-full h-64 flex flex-col items-center justify-center bg-secondary/10 rounded-xl animate-pulse">
        <div className="w-8 h-8 border-4 border-primary/30 border-t-primary rounded-full animate-spin mb-4" />
        <p className="text-muted-foreground">Đang tải sơ đồ ghế...</p>
      </div>
    );
  }

  return (
    <div 
      ref={containerRef}
      className="interactive-seat-map flex justify-center bg-secondary/20 rounded-xl overflow-hidden p-4"
      dangerouslySetInnerHTML={{ __html: svgContent }}
    />
  );
}
