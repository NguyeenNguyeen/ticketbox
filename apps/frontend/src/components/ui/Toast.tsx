"use client";
import { createContext, useContext, useState, useCallback } from "react";
import { X, CheckCircle, AlertCircle, Info } from "lucide-react";
import { cn } from "@/lib/utils";

type ToastVariant = "default" | "success" | "error";
interface ToastItem { id: string; title: string; description?: string; variant: ToastVariant; }

const ToastContext = createContext<{ toast: (t: Omit<ToastItem, "id">) => void }>({ toast: () => {} });

export function useToast() { return useContext(ToastContext); }

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const toast = useCallback((t: Omit<ToastItem, "id">) => {
    const id = Math.random().toString(36).substring(2, 15) + Date.now().toString(36);
    setToasts((prev) => [...prev, { ...t, id }]);
    setTimeout(() => setToasts((prev) => prev.filter((x) => x.id !== id)), 5000);
  }, []);

  const remove = (id: string) => setToasts((prev) => prev.filter((x) => x.id !== id));

  const icons = { default: Info, success: CheckCircle, error: AlertCircle };
  const colors = {
    default: "border-primary/20 bg-white",
    success: "border-success/20 bg-green-50",
    error: "border-destructive/20 bg-red-50",
  };

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-3 max-w-sm w-full">
        {toasts.map((t) => {
          const Icon = icons[t.variant];
          return (
            <div key={t.id} className={cn("flex items-start gap-3 p-4 rounded-xl border shadow-lg animate-slide-in-right", colors[t.variant])}>
              <Icon className={cn("w-5 h-5 mt-0.5 flex-shrink-0", t.variant === "success" ? "text-success" : t.variant === "error" ? "text-destructive" : "text-primary")} />
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-sm">{t.title}</p>
                {t.description && <p className="text-sm text-muted-foreground mt-0.5">{t.description}</p>}
              </div>
              <button onClick={() => remove(t.id)} className="p-0.5 hover:bg-black/5 rounded">
                <X className="w-4 h-4 text-muted-foreground" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}
