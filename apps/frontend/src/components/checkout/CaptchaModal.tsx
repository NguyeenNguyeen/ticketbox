"use client";
import { useState, useEffect, useRef, useCallback } from "react";
import { RefreshCw, X, ShieldCheck, AlertCircle } from "lucide-react";

/* ═══════════════════════════════════════════════════════
   CHALLENGE GENERATORS  (easy mode)
═══════════════════════════════════════════════════════ */
type ChallengeKind = "math" | "text" | "sequence" | "emoji" | "slider";

interface Challenge {
  kind: ChallengeKind;
  answer: string;        // expected answer (compare after trim/lowercase)
  hint: string;
  badge: string;
  // display payloads
  mathExpr?: string;     // e.g. "7 + 4"
  textCode?: string;     // e.g. "A3BK"
  seqTerms?: number[];   // e.g. [2,4,6,8]
  emojiA?: { icon: string; val: number };
  emojiB?: { icon: string; val: number };
  sliderTarget?: number;
}

function rand(min: number, max: number) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

/* 1. MATH ── cộng/trừ, số nhỏ ───────────────────────── */
function makeMath(): Challenge {
  const isAdd = Math.random() < 0.5;
  let a: number, b: number, answer: number, expr: string;
  if (isAdd) {
    a = rand(2, 15); b = rand(2, 15);
    answer = a + b; expr = `${a} + ${b}`;
  } else {
    a = rand(10, 20); b = rand(1, a - 1);
    answer = a - b; expr = `${a} - ${b}`;
  }
  return {
    kind: "math", mathExpr: expr, answer: String(answer),
    hint: "Tính kết quả phép toán trên", badge: "🔢 Toán",
  };
}

/* 2. TEXT ── 4 chữ in hoa, ít méo ───────────────────── */
const CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
function makeText(): Challenge {
  let code = "";
  for (let i = 0; i < 4; i++) code += CHARS[rand(0, CHARS.length - 1)];
  return {
    kind: "text", textCode: code, answer: code.toLowerCase(),
    hint: "Nhập 4 chữ cái trên (không phân biệt hoa/thường)", badge: "🔤 Chữ",
  };
}

/* 3. SEQUENCE ── cộng sai đơn giản ─────────────────────*/
function makeSequence(): Challenge {
  const start = rand(1, 10);
  const step  = rand(2, 6);
  const terms = [start, start + step, start + 2 * step, start + 3 * step];
  const answer = start + 4 * step;
  return {
    kind: "sequence", seqTerms: terms, answer: String(answer),
    hint: `Mỗi số cách nhau ${step} đơn vị — số tiếp theo là?`, badge: "📐 Dãy số",
  };
}

/* 4. EMOJI ── cộng hai giá trị nhỏ ─────────────────────*/
const EMOJIS = ["🍎","🍋","🐶","🐱","⭐","🌙","🔥","💎","🎵","🌈"];
function makeEmoji(): Challenge {
  const [iconA, iconB] = [...EMOJIS].sort(() => Math.random() - 0.5);
  const vA = rand(1, 9), vB = rand(1, 9);
  return {
    kind: "emoji",
    emojiA: { icon: iconA, val: vA },
    emojiB: { icon: iconB, val: vB },
    answer: String(vA + vB),
    hint: `${iconA} = ${vA}, ${iconB} = ${vB} → tổng = ?`, badge: "🎭 Emoji",
  };
}

/* 5. SLIDER ── kéo đến vị trí cho trước ────────────────*/
function makeSlider(): Challenge {
  const target = rand(25, 75); // stays in friendly zone
  return {
    kind: "slider", sliderTarget: target, answer: String(target),
    hint: `Kéo thanh đến đúng vị trí ${target}%`, badge: "🎚️ Kéo thanh",
  };
}

function generateChallenge(): Challenge {
  const makers = [makeMath, makeText, makeSequence, makeEmoji, makeSlider];
  return makers[rand(0, makers.length - 1)]();
}

