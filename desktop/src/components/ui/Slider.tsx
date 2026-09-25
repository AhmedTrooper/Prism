import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
} from "react";

import { cn } from "@/lib/cn";

export type SliderProps = Omit<
  React.InputHTMLAttributes<HTMLInputElement>,
  "value" | "defaultValue" | "onChange"
> & {
  value: number;
  min?: number;
  max?: number;
  step?: number;
  onChange?: (next: number) => void;
  /** Aria label, required for accessibility. */
  "aria-label": string;
};

export const Slider = forwardRef<HTMLInputElement, SliderProps>(
  function Slider(
    { className, value, min = 0, max = 100, step = 1, onChange, ...rest },
    ref,
  ) {
    const [local, setLocal] = useState(value);
    const dragging = useRef(false);
    // Keep local in sync with prop when not actively dragging. This lets
    // the parent treat Slider as controlled while still showing smooth
    // local motion during the drag itself.
    useEffect(() => {
      if (!dragging.current) setLocal(value);
    }, [value]);

    const handleChange = useCallback(
      (e: ChangeEvent<HTMLInputElement>) => {
        const next = Number(e.target.value);
        setLocal(next);
        onChange?.(next);
      },
      [onChange],
    );

    const startDrag = () => {
      dragging.current = true;
    };
    const endDrag = () => {
      dragging.current = false;
    };

    const percent =
      max === min ? 0 : ((local - min) / (max - min)) * 100;

    return (
      <span
        className={cn(
          "relative inline-flex h-9 w-full items-center select-none",
          className,
        )}
      >
        <span
          aria-hidden
          className="absolute inset-x-0 top-1/2 -translate-y-1/2 h-1.5 rounded-full bg-surface-sunken"
        />
        <span
          aria-hidden
          className="absolute top-1/2 -translate-y-1/2 h-1.5 rounded-full bg-prism-primary"
          style={{ width: `${Math.max(0, Math.min(100, percent))}%` }}
        />
        <input
          ref={ref}
          type="range"
          min={min}
          max={max}
          step={step}
          value={local}
          onChange={handleChange}
          onPointerDown={startDrag}
          onPointerUp={endDrag}
          onPointerCancel={endDrag}
          className={cn(
            "relative z-10 h-9 w-full appearance-none bg-transparent cursor-pointer",
            "[&::-webkit-slider-thumb]:appearance-none",
            "[&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:w-4",
            "[&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-prism-on-primary",
            "[&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:border-2",
            "[&::-webkit-slider-thumb]:border-prism-primary",
            "[&::-moz-range-thumb]:h-4 [&::-moz-range-thumb]:w-4 [&::-moz-range-thumb]:rounded-full",
            "[&::-moz-range-thumb]:bg-prism-on-primary [&::-moz-range-thumb]:border-2",
            "[&::-moz-range-thumb]:border-prism-primary",
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-prism-primary",
          )}
          {...rest}
        />
      </span>
    );
  },
);
