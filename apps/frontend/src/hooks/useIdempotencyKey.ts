"use client";
import { useState, useCallback } from "react";

export function useIdempotencyKey() {
  const [key, setKey] = useState(() => crypto.randomUUID());
  const regenerate = useCallback(() => setKey(crypto.randomUUID()), []);
  return { key, regenerate };
}