/* ═══════════════════════════════════════════════════════
   THEME PER KIND
═══════════════════════════════════════════════════════ */
const THEMES: Record<ChallengeKind, { bg: string; text: string; accent: string; pill: string }> = {
  math:     { bg: "linear-gradient(135deg,#ede9fe,#ddd6fe)", text: "#4c1d95", accent: "#7c3aed", pill: "bg-violet-100 text-violet-700" },
  text:     { bg: "linear-gradient(135deg,#fef3c7,#fde68a)", text: "#78350f", accent: "#b45309", pill: "bg-amber-100 text-amber-700" },
  sequence: { bg: "linear-gradient(135deg,#dcfce7,#bbf7d0)", text: "#14532d", accent: "#16a34a", pill: "bg-emerald-100 text-emerald-700" },
  emoji:    { bg: "linear-gradient(135deg,#fce7f3,#fbcfe8)", text: "#831843", accent: "#be185d", pill: "bg-pink-100 text-pink-700" },
  slider:   { bg: "linear-gradient(135deg,#e0f2fe,#bae6fd)", text: "#0c4a6e", accent: "#0369a1", pill: "bg-sky-100 text-sky-700" },
};

/* ═══════════════════════════════════════════════════════
   CHALLENGE DISPLAY (no canvas — plain styled text)
═══════════════════════════════════════════════════════ */
function ChallengeDisplay({ ch }: { ch: Challenge }) {
  const theme = THEMES[ch.kind];

  /* SLIDER */
  if (ch.kind === "slider") return null; // rendered separately

  /* MATH */
  if (ch.kind === "math") {
    return (
      <div className="rounded-2xl flex items-center justify-center py-6 gap-2" style={{ background: theme.bg }}>
        <span className="text-4xl font-black select-none" style={{ color: theme.text, letterSpacing: "0.05em" }}>
          {ch.mathExpr} =
        </span>
        <span className="text-4xl font-black" style={{ color: theme.accent }}>?</span>
      </div>
    );
  }

  /* TEXT */
  if (ch.kind === "text") {
    return (
      <div className="rounded-2xl flex items-center justify-center py-6 gap-2" style={{ background: theme.bg }}>
        {ch.textCode!.split("").map((c, i) => (
          <span
            key={i}
            className="text-4xl font-black select-none"
            style={{
              color: i % 2 === 0 ? theme.text : theme.accent,
              display: "inline-block",
              transform: `rotate(${(Math.random() > .5 ? 1 : -1) * rand(3, 10)}deg)`,
              fontFamily: "'Courier New', monospace",
            }}
          >
            {c}
          </span>
        ))}
      </div>
    );
  }

  /* SEQUENCE */
  if (ch.kind === "sequence") {
    return (
      <div className="rounded-2xl flex items-center justify-center py-5 gap-2 flex-wrap" style={{ background: theme.bg }}>
        {ch.seqTerms!.map((n, i) => (
          <span key={i} className="text-3xl font-black select-none" style={{ color: theme.text }}>
            {n}<span className="text-xl font-normal opacity-50"> ,</span>
          </span>
        ))}
        <span className="text-3xl font-black" style={{ color: theme.accent }}>?</span>
      </div>
    );
  }

  /* EMOJI */
  if (ch.kind === "emoji") {
    const { emojiA, emojiB } = ch;
    return (
      <div className="rounded-2xl py-5 px-4 space-y-2" style={{ background: theme.bg }}>
        {/* Values */}
        <div className="flex justify-center gap-6 text-sm font-semibold" style={{ color: theme.text }}>
          <span>{emojiA!.icon} = {emojiA!.val}</span>
          <span>{emojiB!.icon} = {emojiB!.val}</span>
        </div>
        {/* Equation */}
        <div className="flex items-center justify-center gap-2">
          <span className="text-4xl">{emojiA!.icon}</span>
          <span className="text-2xl font-black" style={{ color: theme.text }}>+</span>
          <span className="text-4xl">{emojiB!.icon}</span>
          <span className="text-2xl font-black" style={{ color: theme.text }}>=</span>
          <span className="text-3xl font-black" style={{ color: theme.accent }}>?</span>
        </div>
      </div>
    );
  }

  return null;
}

