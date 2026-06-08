"use client";
import { forwardRef } from "react";
import { cn } from "@/lib/utils";

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, id, ...props }, ref) => (
    <div className="w-full">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium mb-2">
          {label}
        </label>
      )}
      <input
        id={id}
        ref={ref}
        className={cn(
          "w-full px-4 py-3 rounded-xl border bg-white transition-all focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary",
          error ? "border-destructive" : "border-border",
          className
        )}
        {...props}
      />
      {error && <p className="mt-1.5 text-sm text-destructive">{error}</p>}
    </div>
  )
);
Input.displayName = "Input";
