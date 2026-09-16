import { useState, useEffect } from 'react';

/**
 * Custom hook to debounce a fast-changing value (e.g. search input).
 *
 * @param value The value to debounce.
 * @param delay Milliseconds to delay before updating the debounced value (default 300ms).
 */
export function useDebounce<T>(value: T, delay: number = 300): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);

  return debouncedValue;
}