/* ═══════════════════════════════════════════════════════
   SLIDER UI
═══════════════════════════════════════════════════════ */
function SliderUI({ target, value, onChange }: { target: number; value: number; onChange: (v: number) => void }) {
  const theme = THEMES.slider;
  const ok = Math.abs(value - target) <= 8;
  return (
    <div className="rounded-2xl p-5 space-y-4" style={{ background: theme.bg }}>
      <p className="text-center text-sm font-bold" style={{ color: theme.text }}>
        Kéo đến vị trí <span className="text-orange-500 text-base">{target}%</span>
      </p>
      <div className="relative h-10 flex items-center">
        {/* Track */}
        <div className="w-full h-3 rounded-full overflow-visible relative" style={{ background: "rgba(255,255,255,0.6)" }}>
          {/* Fill */}
          <div className="h-full rounded-full transition-all" style={{
            width: `${value}%`,
            background: ok ? "linear-gradient(90deg,#22c55e,#4ade80)" : `linear-gradient(90deg,${theme.accent},#38bdf8)`,
          }} />
          {/* Target marker */}
          <div className="absolute top-1/2 w-2 h-7 rounded-full bg-orange-400 border-2 border-white shadow-md"
            style={{ left: `${target}%`, transform: "translate(-50%,-50%)" }} />
          <span className="absolute -top-6 text-xs font-bold text-orange-500 select-none"
            style={{ left: `${target}%`, transform: "translateX(-50%)" }}>{target}%</span>
        </div>
        {/* Invisible range input */}
        <input type="range" min={0} max={100} value={value}
          onChange={(e) => onChange(Number(e.target.value))}
          className="absolute inset-0 w-full opacity-0 cursor-pointer" style={{ zIndex: 2 }} />
        {/* Thumb */}
        <div className="absolute top-1/2 w-7 h-7 rounded-full border-2 border-white shadow-lg pointer-events-none transition-all"
          style={{
            left: `${value}%`, transform: "translate(-50%,-50%)", zIndex: 1,
            background: ok ? "linear-gradient(135deg,#22c55e,#4ade80)" : `linear-gradient(135deg,${theme.accent},#38bdf8)`,
          }} />
      </div>
      <div className="flex justify-between text-xs" style={{ color: theme.text }}>
        <span>0%</span>
        <span className="font-bold">{ok ? "✓ Đúng vị trí!" : `Hiện tại: ${value}%`}</span>
        <span>100%</span>
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════
   MAIN MODAL
═══════════════════════════════════════════════════════ */
interface CaptchaModalProps {
  isOpen: boolean;
  onSuccess: () => void;
  onClose: () => void;
}

export function CaptchaModal({ isOpen, onSuccess, onClose }: CaptchaModalProps) {
  const [ch, setCh] = useState<Challenge>(generateChallenge);
  const [input, setInput] = useState("");
  const [sliderVal, setSliderVal] = useState(50);
  const [status, setStatus] = useState<"idle" | "error" | "success">("idle");
  const [attempts, setAttempts] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const refresh = useCallback(() => {
    setCh(generateChallenge());
    setInput(""); setSliderVal(50); setStatus("idle"); setAttempts(0);
    setTimeout(() => inputRef.current?.focus(), 60);
  }, []);

  useEffect(() => { if (isOpen) refresh(); }, [isOpen, refresh]);

  const isSlider = ch.kind === "slider";
  const sliderOk = isSlider && Math.abs(sliderVal - (ch.sliderTarget ?? 50)) <= 8;

  const check = (): boolean => {
    if (isSlider) return sliderOk;
    if (ch.kind === "text") return input.trim().toLowerCase() === ch.answer;
    return input.trim() === ch.answer;
  };

  const handleSubmit = () => {
    if (!isSlider && !input.trim()) return;
    if (check()) {
      setStatus("success");
      setTimeout(onSuccess, 600);
    } else {
      const n = attempts + 1;
      setAttempts(n); setStatus("error");
      setInput("");
      if (n >= 3) setTimeout(refresh, 800);
      else setTimeout(() => inputRef.current?.focus(), 60);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") handleSubmit();
    if (e.key === "Escape") onClose();
  };

  if (!isOpen) return null;

  const theme = THEMES[ch.kind];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backdropFilter: "blur(6px)", background: "rgba(0,0,0,0.42)" }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div
        className="relative w-full max-w-xs rounded-2xl overflow-hidden animate-fade-in"
        style={{ background: "#fff", boxShadow: "0 20px 60px -10px rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.06)" }}
      >
        {/* Top accent stripe */}
        <div className="h-1 w-full" style={{ background: theme.bg }} />

        {/* Close */}
        <button id="captcha-close-btn" onClick={onClose}
          className="absolute top-3 right-3 text-muted-foreground hover:text-foreground transition-colors rounded-full p-1 hover:bg-secondary">
          <X className="w-4 h-4" />
        </button>

        <div className="px-5 pt-5 pb-6 space-y-4">
          {/* Header */}
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
              style={{ background: "linear-gradient(135deg,#7c3aed,#a78bfa)" }}>
              <ShieldCheck className="w-4 h-4 text-white" />
            </div>
            <div className="flex-1">
              <h2 className="font-bold text-sm text-foreground leading-tight">Xác minh bảo mật</h2>
              <p className="text-xs text-muted-foreground">Hoàn thành để tiếp tục</p>
            </div>
            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${theme.pill}`}>
              {ch.badge}
            </span>
          </div>

          {/* Challenge */}
          {isSlider
            ? <SliderUI target={ch.sliderTarget!} value={sliderVal} onChange={setSliderVal} />
            : <ChallengeDisplay ch={ch} />
          }

          {/* Hint + Refresh */}
          <div className="flex items-start justify-between gap-2">
            <p className="text-xs text-muted-foreground leading-relaxed flex-1">{ch.hint}</p>
            <button id="captcha-refresh-btn" onClick={refresh}
              className="flex items-center gap-1 text-xs text-primary hover:text-primary-hover font-medium shrink-0 mt-0.5">
              <RefreshCw className="w-3 h-3" />Đổi
            </button>
          </div>

          {/* Text input (not for slider) */}
          {!isSlider && (
            <input
              ref={inputRef}
              id="captcha-answer-input"
              type={ch.kind === "text" ? "text" : "number"}
              value={input}
              onChange={(e) => { setInput(e.target.value); if (status === "error") setStatus("idle"); }}
              onKeyDown={handleKeyDown}
              placeholder="Nhập đáp án..."
              disabled={status === "success"}
              maxLength={ch.kind === "text" ? 4 : undefined}
              className={`
                w-full px-4 py-3 rounded-xl border-2 text-center font-bold text-xl
                outline-none transition-all
                ${status === "error"   ? "border-destructive bg-red-50 text-destructive"
                : status === "success" ? "border-success bg-green-50 text-success"
                :                       "border-border bg-white focus:border-primary"}
              `}
            />
          )}

          {/* Status */}
          {status === "error" && (
            <div className="flex items-center gap-1.5 text-xs text-destructive animate-fade-in">
              <AlertCircle className="w-3.5 h-3.5 shrink-0" />
              <span>{attempts >= 3 ? "Sai quá 3 lần, đang đổi câu hỏi..." : `Chưa đúng, còn ${3 - attempts} lần thử.`}</span>
            </div>
          )}
          {status === "success" && (
            <div className="flex items-center gap-1.5 text-xs text-success animate-fade-in">
              <ShieldCheck className="w-3.5 h-3.5 shrink-0" />
              <span>Xác minh thành công!</span>
            </div>
          )}

          {/* Attempt bar */}
          {attempts > 0 && status !== "success" && (
            <div className="flex gap-1.5">
              {[1,2,3].map(n => (
                <div key={n} className={`h-1 flex-1 rounded-full transition-all ${n <= attempts ? "bg-destructive" : "bg-border"}`} />
              ))}
            </div>
          )}

          {/* Action buttons */}
          <div className="flex gap-3">
            <button id="captcha-cancel-btn" onClick={onClose}
              className="flex-1 py-3 rounded-xl border border-border text-sm font-medium text-muted-foreground hover:bg-secondary transition-all">
              Hủy
            </button>
            <button id="captcha-submit-btn" onClick={handleSubmit}
              disabled={status === "success" || (!isSlider && !input.trim())}
              className="flex-1 py-3 rounded-xl font-bold text-sm text-white transition-all disabled:opacity-40 disabled:cursor-not-allowed"
              style={{ background: `linear-gradient(135deg,${theme.accent},#a78bfa)` }}>
              {status === "success" ? "✓ Xong" : "Xác nhận"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
