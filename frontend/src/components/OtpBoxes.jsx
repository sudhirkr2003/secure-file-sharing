import React, { useEffect, useRef } from "react";

const OTP_LENGTH = 6;

export default function OtpBoxes({ value, onChange, disabled = false }) {
  const inputRefs = useRef([]);
  const safeValue = String(value ?? "").replace(/\D/g, "").slice(0, OTP_LENGTH);
  const digits = Array.from(
    { length: OTP_LENGTH },
    (_, index) => safeValue[index] || ""
  );

  useEffect(() => {
    inputRefs.current[0]?.focus();
  }, []);

  function updateDigits(nextDigits, focusIndex) {
    onChange(nextDigits.join(""));
    if (focusIndex >= 0 && focusIndex < OTP_LENGTH) {
      inputRefs.current[focusIndex]?.focus();
    }
  }

  function handleChange(index, event) {
    const entered = event.target.value.replace(/\D/g, "");
    const nextDigits = [...digits];

    if (!entered) {
      nextDigits[index] = "";
      updateDigits(nextDigits, index);
      return;
    }

    nextDigits[index] = entered.slice(-1);
    updateDigits(nextDigits, Math.min(index + 1, OTP_LENGTH - 1));
  }

  function handlePaste(event) {
    event.preventDefault();
    const pasted = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, OTP_LENGTH);
    if (!pasted) return;

    const nextDigits = pasted.padEnd(OTP_LENGTH, "").slice(0, OTP_LENGTH).split("");
    onChange(nextDigits.join(""));
    inputRefs.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus();
  }

  function handleKeyDown(index, event) {
    if (event.key === "Backspace" && !digits[index] && index > 0) {
      const nextDigits = [...digits];
      nextDigits[index - 1] = "";
      updateDigits(nextDigits, index - 1);
    } else if (event.key === "ArrowLeft" && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (event.key === "ArrowRight" && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  }

  return (
    <div className="otp-boxes" role="group" aria-label="One-time password">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(element) => { inputRefs.current[index] = element; }}
          className="otp-box"
          inputMode="numeric"
          pattern="[0-9]*"
          maxLength="1"
          autoComplete={index === 0 ? "one-time-code" : "off"}
          aria-label={`OTP digit ${index + 1}`}
          value={digit}
          onChange={(event) => handleChange(index, event)}
          onPaste={handlePaste}
          onKeyDown={(event) => handleKeyDown(index, event)}
          disabled={disabled}
          required
        />
      ))}
    </div>
  );
}
