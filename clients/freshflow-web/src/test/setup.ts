import '@testing-library/jest-dom/vitest';

// JSDOM doesn't implement window.scrollTo by default
if (typeof window !== 'undefined') {
  window.scrollTo = () => {};
}
